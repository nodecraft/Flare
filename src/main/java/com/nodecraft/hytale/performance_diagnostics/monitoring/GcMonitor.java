package com.nodecraft.hytale.performance_diagnostics.monitoring;

import com.nodecraft.hytale.performance_diagnostics.config.MonitorConfig;
import com.nodecraft.hytale.performance_diagnostics.model.GcMetrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class GcMonitor {
    private final MonitorConfig config;
    private final List<GarbageCollectorMXBean> gcBeans;

    public GcMonitor(MonitorConfig config) {
        this.config = config;
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public GcMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        List<GcMetrics.GcCollectorInfo> collectors = new ArrayList<>();
        long totalCollections = 0;
        long totalCollectionTime = 0;
        long lastCollectionTime = 0;
        long lastCollectionDuration = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            double avgPause = count > 0 ? (double) time / count : 0.0;

            collectors.add(new GcMetrics.GcCollectorInfo(
                    gcBean.getName(),
                    count,
                    time,
                    avgPause
            ));

            totalCollections += count;
            totalCollectionTime += time;

            // Track most recent collection (simplified - in real implementation would track via notifications)
            if (time > lastCollectionTime) {
                lastCollectionTime = time;
                lastCollectionDuration = time; // Simplified
            }
        }

        double averagePauseTime = totalCollections > 0
                ? (double) totalCollectionTime / totalCollections
                : 0.0;

        return new GcMetrics(
                collectors,
                totalCollections,
                totalCollectionTime,
                averagePauseTime,
                lastCollectionTime,
                lastCollectionDuration
        );
    }
}
