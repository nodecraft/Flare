package com.nodecraft.hytale.flare.util;

import com.nodecraft.hytale.flare.profiler.ProfilerMetadata;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class EnvironmentInfoCollector {
    private static final int PROFILE_VERSION = 1;
    
    // JVM arguments that might contain sensitive information (PII)
    private static final List<String> SENSITIVE_ARG_PATTERNS = List.of(
            "-Duser.name",
            "-Duser.home",
            "-Djava.home",
            "-Duser.dir",
            "-Dfile.encoding",
            "-agentlib",
            "-javaagent",
            "-Xbootclasspath"
    );

    private EnvironmentInfoCollector() {}

    public static ProfilerMetadata createMetadata(String pluginVersion) {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Runtime runtime = Runtime.getRuntime();

        // Collect JVM arguments, but filter out potentially sensitive information
        List<String> filteredArgs = runtimeBean.getInputArguments().stream()
                .filter(arg -> {
                    // Filter out arguments that might contain PII
                    String lowerArg = arg.toLowerCase();
                    return SENSITIVE_ARG_PATTERNS.stream()
                            .noneMatch(pattern -> lowerArg.startsWith(pattern.toLowerCase()));
                })
                .collect(Collectors.toList());
        String jvmArgs = String.join(" ", filteredArgs);

        return new ProfilerMetadata(
                PROFILE_VERSION,
                pluginVersion,
                Instant.now(),
                new ProfilerMetadata.EnvironmentInfo(
                        System.getProperty("java.version"),
                        System.getProperty("java.vendor"),
                        System.getProperty("java.vm.name"),
                        System.getProperty("java.vm.version"),
                        System.getProperty("os.name"),
                        System.getProperty("os.version"),
                        System.getProperty("os.arch"),
                        runtime.availableProcessors(),
                        runtime.maxMemory(),
                        jvmArgs
                )
        );
    }
}
