package com.nodecraft.hytale.performance_diagnostics.model;

public record TpsMetrics(
    double currentTps,
    double averageTps,
    double minTps,
    double maxTps
) {}
