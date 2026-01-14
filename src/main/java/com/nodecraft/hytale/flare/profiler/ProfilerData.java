package com.nodecraft.hytale.flare.profiler;

import com.nodecraft.hytale.flare.model.PerformanceSnapshot;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record ProfilerData(
    ProfilerMetadata metadata,
    Instant startTime,
    Instant endTime,
    Duration duration,
    Duration samplingInterval,
    List<PerformanceSnapshot> snapshots
) {
    public ProfilerData(ProfilerMetadata metadata, Instant startTime, Duration samplingInterval) {
        this(metadata, startTime, null, null, samplingInterval, new ArrayList<>());
    }

    public ProfilerData withEndTime(Instant endTime) {
        Duration duration = endTime != null && startTime != null
                ? Duration.between(startTime, endTime)
                : null;
        // Create a copy of the snapshots list to maintain immutability
        return new ProfilerData(metadata, startTime, endTime, duration, samplingInterval, new ArrayList<>(snapshots));
    }

    public void addSnapshot(PerformanceSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public int getSnapshotCount() {
        return snapshots.size();
    }
}
