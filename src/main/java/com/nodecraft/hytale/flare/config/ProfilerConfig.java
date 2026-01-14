package com.nodecraft.hytale.flare.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Duration;

public class ProfilerConfig {
    public static final BuilderCodec<ProfilerConfig> CODEC = BuilderCodec.builder(ProfilerConfig.class, ProfilerConfig::new)
            .append(
                    new KeyedCodec<>("SamplingIntervalSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.samplingInterval = value,
                    config -> config.samplingInterval
            ).add()
            .append(
                    new KeyedCodec<>("MaxDurationSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.maxDuration = value,
                    config -> config.maxDuration
            ).add()
            .append(
                    new KeyedCodec<>("MaxSnapshots", Codec.INTEGER),
                    (config, value) -> config.maxSnapshots = value,
                    config -> config.maxSnapshots
            ).add()
            .append(
                    new KeyedCodec<>("CpuProfilingEnabled", Codec.BOOLEAN),     
                    (config, value) -> config.cpuProfilingEnabled = value,      
                    config -> config.cpuProfilingEnabled
            ).add()
            .append(
                    new KeyedCodec<>("DebugEnvLogging", Codec.BOOLEAN),
                    (config, value) -> config.debugEnvLogging = value,
                    config -> config.debugEnvLogging
            ).add()
            .append(
                    new KeyedCodec<>("CpuProfilingEvent", Codec.STRING),
                    (config, value) -> config.cpuProfilingEvent = CpuProfilingEvent.fromString(value),
                    config -> config.cpuProfilingEvent.name()
            ).add()
            .append(
                    new KeyedCodec<>("CpuSamplingIntervalMs", Codec.INTEGER),   
                    (config, value) -> config.cpuSamplingIntervalMs = value,    
                    config -> config.cpuSamplingIntervalMs
            ).add()
            .append(
                    new KeyedCodec<>("SystemMetricsIntervalMs", Codec.INTEGER),
                    (config, value) -> config.systemMetricsIntervalMs = value,
                    config -> config.systemMetricsIntervalMs
            ).add()
            .append(
                    new KeyedCodec<>("MaxStackDepth", Codec.INTEGER),
                    (config, value) -> config.maxStackDepth = value,
                    config -> config.maxStackDepth
            ).add()
            .build();

    private static final Duration MIN_SAMPLING_INTERVAL = Duration.ofSeconds(1);
    private static final int MIN_MAX_SNAPSHOTS = 100;

    private Duration samplingInterval = Duration.ofSeconds(1);
    private Duration maxDuration = Duration.ofHours(1);
    private int maxSnapshots = 3600; // 1 hour at 1 second intervals
    
    // Async-profiler CPU profiling configuration
    private boolean cpuProfilingEnabled = true;
    private boolean debugEnvLogging = false;
    private CpuProfilingEvent cpuProfilingEvent = CpuProfilingEvent.CPU;
    private int cpuSamplingIntervalMs = 4; // 4ms default (250 samples/sec) - lower overhead
    private int systemMetricsIntervalMs = 1000; // 1 second for system metrics  
    private int maxStackDepth = 128;

    public Duration getSamplingInterval() {
        return samplingInterval.compareTo(MIN_SAMPLING_INTERVAL) < 0 ? MIN_SAMPLING_INTERVAL : samplingInterval;
    }

    public Duration getMaxDuration() {
        return maxDuration;
    }

    public int getMaxSnapshots() {
        return Math.max(maxSnapshots, MIN_MAX_SNAPSHOTS);
    }

    public boolean isCpuProfilingEnabled() {
        return cpuProfilingEnabled;
    }

    public boolean isDebugEnvLogging() {
        return debugEnvLogging;
    }

    public CpuProfilingEvent getCpuProfilingEvent() {
        return cpuProfilingEvent;
    }

    public int getCpuSamplingIntervalMs() {
        // Clamp between 1ms and 100ms
        return Math.max(1, Math.min(100, cpuSamplingIntervalMs));
    }

    public int getSystemMetricsIntervalMs() {
        // Clamp between 100ms and 10000ms
        return Math.max(100, Math.min(10000, systemMetricsIntervalMs));
    }

    public int getMaxStackDepth() {
        // Clamp between 32 and 512
        return Math.max(32, Math.min(512, maxStackDepth));
    }

    public enum CpuProfilingEvent {
        CPU,
        WALL;

        public static CpuProfilingEvent fromString(String value) {
            if (value == null) {
                return CPU;
            }
            for (CpuProfilingEvent event : values()) {
                if (event.name().equalsIgnoreCase(value)) {
                    return event;
                }
            }
            return CPU;
        }
    }
}
