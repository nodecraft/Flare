package com.nodecraft.hytale.flare.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.validator.EqualValidator;
import com.hypixel.hytale.codec.validation.validator.OrValidator;
import com.hypixel.hytale.codec.validation.validator.RangeValidator;

import java.time.Duration;

public class ProfilerConfig {
    public static final BuilderCodec<ProfilerConfig> CODEC = BuilderCodec.builder(ProfilerConfig.class, ProfilerConfig::new)
            .append(
                    new KeyedCodec<>("SamplingIntervalSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.samplingInterval = value,
                    config -> config.samplingInterval
            )
            .addValidator(new RangeValidator<>(Duration.ofSeconds(1), Duration.ofSeconds(Integer.MAX_VALUE), true))
            .documentation("The frequency in seconds of how often Flare samples core metrics")
            .add()
            .append(
                    new KeyedCodec<>("MaxDurationSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.maxDuration = value,
                    config -> config.maxDuration
            )
            .addValidator(new RangeValidator<>(Duration.ofSeconds(1), Duration.ofHours(24), true))
            .documentation("The maximum duration of a Flare profile")
            .add()
            .append(
                    new KeyedCodec<>("MaxSnapshots", Codec.INTEGER),
                    (config, value) -> config.maxSnapshots = value,
                    config -> config.maxSnapshots
            )
            .addValidator(new RangeValidator<>(100, Integer.MAX_VALUE, true))
            .documentation("The maximum number of snapshots to keep in memory")
            .add()
            .append(
                    new KeyedCodec<>("CpuProfilingEnabled", Codec.BOOLEAN),     
                    (config, value) -> config.cpuProfilingEnabled = value,      
                    config -> config.cpuProfilingEnabled
            )
            .documentation("Enables CPU profiling for Flare")
            .add()
            .append(
                    new KeyedCodec<>("DebugEnvLogging", Codec.BOOLEAN),
                    (config, value) -> config.debugEnvLogging = value,
                    config -> config.debugEnvLogging
            )
            .documentation("Enables debug logging in the environment when CPU profiling is enabled")
            .add()
            .append(
                    new KeyedCodec<>("CpuProfilingEvent", Codec.STRING),
                    (config, value) -> config.cpuProfilingEvent = CpuProfilingEvent.fromString(value),
                    config -> config.cpuProfilingEvent.name()
            )
            .addValidator(new OrValidator<String>(new Validator[]{
                    new EqualValidator<>("CPU"),
                    new EqualValidator<>("WALL")
            }))
            .documentation("The CPU profiling event to use. CPU is the default, WALL is wall-clock time. CPU may not work in container environments.")
            .add()
            .append(
                    new KeyedCodec<>("CpuSamplingIntervalMs", Codec.INTEGER),   
                    (config, value) -> config.cpuSamplingIntervalMs = value,    
                    config -> config.cpuSamplingIntervalMs
            )
            .addValidator(new RangeValidator<>(1, 1000, true))
            .documentation("The sampling interval in milliseconds for CPU profiling. Lower values result in higher fidelity of data in exchange for slightly higher CPU usage.")
            .add()
            .append(
                    new KeyedCodec<>("SystemMetricsIntervalMs", Codec.INTEGER),
                    (config, value) -> config.systemMetricsIntervalMs = value,
                    config -> config.systemMetricsIntervalMs
            )
            .addValidator(new RangeValidator<>(100, 10000, true))
            .documentation("The interval in milliseconds at which system metrics are sampled. Lower values result in higher fidelity of data in exchange for higher CPU usage. Compared to the CPU profiler this is heavier.")
            .add()
            .append(
                    new KeyedCodec<>("MaxStackDepth", Codec.INTEGER),
                    (config, value) -> config.maxStackDepth = value,
                    config -> config.maxStackDepth
            )
            .addValidator(new RangeValidator<>(32, Integer.MAX_VALUE, true))
            .documentation("The maximum stack depth to capture for Flare profiles. Higher values result in higher memory usage and larger reports.")
            .add()
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
        return cpuSamplingIntervalMs;
    }

    public int getSystemMetricsIntervalMs() {
        return systemMetricsIntervalMs;
    }

    public int getMaxStackDepth() {
        return maxStackDepth;
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
