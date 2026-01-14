package com.nodecraft.hytale.performance_diagnostics;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.nodecraft.hytale.performance_diagnostics.commands.DiagnosticsCommand;
import com.nodecraft.hytale.performance_diagnostics.config.PerformanceDiagnosticsConfig;
import com.nodecraft.hytale.performance_diagnostics.monitoring.*;
import com.nodecraft.hytale.performance_diagnostics.profiler.PerformanceProfiler;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class PerformanceDiagnosticsPlugin extends JavaPlugin {

    private final Config<PerformanceDiagnosticsConfig> _config = withConfig(PerformanceDiagnosticsConfig.CODEC);
    private PerformanceDiagnosticsConfig config;

    private HeapMonitor heapMonitor;
    private GcMonitor gcMonitor;
    private ThreadMonitor threadMonitor;
    private TpsMonitor tpsMonitor;
    private CpuMonitor cpuMonitor;
    private PerformanceProfiler profiler;
    private DiagnosticsCommand diagnosticsCommand;

    public PerformanceDiagnosticsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.config = this._config.get();

        // Initialize monitors
        this.heapMonitor = new HeapMonitor(config.getHeapMonitorConfig());
        this.gcMonitor = new GcMonitor(config.getGcMonitorConfig());
        this.threadMonitor = new ThreadMonitor(config.getThreadMonitorConfig());
        this.tpsMonitor = new TpsMonitor(config.getTpsMonitorConfig());
        this.cpuMonitor = new CpuMonitor(config.getCpuMonitorConfig());

        // Initialize profiler
        // Profiles will be stored in mods/Flare/profiles/
        Path profilesDirectory = java.nio.file.Paths.get("mods", "Flare", "profiles");
        String pluginVersion = getManifest().getVersion() != null 
                ? getManifest().getVersion().toString() 
                : "unknown";
        this.profiler = new PerformanceProfiler(
                getLogger().getSubLogger("Profiler"),
                config.getProfilerConfig(),
                heapMonitor,
                gcMonitor,
                threadMonitor,
                tpsMonitor,
                cpuMonitor,
                profilesDirectory,
                pluginVersion
        );

        // Initialize command
        this.diagnosticsCommand = new DiagnosticsCommand(
                this,
                heapMonitor,
                gcMonitor,
                threadMonitor,
                tpsMonitor,
                cpuMonitor,
                profiler
        );

        // Register command
        getCommandRegistry().registerCommand(diagnosticsCommand);

        getLogger().atInfo().log("Flare plugin initialized");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("Flare plugin started");
        getLogger().atInfo().log("Use /flare for performance information");
    }

    @Override
    protected void shutdown() {
        // Stop any active profiling session
        if (profiler != null) {
            profiler.stop();
            // Shutdown the profiler executor service gracefully
            profiler.shutdown();
        }

        getLogger().atInfo().log("Flare plugin stopped");
    }
}
