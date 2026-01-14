package com.nodecraft.hytale.flare.model;

import java.time.Instant;

public record PerformanceSnapshot(
    Instant timestamp,
    HeapMetrics heap,
    GcMetrics gc,
    ThreadMetrics threads,
    TpsMetrics tps,
    CpuMetrics cpu,
    WorldMetrics world,
    NetworkMetrics network
) {}
