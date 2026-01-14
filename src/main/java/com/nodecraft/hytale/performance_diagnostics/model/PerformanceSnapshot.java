package com.nodecraft.hytale.performance_diagnostics.model;

import java.time.Instant;

public record PerformanceSnapshot(
    Instant timestamp,
    HeapMetrics heap,
    GcMetrics gc,
    ThreadMetrics threads,
    TpsMetrics tps,
    CpuMetrics cpu
) {}
