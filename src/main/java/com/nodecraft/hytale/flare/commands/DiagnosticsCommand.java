package com.nodecraft.hytale.flare.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.nodecraft.hytale.flare.monitoring.*;
import com.nodecraft.hytale.flare.model.*;
import com.nodecraft.hytale.flare.profiler.PerformanceProfiler;
import com.nodecraft.hytale.flare.profiler.ProfilerSession;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public final class DiagnosticsCommand extends AbstractCommandCollection {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private final JavaPlugin plugin;
    private final HeapMonitor heapMonitor;
    private final GcMonitor gcMonitor;
    private final ThreadMonitor threadMonitor;
    private final TpsMonitor tpsMonitor;
    private final CpuMonitor cpuMonitor;
    private final NetworkMonitor networkMonitor;
    private final WorldMonitor worldMonitor;
    private final PerformanceProfiler profiler;

    public DiagnosticsCommand(
            JavaPlugin plugin,
            HeapMonitor heapMonitor,
            GcMonitor gcMonitor,
            ThreadMonitor threadMonitor,
            TpsMonitor tpsMonitor,
            CpuMonitor cpuMonitor,
            NetworkMonitor networkMonitor,
            WorldMonitor worldMonitor,
            PerformanceProfiler profiler
    ) {
        super("flare", "Performance diagnostics and profiling commands");
        this.plugin = plugin;
        this.heapMonitor = heapMonitor;
        this.gcMonitor = gcMonitor;
        this.threadMonitor = threadMonitor;
        this.tpsMonitor = tpsMonitor;
        this.cpuMonitor = cpuMonitor;
        this.networkMonitor = networkMonitor;
        this.worldMonitor = worldMonitor;
        this.profiler = profiler;

        // Add subcommands
        this.addSubCommand(new InfoCommand());
        this.addSubCommand(new StatusCommand());
        this.addSubCommand(new HeapCommand());
        this.addSubCommand(new GcCommand());
        this.addSubCommand(new ThreadsCommand());
        this.addSubCommand(new TpsCommand());
        this.addSubCommand(new WorldCommand());
        this.addSubCommand(new CpuCommand());
        this.addSubCommand(new NetworkCommand());
        this.addSubCommand(new ProfileCommand());
    }

    private class InfoCommand extends CommandBase {
        public InfoCommand() {
            super("info", "Show plugin information");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showInfo(context);
        }
    }

    private class StatusCommand extends CommandBase {
        public StatusCommand() {
            super("status", "Show current performance metrics snapshot");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showStatus(context);
        }
    }

    private class HeapCommand extends CommandBase {
        public HeapCommand() {
            super("heap", "Show detailed heap memory status");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showHeap(context);
        }
    }

    private class GcCommand extends CommandBase {
        public GcCommand() {
            super("gc", "Show garbage collection statistics");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showGc(context);
        }
    }

    private class ThreadsCommand extends CommandBase {
        public ThreadsCommand() {
            super("threads", "Show thread information and deadlock detection");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showThreads(context);
        }
    }

    private class TpsCommand extends CommandBase {
        public TpsCommand() {
            super("tps", "Show TPS information");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showTps(context);
        }
    }

    private class WorldCommand extends CommandBase {
        private final OptionalArg<String> worldArg =
                this.withOptionalArg("world", "flare.commands.world.name", ArgTypes.STRING);

        public WorldCommand() {
            super("world", "Show world metrics (optionally for a specific world)");
            this.setAllowsExtraArguments(true);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showWorld(context, worldArg);
        }
    }

    private class CpuCommand extends CommandBase {
        public CpuCommand() {
            super("cpu", "Show CPU usage (if available)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showCpu(context);
        }
    }

    private class NetworkCommand extends CommandBase {
        public NetworkCommand() {
            super("network", "Show network statistics");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            showNetwork(context);
        }
    }

    private class ProfileCommand extends AbstractCommandCollection {
        public ProfileCommand() {
            super("profile", "Performance profiling commands");
            this.addSubCommand(new ProfileStartCommand());
            this.addSubCommand(new ProfileStopCommand());
            this.addSubCommand(new ProfileStatusCommand());
        }

        private class ProfileStartCommand extends CommandBase {
            private final OptionalArg<Integer> timeoutSecondsArg =
                    this.withOptionalArg("timeout", "flare.commands.profile.start.timeout", ArgTypes.INTEGER);

            public ProfileStartCommand() {
                super("start", "Start a profiling session");
                this.setAllowsExtraArguments(true);
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {
                startProfile(context, timeoutSecondsArg);
            }
        }

        private class ProfileStopCommand extends CommandBase {
            public ProfileStopCommand() {
                super("stop", "Stop the current profiling session");
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {
                stopProfile(context);
            }
        }

        private class ProfileStatusCommand extends CommandBase {
            public ProfileStatusCommand() {
                super("status", "Show profiling session status");
            }

            @Override
            protected void executeSync(@Nonnull CommandContext context) {
                showProfileStatus(context);
            }
        }
    }

    private void showStatus(CommandContext context) {
        PerformanceSnapshot snapshot = collectSnapshot();
        context.sendMessage(Message.raw("=== Performance Diagnostics Status ==="));
        
        if (snapshot.heap() != null) {
            HeapMetrics heap = snapshot.heap();
            context.sendMessage(Message.raw(String.format(
                    "Heap: %s / %s MB (%.1f%%)",
                    formatBytes(heap.used()),
                    formatBytes(heap.max()),
                    heap.usageRatio() * 100
            )));
        }

        if (snapshot.tps() != null) {
            TpsMetrics tps = snapshot.tps();
            context.sendMessage(Message.raw(String.format(
                    "TPS: %.2f (avg: %.2f, min: %.2f, max: %.2f)",
                    tps.currentTps(), tps.averageTps(), tps.minTps(), tps.maxTps()
            )));
            String tickTimeSummary = formatTickTimeSummary();
            if (tickTimeSummary != null) {
                context.sendMessage(Message.raw(tickTimeSummary));
            }
        }

        if (snapshot.threads() != null) {
            ThreadMetrics threads = snapshot.threads();
            context.sendMessage(Message.raw(String.format(
                    "Threads: %d total, %d daemon%s",
                    threads.totalThreads(),
                    threads.daemonThreads(),
                    threads.hasDeadlocks() ? " (DEADLOCKS DETECTED!)" : ""
            )));
        }

        if (snapshot.cpu() != null && snapshot.cpu().cpuMonitoringAvailable()) {
            CpuMetrics cpu = snapshot.cpu();
            context.sendMessage(Message.raw(String.format(
                    "CPU: Process %.1f%%, System %.1f%% (%d cores)",
                    cpu.processCpuLoad() * 100,
                    cpu.systemCpuLoad() * 100,
                    cpu.availableProcessors()
            )));
        }

        if (snapshot.world() != null) {
            WorldMetrics world = snapshot.world();
            context.sendMessage(Message.raw(String.format(
                    "Worlds: %d, Chunks: %d, Entities: %d",
                    world.worldCount(),
                    world.totalLoadedChunks(),
                    world.totalEntities()
            )));
        }

        if (snapshot.network() != null) {
            NetworkMetrics network = snapshot.network();
            context.sendMessage(Message.raw(String.format(
                    "Network: Sent %s / Received %s",
                    formatBytes(network.totalSentCompressedBytes()),
                    formatBytes(network.totalReceivedCompressedBytes())
            )));
        }
    }

    private void showHeap(CommandContext context) {
        HeapMetrics heap = heapMonitor.collect();
        if (heap == null) {
            context.sendMessage(Message.raw("Heap monitoring is disabled"));
            return;
        }

        context.sendMessage(Message.raw("=== Heap Memory Status ==="));
        context.sendMessage(Message.raw(String.format("Used: %s", formatBytes(heap.used()))));
        context.sendMessage(Message.raw(String.format("Free: %s", formatBytes(heap.free()))));
        context.sendMessage(Message.raw(String.format("Committed: %s", formatBytes(heap.committed()))));
        context.sendMessage(Message.raw(String.format("Max: %s", formatBytes(heap.max()))));
        context.sendMessage(Message.raw(String.format("Usage: %.2f%%", heap.usageRatio() * 100)));
    }

    private void showGc(CommandContext context) {
        GcMetrics gc = gcMonitor.collect();
        if (gc == null) {
            context.sendMessage(Message.raw("GC monitoring is disabled"));
            return;
        }

        context.sendMessage(Message.raw("=== Garbage Collection Status ==="));
        context.sendMessage(Message.raw(String.format("Total Collections: %d", gc.totalCollections())));
        context.sendMessage(Message.raw(String.format("Total Collection Time: %d ms", gc.totalCollectionTime())));
        context.sendMessage(Message.raw(String.format("Average Pause Time: %.2f ms", gc.averagePauseTime())));

        for (GcMetrics.GcCollectorInfo collector : gc.collectors()) {
            context.sendMessage(Message.raw(String.format(
                    "  %s: %d collections, %d ms total, %.2f ms avg",
                    collector.name(),
                    collector.collectionCount(),
                    collector.collectionTime(),
                    collector.averagePauseTime()
            )));
        }
    }

    private void showThreads(CommandContext context) {
        ThreadMetrics threads = threadMonitor.collect();
        if (threads == null) {
            context.sendMessage(Message.raw("Thread monitoring is disabled"));
            return;
        }

        context.sendMessage(Message.raw("=== Thread Status ==="));
        context.sendMessage(Message.raw(String.format("Total Threads: %d", threads.totalThreads())));
        context.sendMessage(Message.raw(String.format("Peak Threads: %d", threads.peakThreads())));
        context.sendMessage(Message.raw(String.format("Total Started: %d", threads.totalStartedThreads())));
        context.sendMessage(Message.raw(String.format("Daemon Threads: %d", threads.daemonThreads())));

        context.sendMessage(Message.raw("Threads by State:"));
        for (var entry : threads.threadsByState().entrySet()) {
            if (entry.getValue() > 0) {
                context.sendMessage(Message.raw(String.format("  %s: %d", entry.getKey(), entry.getValue())));
            }
        }

        if (threads.hasDeadlocks()) {
            context.sendMessage(Message.raw("WARNING: Deadlocks detected!"));
            context.sendMessage(Message.raw("Deadlocked Thread IDs: " + threads.deadlockedThreads()));
        }
    }

    private void showTps(CommandContext context) {
        TpsMetrics tps = tpsMonitor.collect();
        if (tps == null) {
            context.sendMessage(Message.raw("TPS monitoring is disabled"));
            return;
        }

        context.sendMessage(Message.raw("=== TPS Status ==="));
        context.sendMessage(Message.raw(String.format("Current TPS: %.2f", tps.currentTps())));
        context.sendMessage(Message.raw(String.format("Average TPS: %.2f", tps.averageTps())));
        context.sendMessage(Message.raw(String.format("Min TPS: %.2f", tps.minTps())));
        context.sendMessage(Message.raw(String.format("Max TPS: %.2f", tps.maxTps())));

        String tickTimeSummary = formatTickTimeSummary();
        if (tickTimeSummary != null) {
            context.sendMessage(Message.raw(tickTimeSummary));
        }

        WorldMetrics worldMetrics = worldMonitor.collect();
        if (worldMetrics != null && !worldMetrics.worlds().isEmpty()) {
            context.sendMessage(Message.raw("World Tick (avg 10s):"));
            var worldMap = Universe.get().getWorlds();
            for (WorldSnapshot snapshot : worldMetrics.worlds()) {
                String tickMs = formatTickTimeMs(snapshot.avgTickNanos());
                String tpsInfo = "";
                World world = worldMap.get(snapshot.name());
                if (world != null && snapshot.avgTickNanos() > 0.0) {
                    double worldTps = 1_000_000_000.0 / Math.max(snapshot.avgTickNanos(), world.getTickStepNanos());
                    tpsInfo = String.format(", TPS %.2f", worldTps);
                }
                context.sendMessage(Message.raw(String.format(
                        "  %s: %s%s",
                        snapshot.name(),
                        tickMs,
                        tpsInfo
                )));
            }
        }
    }

    private void showWorld(CommandContext context, OptionalArg<String> worldArg) {
        WorldMetrics worldMetrics = worldMonitor.collect();
        if (worldMetrics == null) {
            context.sendMessage(Message.raw("World monitoring is disabled"));
            return;
        }

        Map<String, World> worlds = Universe.get().getWorlds();
        if (worlds.isEmpty()) {
            context.sendMessage(Message.raw("No worlds found"));
            return;
        }

        String requestedWorld = null;
        if (worldArg != null && worldArg.provided(context)) {
            requestedWorld = worldArg.get(context);
        } else {
            requestedWorld = parseTrailingWorld(context.getInputString());
        }

        if (requestedWorld == null || requestedWorld.isBlank()) {
            context.sendMessage(Message.raw("=== World Summary ==="));
            context.sendMessage(Message.raw(String.format(
                    "Worlds: %d, Total Chunks: %d, Total Entities: %d",
                    worldMetrics.worldCount(),
                    worldMetrics.totalLoadedChunks(),
                    worldMetrics.totalEntities()
            )));
            for (WorldSnapshot snapshot : worldMetrics.worlds()) {
                context.sendMessage(Message.raw(String.format(
                        "  %s: chunks %d, entities %d, tick %s, ticking %s, paused %s",
                        snapshot.name(),
                        snapshot.loadedChunks(),
                        snapshot.entityCount(),
                        formatTickTimeMs(snapshot.avgTickNanos()),
                        snapshot.ticking() ? "yes" : "no",
                        snapshot.paused() ? "yes" : "no"
                )));
            }
            return;
        }

        World world = worlds.get(requestedWorld);
        if (world == null) {
            context.sendMessage(Message.raw(String.format("World not found: %s", requestedWorld)));
            context.sendMessage(Message.raw(String.format("Available worlds: %s", String.join(", ", worlds.keySet()))));
            return;
        }

        context.sendMessage(Message.raw(String.format("=== World: %s ===", world.getName())));
        context.sendMessage(Message.raw(String.format("Ticking: %s", world.isTicking() ? "yes" : "no")));
        context.sendMessage(Message.raw(String.format("Paused: %s", world.isPaused() ? "yes" : "no")));

        ChunkStore chunkStore = world.getChunkStore();
        context.sendMessage(Message.raw(String.format(
                "Chunks: loaded %d, total loaded %d, total generated %d",
                chunkStore.getLoadedChunksCount(),
                chunkStore.getTotalLoadedChunksCount(),
                chunkStore.getTotalGeneratedChunksCount()
        )));

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        context.sendMessage(Message.raw(String.format(
                "Entities: %d (archetype chunks %d)",
                entityStore.getEntityCount(),
                entityStore.getArchetypeChunkCount()
        )));

        long tickStepNanos = world.getTickStepNanos();
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        if (metrics == null) {
            context.sendMessage(Message.raw("Tick Metrics: unavailable"));
            return;
        }

        double avg1s = computeWindowAverage(metrics, 1_000_000_000L);
        double avg10s = getAverageSafe(metrics, 0);
        double avg1m = getAverageSafe(metrics, 1);
        double avg5m = getAverageSafe(metrics, 2);
        long min10s = metrics.calculateMin(0);
        long max10s = metrics.calculateMax(0);

        context.sendMessage(Message.raw(String.format(
                "Tick Time Avg: 1s %s, 10s %s, 1m %s, 5m %s (target: %.2f ms)",
                formatTickTimeMs(avg1s),
                formatTickTimeMs(avg10s),
                formatTickTimeMs(avg1m),
                formatTickTimeMs(avg5m),
                tickStepNanos / 1_000_000.0
        )));
        context.sendMessage(Message.raw(String.format(
                "Tick Time 10s Min/Max: %s / %s",
                formatTickTimeMs(min10s),
                formatTickTimeMs(max10s)
        )));
    }

    private String formatTickTimeSummary() {
        long tickStepNanos = tpsMonitor.getLastTickStepNanos();
        if (tickStepNanos <= 0L) {
            return null;
        }

        double avg1s = tpsMonitor.getLastTickAvg1sNanos();
        double avg10s = tpsMonitor.getLastTickAvg10sNanos();
        double avg1m = tpsMonitor.getLastTickAvg1mNanos();
        double avg5m = tpsMonitor.getLastTickAvg5mNanos();

        return String.format(
                "Avg Tick Time: 1s %s, 10s %s, 1m %s, 5m %s (target: %.2f ms)",
                formatTickTimeMs(avg1s),
                formatTickTimeMs(avg10s),
                formatTickTimeMs(avg1m),
                formatTickTimeMs(avg5m),
                tickStepNanos / 1_000_000.0
        );
    }

    private String formatTickTimeMs(double nanos) {
        if (nanos <= 0.0) {
            return "n/a";
        }
        return String.format("%.2f ms", nanos / 1_000_000.0);
    }

    private static double getAverageSafe(HistoricMetric metrics, int index) {
        long[] periods = metrics.getPeriodsNanos();
        if (periods == null || index < 0 || index >= periods.length) {
            return 0.0;
        }
        return metrics.getAverage(index);
    }

    private static double computeWindowAverage(HistoricMetric metrics, long windowNanos) {
        long[] timestamps = metrics.getAllTimestamps();
        long[] values = metrics.getAllValues();
        if (timestamps.length == 0 || values.length == 0) {
            return 0.0;
        }

        long latest = timestamps[timestamps.length - 1];
        long cutoff = latest - windowNanos;
        long sum = 0L;
        int count = 0;

        for (int i = timestamps.length - 1; i >= 0; i--) {
            if (timestamps[i] < cutoff) {
                break;
            }
            sum += values[i];
            count++;
        }

        return count > 0 ? sum / (double) count : 0.0;
    }

    private void showCpu(CommandContext context) {
        CpuMetrics cpu = cpuMonitor.collect();
        if (cpu == null) {
            context.sendMessage(Message.raw("CPU monitoring is disabled"));
            return;
        }

        if (!cpu.cpuMonitoringAvailable()) {
            context.sendMessage(Message.raw("CPU monitoring is not available on this JVM"));
            return;
        }

        context.sendMessage(Message.raw("=== CPU Status ==="));
        context.sendMessage(Message.raw(String.format("Available Processors: %d", cpu.availableProcessors())));
        context.sendMessage(Message.raw(String.format("Process CPU Load: %.2f%%", cpu.processCpuLoad() * 100)));
        context.sendMessage(Message.raw(String.format("System CPU Load: %.2f%%", cpu.systemCpuLoad() * 100)));
    }

    private void showNetwork(CommandContext context) {
        NetworkMetrics network = networkMonitor.collect();
        if (network == null) {
            context.sendMessage(Message.raw("Network monitoring is disabled"));
            return;
        }

        context.sendMessage(Message.raw("=== Network Stats ==="));
        context.sendMessage(Message.raw(String.format(
                "Total Sent: %s (raw: %s)",
                formatBytes(network.totalSentCompressedBytes()),
                formatBytes(network.totalSentUncompressedBytes())
        )));
        context.sendMessage(Message.raw(String.format(
                "Total Received: %s (raw: %s)",
                formatBytes(network.totalReceivedCompressedBytes()),
                formatBytes(network.totalReceivedUncompressedBytes())
        )));
        context.sendMessage(Message.raw(String.format(
                "Since Start - Sent: %s, Received: %s",
                formatBytes(network.sinceStartSentCompressedBytes()),
                formatBytes(network.sinceStartReceivedCompressedBytes())
        )));
        if (network.profileActive()) {
            context.sendMessage(Message.raw(String.format(
                    "During Profile - Sent: %s, Received: %s",
                    formatBytes(network.sinceProfileSentCompressedBytes()),
                    formatBytes(network.sinceProfileReceivedCompressedBytes())
            )));
        }
    }

    private void startProfile(CommandContext context, OptionalArg<Integer> timeoutSecondsArg) {
        ProfilerSession activeSession = profiler.getActiveSession();
        if (activeSession != null) {
            context.sendMessage(Message.raw("A profiling session is already active"));
            return;
        }

        Duration timeout = null;
        if (timeoutSecondsArg != null && timeoutSecondsArg.provided(context)) {
            Integer seconds = timeoutSecondsArg.get(context);
            if (seconds == null || seconds <= 0) {
                context.sendMessage(Message.raw("Timeout must be a positive number of seconds"));
                return;
            }
            timeout = Duration.ofSeconds(seconds);
        } else {
            Integer seconds = parseTrailingSeconds(context.getInputString());
            if (seconds != null) {
                if (seconds <= 0) {
                    context.sendMessage(Message.raw("Timeout must be a positive number of seconds"));
                    return;
                }
                timeout = Duration.ofSeconds(seconds);
            }
        }

        if (profiler.start(timeout)) {
            if (timeout != null) {
                context.sendMessage(Message.raw(String.format(
                        "Started performance profiling session (auto-stop in %d seconds)",
                        timeout.getSeconds()
                )));
            } else {
                context.sendMessage(Message.raw("Started performance profiling session"));
            }
        } else {
            context.sendMessage(Message.raw("Failed to start profiling session"));
        }
    }

    private Integer parseTrailingSeconds(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        String last = parts[parts.length - 1];
        if (last.startsWith("--")) {
            return null;
        }
        try {
            return Integer.parseInt(last);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseTrailingWorld(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        String last = parts[parts.length - 1];
        if (last.startsWith("--")) {
            return null;
        }
        return last;
    }

    private void stopProfile(CommandContext context) {
        ProfilerSession activeSession = profiler.getActiveSession();
        if (activeSession == null) {
            context.sendMessage(Message.raw("No active profiling session"));
            return;
        }

        if (profiler.stop()) {
            context.sendMessage(Message.raw("Stopped profiling session"));
        } else {
            context.sendMessage(Message.raw("Failed to stop profiling session"));
        }
    }

    private void showProfileStatus(CommandContext context) {
        ProfilerSession activeSession = profiler.getActiveSession();
        if (activeSession == null) {
            context.sendMessage(Message.raw("No active profiling session"));
            return;
        }

        Instant startTime = activeSession.getStartTime();
        Duration elapsed = Duration.between(startTime, Instant.now());
        int snapshotCount = activeSession.getSnapshotCount();

        context.sendMessage(Message.raw("=== Profiling Session Status ==="));
        context.sendMessage(Message.raw(String.format("Started: %s", startTime)));
        context.sendMessage(Message.raw(String.format("Elapsed: %s", formatDuration(elapsed))));
        context.sendMessage(Message.raw(String.format("Snapshots: %d", snapshotCount)));
        context.sendMessage(Message.raw(String.format("Active: %s", activeSession.isActive() ? "Yes" : "No")));
    }

    private void showInfo(CommandContext context) {
        var manifest = plugin.getManifest();
        
        context.sendMessage(Message.raw("=== Flare Plugin Information ==="));
        context.sendMessage(Message.raw(String.format("Name: %s", manifest.getName())));
        context.sendMessage(Message.raw(String.format("Group: %s", manifest.getGroup())));
        
        if (manifest.getVersion() != null) {
            context.sendMessage(Message.raw(String.format("Version: %s", manifest.getVersion())));
        }
        
        if (manifest.getDescription() != null && !manifest.getDescription().isEmpty()) {
            context.sendMessage(Message.raw(String.format("Description: %s", manifest.getDescription())));
        }
        
        var authors = manifest.getAuthors();
        if (authors != null && !authors.isEmpty()) {
            context.sendMessage(Message.raw("Authors:"));
            for (var author : authors) {
                String authorInfo = author.getName();
                if (author.getEmail() != null && !author.getEmail().isEmpty()) {
                    authorInfo += " (" + author.getEmail() + ")";
                }
                if (author.getUrl() != null && !author.getUrl().isEmpty()) {
                    authorInfo += " - " + author.getUrl();
                }
                context.sendMessage(Message.raw("  - " + authorInfo));
            }
        }
        
        if (manifest.getWebsite() != null && !manifest.getWebsite().isEmpty()) {
            context.sendMessage(Message.raw(String.format("Website: %s", manifest.getWebsite())));
        }
        
        context.sendMessage(Message.raw(String.format("Plugin State: %s", plugin.getState())));
        context.sendMessage(Message.raw(String.format("Data Directory: %s", plugin.getDataDirectory())));
    }

    private PerformanceSnapshot collectSnapshot() {
        return new PerformanceSnapshot(
                Instant.now(),
                heapMonitor.collect(),
                gcMonitor.collect(),
                threadMonitor.collect(),
                tpsMonitor.collect(),
                cpuMonitor.collect(),
                worldMonitor.collect(),
                networkMonitor.collect()
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return DECIMAL_FORMAT.format(bytes / 1024.0) + " KB";
        if (bytes < 1024 * 1024 * 1024) return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }
}
