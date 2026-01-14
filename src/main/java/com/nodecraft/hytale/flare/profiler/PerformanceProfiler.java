package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.nodecraft.hytale.flare.config.ProfilerConfig;
import com.nodecraft.hytale.flare.monitoring.*;
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
        
        // Create dedicated executor service for profiling operations
        this.profilerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Flare-Profiler");
            t.setDaemon(true);
            return t;
        });
    }

    public boolean start() {
        if (activeSession.get() != null) {
            return false;
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
        ProfilerData data = session.getData();

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

        // Collect all metrics on the background thread
        PerformanceSnapshot snapshot = new PerformanceSnapshot(
                Instant.now(),
                heapMonitor.collect(),
                gcMonitor.collect(),
                threadMonitor.collect(),
                tpsMonitor.collect(),
                cpuMonitor.collect()
        );

        session.addSnapshot(snapshot);

        if (!session.isActive()) {
            // Auto-stop if limits reached
            // Note: stop() may write files, which is acceptable on background thread
            stop();
        }
    }
}
