package com.nodecraft.hytale.flare.profiler;

import java.time.Instant;

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
        String cpuModel,
        int availableProcessors,
        long maxMemory,
        String jvmArgs
    ) {}
}
