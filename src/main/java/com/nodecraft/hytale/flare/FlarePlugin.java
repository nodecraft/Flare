package com.nodecraft.hytale.flare;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.nodecraft.hytale.flare.commands.DiagnosticsCommand;
import com.nodecraft.hytale.flare.config.FlareConfig;
import com.nodecraft.hytale.flare.monitoring.CpuMonitor;
import com.nodecraft.hytale.flare.monitoring.GcMonitor;
import com.nodecraft.hytale.flare.monitoring.HeapMonitor;
import com.nodecraft.hytale.flare.monitoring.ThreadMonitor;
import com.nodecraft.hytale.flare.monitoring.TpsMonitor;
import com.nodecraft.hytale.flare.monitoring.WorldMonitor;
import com.nodecraft.hytale.flare.profiler.PerformanceProfiler;

public class FlarePlugin extends JavaPlugin {

    private final Config<FlareConfig> _config = withConfig(FlareConfig.CODEC);
    private FlareConfig config;

    private HeapMonitor heapMonitor;
    private GcMonitor gcMonitor;
    private ThreadMonitor threadMonitor;
    private TpsMonitor tpsMonitor;
    private CpuMonitor cpuMonitor;
    private WorldMonitor worldMonitor;
    private PerformanceProfiler profiler;
    private DiagnosticsCommand diagnosticsCommand;

    public FlarePlugin(@Nonnull JavaPluginInit init) {
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
        this.worldMonitor = new WorldMonitor(config.getWorldMonitorConfig());

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
                worldMonitor,
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
                worldMonitor,
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
