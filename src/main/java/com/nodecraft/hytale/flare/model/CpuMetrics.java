package com.nodecraft.hytale.flare.model;

public record CpuMetrics(
    double processCpuLoad,
    double systemCpuLoad,
    long availableProcessors,
    boolean cpuMonitoringAvailable
) {
    public static CpuMetrics unavailable() {
        return new CpuMetrics(-1.0, -1.0, Runtime.getRuntime().availableProcessors(), false);
    }
}
