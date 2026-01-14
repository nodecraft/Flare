package com.nodecraft.hytale.flare.util;

import com.nodecraft.hytale.flare.profiler.ProfilerMetadata;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
                        readCpuModel(),
                        runtime.availableProcessors(),
                        runtime.maxMemory(),
                        jvmArgs
                )
        );
    }

    private static String readCpuModel() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("linux")) {
            try {
                String[] lines = Files.readString(Path.of("/proc/cpuinfo"), StandardCharsets.UTF_8).split("\n");
                for (String line : lines) {
                    String[] parts = line.split("\\s+:\\s", 2);
                    if (parts.length == 2 && (parts[0].equals("model name") || parts[0].equals("Processor"))) {
                        return parts[1].trim();
                    }
                }
            } catch (Exception e) {
                return null;
            }
        } else if (osName.contains("windows")) {
            String wmic = readCommand(new String[]{"wmic", "cpu", "get", "name"});
            if (wmic != null) {
                if (wmic.startsWith("Name")) {
                    return wmic.substring(4).trim();
                }
                return wmic;
            }
        } else if (osName.contains("mac")) {
            String sysctl = readCommand(new String[]{"sysctl", "-n", "machdep.cpu.brand_string"});
            if (sysctl != null) {
                return sysctl;
            }
        }
        return null;
    }

    private static String readCommand(String[] command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (String line : output.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.equalsIgnoreCase("name")) {
                    continue;
                }
                return trimmed;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
