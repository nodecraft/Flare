package com.nodecraft.hytale.flare.profiler;

import com.nodecraft.hytale.flare.config.ProfilerConfig;
import com.nodecraft.hytale.flare.model.PerformanceSnapshot;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProfilerSession {
    private final ProfilerData data;
    private final ProfilerConfig config;
    private final AtomicBoolean active;
    private ProfilerPreamble postamble;
    private ScheduledFuture<?> samplingTask;
    private final Runnable samplingCallback;
    private final ScheduledExecutorService profilerExecutor;

    public ProfilerSession(
            ProfilerMetadata metadata,
            ProfilerPreamble preamble,
            ProfilerConfig config,
            Runnable samplingCallback,
            ScheduledExecutorService profilerExecutor
    ) {
        this.config = config;
        this.data = new ProfilerData(metadata, preamble, Instant.now(), config.getSamplingInterval());
        this.active = new AtomicBoolean(true);
        this.samplingCallback = samplingCallback;
        this.profilerExecutor = profilerExecutor;
    }

    public void startSampling(java.util.concurrent.ScheduledExecutorService executor) {
        if (samplingTask != null) {
            return;
        }

        long intervalSeconds = config.getSamplingInterval().getSeconds();       
        samplingTask = profilerExecutor.scheduleAtFixedRate(
                () -> {
                    if (isActive()) {
                        samplingCallback.run();
                    }
                },
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        active.set(false);
        if (samplingTask != null) {
            samplingTask.cancel(false);
            samplingTask = null;
        }
    }

    public boolean isActive() {
        return active.get() && !isMaxDurationReached() && !isMaxSnapshotsReached();
    }

    public boolean isMaxDurationReached() {
        if (data.startTime() == null) {
            return false;
        }
        Duration maxDuration = config.getMaxDuration();
        if (maxDuration == null || maxDuration.isZero() || maxDuration.isNegative()) {
            return false;
        }
        Duration elapsed = Duration.between(data.startTime(), Instant.now());   
        return elapsed.compareTo(maxDuration) >= 0;
    }

    public boolean isMaxSnapshotsReached() {
        return data.getSnapshotCount() >= config.getMaxSnapshots();
    }

    public void addSnapshot(PerformanceSnapshot snapshot) {
        if (isActive()) {
            data.addSnapshot(snapshot);
        }
    }

    public void setPostamble(ProfilerPreamble postamble) {
        this.postamble = postamble;
    }

    public ProfilerData getData() {
        Instant endTime = isActive() ? null : Instant.now();
        ProfilerData snapshot = data.withEndTime(endTime);
        return postamble != null ? snapshot.withPostamble(postamble) : snapshot;
    }

    public Instant getStartTime() {
        return data.startTime();
    }

    public int getSnapshotCount() {
        return data.getSnapshotCount();
    }
}
