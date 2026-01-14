package com.nodecraft.hytale.performance_diagnostics.monitoring;

import com.nodecraft.hytale.performance_diagnostics.config.MonitorConfig;
import com.nodecraft.hytale.performance_diagnostics.model.CpuMetrics;
import com.nodecraft.hytale.performance_diagnostics.util.JmxUtil;

import java.lang.management.OperatingSystemMXBean;

public final class CpuMonitor {
    private final MonitorConfig config;
    private final OperatingSystemMXBean osBean;

    public CpuMonitor(MonitorConfig config) {
        this.config = config;
        this.osBean = JmxUtil.getOperatingSystemMXBean();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public CpuMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        if (!JmxUtil.isSunOperatingSystemMXBean(osBean)) {
            return CpuMetrics.unavailable();
        }

        double processCpuLoad = JmxUtil.getProcessCpuLoad(osBean);
        double systemCpuLoad = JmxUtil.getSystemCpuLoad(osBean);
        long availableProcessors = osBean.getAvailableProcessors();

        // CPU load can be negative if not available yet
        boolean available = processCpuLoad >= 0.0 || systemCpuLoad >= 0.0;

        return new CpuMetrics(
                processCpuLoad >= 0.0 ? processCpuLoad : -1.0,
                systemCpuLoad >= 0.0 ? systemCpuLoad : -1.0,
                availableProcessors,
                available
        );
    }
}
