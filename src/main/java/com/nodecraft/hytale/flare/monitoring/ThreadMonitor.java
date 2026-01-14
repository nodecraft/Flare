package com.nodecraft.hytale.flare.monitoring;

import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.ThreadMetrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ThreadMonitor {
    private final MonitorConfig config;
    private final ThreadMXBean threadBean;

    public ThreadMonitor(MonitorConfig config) {
        this.config = config;
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public ThreadMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        // Use lightweight aggregate methods - these don't require thread suspension
        int totalThreads = threadBean.getThreadCount();
        int peakThreads = threadBean.getPeakThreadCount();
        long totalStartedThreads = threadBean.getTotalStartedThreadCount();
        int daemonThreads = threadBean.getDaemonThreadCount();

        // For passive profiling, we avoid expensive getThreadInfo() calls that may suspend threads
        // Instead, we use a lightweight approach: get thread info without stack traces
        // This is much faster and doesn't require thread suspension
        Map<Thread.State, Integer> threadsByState = new HashMap<>();
        for (Thread.State state : Thread.State.values()) {
            threadsByState.put(state, 0);
        }

        // For passive profiling, we can skip detailed thread state counting if there are many threads
        // This is the most expensive operation in thread monitoring
        long[] allThreadIds = threadBean.getAllThreadIds();
        if (allThreadIds != null && allThreadIds.length > 0) {
            // Only collect thread states if thread count is reasonable (< 200 threads)
            // For high thread counts, this operation becomes too expensive
            if (allThreadIds.length < 200) {
                // Batch get thread info without stack traces for better performance
                java.lang.management.ThreadInfo[] threadInfos = threadBean.getThreadInfo(allThreadIds, 0);
                if (threadInfos != null) {
                    for (java.lang.management.ThreadInfo threadInfo : threadInfos) {
                        if (threadInfo != null) {
                            Thread.State state = threadInfo.getThreadState();
                            threadsByState.put(state, threadsByState.get(state) + 1);
                        }
                    }
                }
            } else {
                // Too many threads - skip state counting to avoid overhead
                // Just initialize with zeros (already done above)
            }
        }

        // Check for deadlocks - this is already lightweight and non-blocking
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        List<Long> deadlockedThreadList = deadlockedThreads != null
                ? Arrays.stream(deadlockedThreads).boxed().collect(Collectors.toList())
                : List.of();

        return new ThreadMetrics(
                totalThreads,
                peakThreads,
                totalStartedThreads,
                daemonThreads,
                threadsByState,
                deadlockedThreadList
        );
    }
}
