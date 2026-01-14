package com.nodecraft.hytale.flare.model;

public record TpsMetrics(
    double currentTps,
    double averageTps,
    double minTps,
    double maxTps
) {}
