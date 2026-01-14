package com.nodecraft.hytale.flare.profiler;

import com.nodecraft.hytale.flare.model.CpuProfileData;
import com.nodecraft.hytale.flare.model.PerformanceSnapshot;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record ProfilerData(
    ProfilerMetadata metadata,
    ProfilerPreamble preamble,
    ProfilerPreamble postamble,
    Instant startTime,
    Instant endTime,
    Duration duration,
    Duration samplingInterval,
    List<PerformanceSnapshot> snapshots,
    CpuProfileData cpuProfile
) {
    public ProfilerData(ProfilerMetadata metadata, ProfilerPreamble preamble, Instant startTime, Duration samplingInterval) {
        this(metadata, preamble, null, startTime, null, null, samplingInterval, new ArrayList<>(), null);
    }

    public ProfilerData withEndTime(Instant endTime) {
        Duration duration = endTime != null && startTime != null
                ? Duration.between(startTime, endTime)
                : null;
        // Create a copy of the snapshots list to maintain immutability
        return new ProfilerData(
                metadata,
                preamble,
                postamble,
                startTime,
                endTime,
                duration,
                samplingInterval,
                new ArrayList<>(snapshots),
                cpuProfile
        );
    }
    
    public ProfilerData withCpuProfile(CpuProfileData cpuProfile) {
        return new ProfilerData(
                metadata,
                preamble,
                postamble,
                startTime,
                endTime,
                duration,
                samplingInterval,
                new ArrayList<>(snapshots),
                cpuProfile
        );
    }

    public ProfilerData withPreamble(ProfilerPreamble preamble) {
        return new ProfilerData(
                metadata,
                preamble,
                postamble,
                startTime,
                endTime,
                duration,
                samplingInterval,
                new ArrayList<>(snapshots),
                cpuProfile
        );
    }

    public ProfilerData withPostamble(ProfilerPreamble postamble) {
        return new ProfilerData(
                metadata,
                preamble,
                postamble,
                startTime,
                endTime,
                duration,
                samplingInterval,
                new ArrayList<>(snapshots),
                cpuProfile
        );
    }

    public void addSnapshot(PerformanceSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public int getSnapshotCount() {
        return snapshots.size();
    }
}
