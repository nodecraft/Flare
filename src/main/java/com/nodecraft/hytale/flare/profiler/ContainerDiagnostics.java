package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.logger.HytaleLogger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ContainerDiagnostics {
    private ContainerDiagnostics() {}

    public static void logAsyncProfilerEnvironment(HytaleLogger logger) {
        String osName = System.getProperty("os.name", "");
        if (!osName.toLowerCase(Locale.ROOT).contains("linux")) {
            return;
        }

        logSysctl(logger, "kernel.perf_event_paranoid", "/proc/sys/kernel/perf_event_paranoid");
        logSysctl(logger, "kernel.kptr_restrict", "/proc/sys/kernel/kptr_restrict");
        logSysctl(logger, "kernel.perf_event_max_sample_rate", "/proc/sys/kernel/perf_event_max_sample_rate");
        logSysctl(logger, "kernel.perf_event_mlock_kb", "/proc/sys/kernel/perf_event_mlock_kb");
        logFile(logger, "cgroup", "/proc/1/cgroup");
        logFile(logger, "mountinfo", "/proc/1/mountinfo");
        logCapEff(logger, "/proc/self/status");
    }

    public static boolean isCpuProfilingRestricted() {
        String osName = System.getProperty("os.name", "");
        if (!osName.toLowerCase(Locale.ROOT).contains("linux")) {
            return false;
        }

        Long paranoid = readLong("/proc/sys/kernel/perf_event_paranoid");
        Long sampleRate = readLong("/proc/sys/kernel/perf_event_max_sample_rate");
        Long mlockKb = readLong("/proc/sys/kernel/perf_event_mlock_kb");
        String capEff = readStatusField("/proc/self/status", "CapEff");

        if (paranoid != null && paranoid >= 3) {
            return true;
        }
        if (sampleRate != null && sampleRate <= 1) {
            return true;
        }
        if (mlockKb != null && mlockKb <= 32) {
            return true;
        }
        return capEff != null && capEff.equals("0000000000000000");
    }

    private static void logSysctl(HytaleLogger logger, String name, String path) {
        String value = readSingleLine(path);
        logger.atInfo().log("Async-profiler env %s=%s", name, value != null ? value : "unavailable");
    }

    private static void logFile(HytaleLogger logger, String name, String path) {
        String value = readSingleLine(path);
        logger.atInfo().log("Async-profiler env %s=%s", name, value != null ? value : "unavailable");
    }

    private static void logCapEff(HytaleLogger logger, String path) {
        String capEff = readStatusField(path, "CapEff");
        logger.atInfo().log("Async-profiler env CapEff=%s", capEff != null ? capEff : "unavailable");
    }

    private static String readSingleLine(String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            return null;
        }
    }

    private static String readStatusField(String path, String field) {
        try {
            String[] lines = Files.readString(Path.of(path), StandardCharsets.UTF_8).split("\n");
            for (String line : lines) {
                if (line.startsWith(field + ":")) {
                    return line.substring(field.length() + 1).trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Long readLong(String path) {
        String value = readSingleLine(path);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
