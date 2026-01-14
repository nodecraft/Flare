package com.nodecraft.hytale.flare.model;

public record HeapMetrics(
    long used,
    long max,
    long committed,
    long free,
    double usageRatio
) {
    public static HeapMetrics fromMemoryUsage(java.lang.management.MemoryUsage usage) {
        long used = usage.getUsed();
        long max = usage.getMax();
        long committed = usage.getCommitted();
        long free = max > 0 ? max - used : committed - used;
        double usageRatio = max > 0 ? (double) used / max : (double) used / committed;
        return new HeapMetrics(used, max, committed, free, usageRatio);
    }
}
