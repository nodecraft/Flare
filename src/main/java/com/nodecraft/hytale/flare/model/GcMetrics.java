package com.nodecraft.hytale.flare.model;

import java.util.List;

public record GcMetrics(
    List<GcCollectorInfo> collectors,
    long totalCollections,
    long totalCollectionTime,
    double averagePauseTime,
    long lastCollectionTime,
    long lastCollectionDuration
) {
    public record GcCollectorInfo(
        String name,
        long collectionCount,
        long collectionTime,
        double averagePauseTime
    ) {}
}
