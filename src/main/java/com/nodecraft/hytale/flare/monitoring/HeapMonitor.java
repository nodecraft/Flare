package com.nodecraft.hytale.flare.monitoring;

import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.HeapMetrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public final class HeapMonitor {
    private final MonitorConfig config;
    private final MemoryMXBean memoryBean;

    public HeapMonitor(MonitorConfig config) {
        this.config = config;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public HeapMetrics collect() {
        if (!isEnabled()) {
            return null;
        }
        return HeapMetrics.fromMemoryUsage(memoryBean.getHeapMemoryUsage());
    }
}
