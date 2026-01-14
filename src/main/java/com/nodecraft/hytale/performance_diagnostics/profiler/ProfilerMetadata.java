package com.nodecraft.hytale.performance_diagnostics.profiler;

import java.time.Instant;
import java.util.Map;

public record ProfilerMetadata(
    int profileVersion,
    String pluginVersion,
    Instant profileCreatedAt,
    EnvironmentInfo environment
) {
    public record EnvironmentInfo(
        String javaVersion,
        String javaVendor,
        String javaVmName,
        String javaVmVersion,
        String osName,
        String osVersion,
        String osArchitecture,
        int availableProcessors,
        long maxMemory,
        String jvmArgs
    ) {}
}
