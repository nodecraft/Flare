package com.nodecraft.hytale.performance_diagnostics.model;

import java.util.List;
import java.util.Map;

public record ThreadMetrics(
    int totalThreads,
    int peakThreads,
    long totalStartedThreads,
    int daemonThreads,
    Map<Thread.State, Integer> threadsByState,
    List<Long> deadlockedThreads
) {
    public boolean hasDeadlocks() {
        return deadlockedThreads != null && !deadlockedThreads.isEmpty();
    }
}
