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
            .build();

    private static final Duration MIN_SAMPLING_INTERVAL = Duration.ofSeconds(1);
    private static final int MIN_MAX_SNAPSHOTS = 100;

    private Duration samplingInterval = Duration.ofSeconds(1);
    private Duration maxDuration = Duration.ofHours(1);
    private int maxSnapshots = 3600; // 1 hour at 1 second intervals

    public Duration getSamplingInterval() {
        return samplingInterval.compareTo(MIN_SAMPLING_INTERVAL) < 0 ? MIN_SAMPLING_INTERVAL : samplingInterval;
    }

    public Duration getMaxDuration() {
        return maxDuration;
    }

    public int getMaxSnapshots() {
        return Math.max(maxSnapshots, MIN_MAX_SNAPSHOTS);
    }
}
