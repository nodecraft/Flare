package com.nodecraft.hytale.flare.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FlareConfig {
    public static final BuilderCodec<FlareConfig> CODEC = BuilderCodec.builder(FlareConfig.class, FlareConfig::new)
            .append(
                    new KeyedCodec<>("HeapMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.heapMonitorConfig = value,
                    config -> config.heapMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("GcMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.gcMonitorConfig = value,
                    config -> config.gcMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("ThreadMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.threadMonitorConfig = value,
                    config -> config.threadMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("TpsMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.tpsMonitorConfig = value,
                    config -> config.tpsMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("CpuMonitor", MonitorConfig.CODEC),        
                    (config, value) -> config.cpuMonitorConfig = value,
                    config -> config.cpuMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("NetworkMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.networkMonitorConfig = value,
                    config -> config.networkMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("WorldMonitor", MonitorConfig.CODEC),
                    (config, value) -> config.worldMonitorConfig = value,
                    config -> config.worldMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("Profiler", ProfilerConfig.CODEC),
                    (config, value) -> config.profilerConfig = value,
                    config -> config.profilerConfig
            ).add()
            .build();

    private MonitorConfig heapMonitorConfig = new MonitorConfig();
    private MonitorConfig gcMonitorConfig = new MonitorConfig();
    private MonitorConfig threadMonitorConfig = new MonitorConfig();
    private MonitorConfig tpsMonitorConfig = new MonitorConfig();
    private MonitorConfig cpuMonitorConfig = new MonitorConfig();
    private MonitorConfig networkMonitorConfig = new MonitorConfig();
    private MonitorConfig worldMonitorConfig = new MonitorConfig();
    private ProfilerConfig profilerConfig = new ProfilerConfig();

    public MonitorConfig getHeapMonitorConfig() {
        return heapMonitorConfig;
    }

    public MonitorConfig getGcMonitorConfig() {
        return gcMonitorConfig;
    }

    public MonitorConfig getThreadMonitorConfig() {
        return threadMonitorConfig;
    }

    public MonitorConfig getTpsMonitorConfig() {
        return tpsMonitorConfig;
    }

    public MonitorConfig getCpuMonitorConfig() {
        return cpuMonitorConfig;
    }

    public MonitorConfig getNetworkMonitorConfig() {
        return networkMonitorConfig;
    }

    public MonitorConfig getWorldMonitorConfig() {
        return worldMonitorConfig;
    }

    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }
}
