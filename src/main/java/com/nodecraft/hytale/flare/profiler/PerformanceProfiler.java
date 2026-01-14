package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.nodecraft.hytale.flare.config.ProfilerConfig;
import com.nodecraft.hytale.flare.monitoring.*;
import com.nodecraft.hytale.flare.model.CpuProfileData;
import com.nodecraft.hytale.flare.model.PerformanceSnapshot;
import com.nodecraft.hytale.flare.util.EnvironmentInfoCollector;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class PerformanceProfiler {
    private final HytaleLogger logger;
    private final ProfilerConfig config;
    private final HeapMonitor heapMonitor;
    private final GcMonitor gcMonitor;
    private final ThreadMonitor threadMonitor;
    private final TpsMonitor tpsMonitor;
    private final CpuMonitor cpuMonitor;
    private final Path profilesDirectory;
    private final String pluginVersion;

    private final AtomicReference<ProfilerSession> activeSession = new AtomicReference<>();
    private final ScheduledExecutorService profilerExecutor;
    private final AsyncProfilerWrapper asyncProfiler;
    
    // Cached metrics for tiered collection
    private volatile PerformanceSnapshot lastFullSnapshot = null;
    private volatile Instant lastFullCollection = Instant.MIN;
    private final long fullCollectionIntervalMs;

    public PerformanceProfiler(
            HytaleLogger logger,
            ProfilerConfig config,
            HeapMonitor heapMonitor,
            GcMonitor gcMonitor,
            ThreadMonitor threadMonitor,
            TpsMonitor tpsMonitor,
            CpuMonitor cpuMonitor,
            Path profilesDirectory,
            String pluginVersion
    ) {
        this.logger = logger;
        this.config = config;
        this.heapMonitor = heapMonitor;
        this.gcMonitor = gcMonitor;
        this.threadMonitor = threadMonitor;
        this.tpsMonitor = tpsMonitor;
        this.cpuMonitor = cpuMonitor;
        this.profilesDirectory = profilesDirectory;
        this.pluginVersion = pluginVersion;
        
        // Configure tiered collection interval (collect full metrics less frequently)
        this.fullCollectionIntervalMs = config.getSystemMetricsIntervalMs();
        
        // Create dedicated executor service for profiling operations
        this.profilerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Flare-Profiler");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize async-profiler if enabled and available
        if (config.isCpuProfilingEnabled() && AsyncProfilerWrapper.isAvailable()) {
            this.asyncProfiler = AsyncProfilerWrapper.create(logger);
            if (asyncProfiler.isInitialized()) {
                logger.atInfo().log("Async-profiler initialized for high-frequency CPU profiling");
            } else {
                logger.atWarning().log("Async-profiler requested but not available. CPU profiling will use fallback.");
            }
        } else {
            this.asyncProfiler = null;
            if (config.isCpuProfilingEnabled() && !AsyncProfilerWrapper.isAvailable()) {
                logger.atInfo().log("Async-profiler not available on this platform. CPU profiling disabled.");
            }
        }
    }

    public boolean start() {
        if (activeSession.get() != null) {
            return false;
        }

        // Start async-profiler if available and enabled
        if (asyncProfiler != null && asyncProfiler.isInitialized() && config.isCpuProfilingEnabled()) {
            int intervalMs = config.getCpuSamplingIntervalMs();
            if (!asyncProfiler.start(intervalMs)) {
                logger.atWarning().log("Failed to start async-profiler, continuing with system metrics only");
            }
        }

        ProfilerMetadata metadata = EnvironmentInfoCollector.createMetadata(pluginVersion);
        ProfilerSession session = new ProfilerSession(metadata, config, this::collectSnapshot, profilerExecutor);
        if (activeSession.compareAndSet(null, session)) {
            session.startSampling(HytaleServer.SCHEDULED_EXECUTOR);
            logger.atInfo().log("Started performance profiling session");
            return true;
        }
        return false;
    }

    public boolean stop() {
        ProfilerSession session = activeSession.getAndSet(null);
        if (session == null) {
            return false;
        }

        session.stop();
        
        // Stop async-profiler and collect CPU profile data
        CpuProfileData cpuProfile = null;
        if (asyncProfiler != null && asyncProfiler.isProfiling()) {
            cpuProfile = asyncProfiler.stop();
            if (cpuProfile != null) {
                logger.atInfo().log("Collected CPU profile with %d stack samples", cpuProfile.samples().size());
            }
        }
        
        ProfilerData data = session.getData();
        
        // Add CPU profile data if available
        if (cpuProfile != null) {
            data = data.withCpuProfile(cpuProfile);
        }

        try {
            Path filePath = ProfilerWriter.writeProfilerData(data, profilesDirectory);
            logger.atInfo().log("Stopped profiling session. Wrote %d snapshots to %s", 
                    data.snapshots().size(), filePath);
            return true;
        } catch (Exception e) {
            logger.atSevere().log("Failed to write profiler data: %s", e.getMessage());
            return false;
        }
    }

    public ProfilerSession getActiveSession() {
        return activeSession.get();
    }

    /**
     * Shuts down the profiler executor service gracefully.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        // Stop async-profiler if running
        if (asyncProfiler != null && asyncProfiler.isProfiling()) {
            asyncProfiler.stop();
            asyncProfiler.cleanup();
        }
        
        // Cleanup native libraries
        NativeLibraryLoader.cleanup();
        
        try {
            profilerExecutor.shutdown();
            if (!profilerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                profilerExecutor.shutdownNow();
                if (!profilerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.atWarning().log("Profiler executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            profilerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void collectSnapshot() {
        // This method is now called asynchronously on the profiler executor thread
        ProfilerSession session = activeSession.get();
        if (session == null || !session.isActive()) {
            return;
        }

        Instant now = Instant.now();
        long timeSinceFullCollection = java.time.Duration.between(lastFullCollection, now).toMillis();
        
        // Tiered collection: collect expensive metrics less frequently
        // Fast metrics (TPS, CPU) are collected every snapshot
        // Slow metrics (Heap, GC, Threads) are collected less frequently
        PerformanceSnapshot snapshot;
        
        if (timeSinceFullCollection >= fullCollectionIntervalMs || lastFullSnapshot == null) {
            // Collect all metrics (full snapshot)
            snapshot = new PerformanceSnapshot(
                    now,
                    heapMonitor.collect(),
                    gcMonitor.collect(),
                    threadMonitor.collect(),
                    tpsMonitor.collect(),
                    cpuMonitor.collect()
            );
            lastFullSnapshot = snapshot;
            lastFullCollection = now;
        } else {
            // Collect only fast-changing metrics, reuse cached slow metrics
            PerformanceSnapshot cached = lastFullSnapshot;
            snapshot = new PerformanceSnapshot(
                    now,
                    cached.heap(),      // Reuse cached heap
                    cached.gc(),        // Reuse cached GC
                    cached.threads(),   // Reuse cached threads (most expensive)
                    tpsMonitor.collect(), // Collect TPS (changes frequently)
                    cpuMonitor.collect()  // Collect CPU (changes frequently)
            );
        }

        session.addSnapshot(snapshot);

        if (!session.isActive()) {
            // Auto-stop if limits reached
            // Note: stop() may write files, which is acceptable on background thread
            stop();
        }
    }
}
