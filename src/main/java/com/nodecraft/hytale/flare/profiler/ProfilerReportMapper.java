package com.nodecraft.hytale.flare.profiler;

import com.nodecraft.hytale.flare.model.CpuMetrics;
import com.nodecraft.hytale.flare.model.CpuProfileData;
import com.nodecraft.hytale.flare.model.GcMetrics;
import com.nodecraft.hytale.flare.model.HeapMetrics;
import com.nodecraft.hytale.flare.model.NetworkMetrics;
import com.nodecraft.hytale.flare.model.PerformanceSnapshot;
import com.nodecraft.hytale.flare.model.StackFrame;
import com.nodecraft.hytale.flare.model.StackSample;
import com.nodecraft.hytale.flare.model.ThreadMetrics;
import com.nodecraft.hytale.flare.model.TpsMetrics;
import com.nodecraft.hytale.flare.model.WorldMetrics;
import com.nodecraft.hytale.flare.model.WorldSnapshot;
import com.nodecraft.hytale.flare.report.EnvironmentInfo;
import com.nodecraft.hytale.flare.report.GcCollectorInfo;
import com.nodecraft.hytale.flare.report.ThreadState;
import com.nodecraft.hytale.flare.report.ThreadStateCount;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ProfilerReportMapper {
    private ProfilerReportMapper() {}

    public static com.nodecraft.hytale.flare.report.ProfilerData toProto(
            com.nodecraft.hytale.flare.profiler.ProfilerData data) {
        com.nodecraft.hytale.flare.report.ProfilerData.Builder builder =
                com.nodecraft.hytale.flare.report.ProfilerData.newBuilder()
                .setStartTimeMillis(toEpochMillis(data.startTime()))
                .setEndTimeMillis(toEpochMillis(data.endTime()))
                .setDurationMillis(toMillis(data.duration()))
                .setSamplingIntervalMillis(toMillis(data.samplingInterval()));

        if (data.metadata() != null) {
            builder.setMetadata(toProto(data.metadata()));
        }
        if (data.preamble() != null) {
            builder.setPreamble(toProto(data.preamble()));
        }
        if (data.postamble() != null) {
            builder.setPostamble(toProto(data.postamble()));
        }
        if (data.snapshots() != null) {
            for (PerformanceSnapshot snapshot : data.snapshots()) {
                if (snapshot != null) {
                    builder.addSnapshots(toProto(snapshot));
                }
            }
        }
        if (data.cpuProfile() != null) {
            builder.setCpuProfile(toProto(data.cpuProfile()));
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.ProfilerMetadata toProto(
            com.nodecraft.hytale.flare.profiler.ProfilerMetadata metadata) {
        com.nodecraft.hytale.flare.report.ProfilerMetadata.Builder builder =
                com.nodecraft.hytale.flare.report.ProfilerMetadata.newBuilder()
                .setProfileVersion(metadata.profileVersion())
                .setPluginVersion(nullToEmpty(metadata.pluginVersion()))
                .setProfileCreatedAtMillis(toEpochMillis(metadata.profileCreatedAt()));

        if (metadata.environment() != null) {
            builder.setEnvironment(toProto(metadata.environment()));
        }

        return builder.build();
    }

    private static EnvironmentInfo toProto(com.nodecraft.hytale.flare.profiler.ProfilerMetadata.EnvironmentInfo environment) {
        return EnvironmentInfo.newBuilder()
                .setJavaVersion(nullToEmpty(environment.javaVersion()))
                .setJavaVendor(nullToEmpty(environment.javaVendor()))
                .setJavaVmName(nullToEmpty(environment.javaVmName()))
                .setJavaVmVersion(nullToEmpty(environment.javaVmVersion()))
                .setOsName(nullToEmpty(environment.osName()))
                .setOsVersion(nullToEmpty(environment.osVersion()))
                .setOsArchitecture(nullToEmpty(environment.osArchitecture()))
                .setCpuModel(nullToEmpty(environment.cpuModel()))
                .setAvailableProcessors(environment.availableProcessors())
                .setMaxMemory(environment.maxMemory())
                .setJvmArgs(nullToEmpty(environment.jvmArgs()))
                .build();
    }

    private static com.nodecraft.hytale.flare.report.ProfilerPreamble toProto(
            com.nodecraft.hytale.flare.profiler.ProfilerPreamble preamble) {
        com.nodecraft.hytale.flare.report.ProfilerPreamble.Builder builder =
                com.nodecraft.hytale.flare.report.ProfilerPreamble.newBuilder()
                .setCapturedAtMillis(toEpochMillis(preamble.capturedAt()));

        if (preamble.serverConfig() != null) {
            builder.setServerConfig(toProto(preamble.serverConfig()));
        }
        if (preamble.worldConfigs() != null) {
            for (WorldConfigDump worldConfig : preamble.worldConfigs()) {
                if (worldConfig != null) {
                    builder.addWorldConfigs(toProto(worldConfig));
                }
            }
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.ConfigFileDump toProto(
            com.nodecraft.hytale.flare.profiler.ConfigFileDump dump) {
        return com.nodecraft.hytale.flare.report.ConfigFileDump.newBuilder()
                .setPath(nullToEmpty(dump.path()))
                .setContents(nullToEmpty(dump.contents()))
                .setError(nullToEmpty(dump.error()))
                .build();
    }

    private static com.nodecraft.hytale.flare.report.WorldConfigDump toProto(
            com.nodecraft.hytale.flare.profiler.WorldConfigDump dump) {
        return com.nodecraft.hytale.flare.report.WorldConfigDump.newBuilder()
                .setWorldName(nullToEmpty(dump.worldName()))
                .setPath(nullToEmpty(dump.path()))
                .setContents(nullToEmpty(dump.contents()))
                .setError(nullToEmpty(dump.error()))
                .build();
    }

    private static com.nodecraft.hytale.flare.report.PerformanceSnapshot toProto(PerformanceSnapshot snapshot) {
        com.nodecraft.hytale.flare.report.PerformanceSnapshot.Builder builder =
                com.nodecraft.hytale.flare.report.PerformanceSnapshot.newBuilder()
                .setTimestampMillis(toEpochMillis(snapshot.timestamp()));

        if (snapshot.heap() != null) {
            builder.setHeap(toProto(snapshot.heap()));
        }
        if (snapshot.gc() != null) {
            builder.setGc(toProto(snapshot.gc()));
        }
        if (snapshot.threads() != null) {
            builder.setThreads(toProto(snapshot.threads()));
        }
        if (snapshot.tps() != null) {
            builder.setTps(toProto(snapshot.tps()));
        }
        if (snapshot.cpu() != null) {
            builder.setCpu(toProto(snapshot.cpu()));
        }
        if (snapshot.world() != null) {
            builder.setWorld(toProto(snapshot.world()));
        }
        if (snapshot.network() != null) {
            builder.setNetwork(toProto(snapshot.network()));
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.HeapMetrics toProto(HeapMetrics heap) {
        com.nodecraft.hytale.flare.report.HeapMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.HeapMetrics.newBuilder()
                .setUsed(heap.used())
                .setMax(heap.max())
                .setCommitted(heap.committed())
                .setFree(heap.free())
                .setUsageRatio(heap.usageRatio());
        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.GcMetrics toProto(GcMetrics gc) {
        com.nodecraft.hytale.flare.report.GcMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.GcMetrics.newBuilder()
                .setTotalCollections(gc.totalCollections())
                .setTotalCollectionTime(gc.totalCollectionTime())
                .setAveragePauseTime(gc.averagePauseTime())
                .setLastCollectionTime(gc.lastCollectionTime())
                .setLastCollectionDuration(gc.lastCollectionDuration());

        List<GcMetrics.GcCollectorInfo> collectors = gc.collectors();
        if (collectors != null) {
            for (GcMetrics.GcCollectorInfo collector : collectors) {
                if (collector != null) {
                    builder.addCollectors(toProto(collector));
                }
            }
        }

        return builder.build();
    }

    private static GcCollectorInfo toProto(GcMetrics.GcCollectorInfo collector) {
        return GcCollectorInfo.newBuilder()
                .setName(nullToEmpty(collector.name()))
                .setCollectionCount(collector.collectionCount())
                .setCollectionTime(collector.collectionTime())
                .setAveragePauseTime(collector.averagePauseTime())
                .build();
    }

    private static com.nodecraft.hytale.flare.report.ThreadMetrics toProto(ThreadMetrics threads) {
        com.nodecraft.hytale.flare.report.ThreadMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.ThreadMetrics.newBuilder()
                .setTotalThreads(threads.totalThreads())
                .setPeakThreads(threads.peakThreads())
                .setTotalStartedThreads(threads.totalStartedThreads())
                .setDaemonThreads(threads.daemonThreads());

        Map<Thread.State, Integer> byState = threads.threadsByState();
        if (byState != null) {
            for (Map.Entry<Thread.State, Integer> entry : byState.entrySet()) {
                ThreadState state = toProto(entry.getKey());
                int count = entry.getValue() != null ? entry.getValue() : 0;
                builder.addThreadsByState(ThreadStateCount.newBuilder()
                        .setState(state)
                        .setCount(count)
                        .build());
            }
        }

        List<Long> deadlocked = threads.deadlockedThreads();
        if (deadlocked != null) {
            builder.addAllDeadlockedThreads(deadlocked);
        }

        return builder.build();
    }

    private static ThreadState toProto(Thread.State state) {
        if (state == null) {
            return ThreadState.THREAD_STATE_UNSPECIFIED;
        }
        return switch (state) {
            case NEW -> ThreadState.THREAD_STATE_NEW;
            case RUNNABLE -> ThreadState.THREAD_STATE_RUNNABLE;
            case BLOCKED -> ThreadState.THREAD_STATE_BLOCKED;
            case WAITING -> ThreadState.THREAD_STATE_WAITING;
            case TIMED_WAITING -> ThreadState.THREAD_STATE_TIMED_WAITING;
            case TERMINATED -> ThreadState.THREAD_STATE_TERMINATED;
        };
    }

    private static com.nodecraft.hytale.flare.report.TpsMetrics toProto(TpsMetrics tps) {
        com.nodecraft.hytale.flare.report.TpsMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.TpsMetrics.newBuilder()
                .setCurrentTps(tps.currentTps())
                .setAverageTps(tps.averageTps())
                .setMinTps(tps.minTps())
                .setMaxTps(tps.maxTps());
        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.CpuMetrics toProto(CpuMetrics cpu) {
        return com.nodecraft.hytale.flare.report.CpuMetrics.newBuilder()
                .setProcessCpuLoad(cpu.processCpuLoad())
                .setSystemCpuLoad(cpu.systemCpuLoad())
                .setAvailableProcessors((int) cpu.availableProcessors())
                .setCpuMonitoringAvailable(cpu.cpuMonitoringAvailable())
                .build();
    }

    private static com.nodecraft.hytale.flare.report.WorldMetrics toProto(WorldMetrics world) {
        com.nodecraft.hytale.flare.report.WorldMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.WorldMetrics.newBuilder()
                .setWorldCount(world.worldCount())
                .setTotalLoadedChunks(world.totalLoadedChunks())
                .setTotalEntities(world.totalEntities());

        if (world.worlds() != null) {
            for (WorldSnapshot snapshot : world.worlds()) {
                if (snapshot != null) {
                    builder.addWorlds(toProto(snapshot));
                }
            }
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.WorldSnapshot toProto(WorldSnapshot snapshot) {
        com.nodecraft.hytale.flare.report.WorldSnapshot.Builder builder =
                com.nodecraft.hytale.flare.report.WorldSnapshot.newBuilder()
                .setName(nullToEmpty(snapshot.name()))
                .setTicking(snapshot.ticking())
                .setPaused(snapshot.paused())
                .setLoadedChunks(snapshot.loadedChunks())
                .setTotalGeneratedChunks(snapshot.totalGeneratedChunks())
                .setTotalLoadedChunks(snapshot.totalLoadedChunks())
                .setEntityCount(snapshot.entityCount())
                .setArchetypeChunkCount(snapshot.archetypeChunkCount())
                .setAvgTickNanos(snapshot.avgTickNanos());
        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.NetworkMetrics toProto(NetworkMetrics network) {
        com.nodecraft.hytale.flare.report.NetworkMetrics.Builder builder =
                com.nodecraft.hytale.flare.report.NetworkMetrics.newBuilder()
                .setTotalSentPackets(network.totalSentPackets())
                .setTotalReceivedPackets(network.totalReceivedPackets())
                .setTotalSentUncompressedBytes(network.totalSentUncompressedBytes())
                .setTotalReceivedUncompressedBytes(network.totalReceivedUncompressedBytes())
                .setTotalSentCompressedBytes(network.totalSentCompressedBytes())
                .setTotalReceivedCompressedBytes(network.totalReceivedCompressedBytes())
                .setSinceStartSentPackets(network.sinceStartSentPackets())
                .setSinceStartReceivedPackets(network.sinceStartReceivedPackets())
                .setSinceStartSentUncompressedBytes(network.sinceStartSentUncompressedBytes())
                .setSinceStartReceivedUncompressedBytes(network.sinceStartReceivedUncompressedBytes())
                .setSinceStartSentCompressedBytes(network.sinceStartSentCompressedBytes())
                .setSinceStartReceivedCompressedBytes(network.sinceStartReceivedCompressedBytes())
                .setProfileActive(network.profileActive())
                .setSinceProfileSentPackets(network.sinceProfileSentPackets())
                .setSinceProfileReceivedPackets(network.sinceProfileReceivedPackets())
                .setSinceProfileSentUncompressedBytes(network.sinceProfileSentUncompressedBytes())
                .setSinceProfileReceivedUncompressedBytes(network.sinceProfileReceivedUncompressedBytes())
                .setSinceProfileSentCompressedBytes(network.sinceProfileSentCompressedBytes())
                .setSinceProfileReceivedCompressedBytes(network.sinceProfileReceivedCompressedBytes());
        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.CpuProfileData toProto(CpuProfileData cpuProfile) {
        com.nodecraft.hytale.flare.report.CpuProfileData.Builder builder =
                com.nodecraft.hytale.flare.report.CpuProfileData.newBuilder()
                .setStartTimeMillis(toEpochMillis(cpuProfile.startTime()))
                .setEndTimeMillis(toEpochMillis(cpuProfile.endTime()))
                .setSamplingIntervalMs(cpuProfile.samplingIntervalMs());

        List<StackSample> samples = cpuProfile.samples();
        if (samples != null) {
            for (StackSample sample : samples) {
                if (sample != null) {
                    builder.addSamples(toProto(sample));
                }
            }
        }

        Map<String, Long> hotspots = cpuProfile.methodHotspots();
        if (hotspots != null) {
            builder.putAllMethodHotspots(hotspots);
        }

        Map<String, Double> methodTimes = cpuProfile.methodTimeMs();
        if (methodTimes != null) {
            builder.putAllMethodTimeMs(methodTimes);
        }

        Map<String, Double> methodPercentages = cpuProfile.methodPercentages();
        if (methodPercentages != null) {
            builder.putAllMethodPercentages(methodPercentages);
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.StackSample toProto(StackSample sample) {
        com.nodecraft.hytale.flare.report.StackSample.Builder builder =
                com.nodecraft.hytale.flare.report.StackSample.newBuilder()
                .setTimestampMillis(toEpochMillis(sample.timestamp()))
                .setThreadName(nullToEmpty(sample.threadName()))
                .setThreadId(sample.threadId())
                .setSampleCount(sample.sampleCount());

        List<StackFrame> frames = sample.stackTrace();
        if (frames != null) {
            for (StackFrame frame : frames) {
                if (frame != null) {
                    builder.addStackTrace(toProto(frame));
                }
            }
        }

        return builder.build();
    }

    private static com.nodecraft.hytale.flare.report.StackFrame toProto(StackFrame frame) {
        com.nodecraft.hytale.flare.report.StackFrame.Builder builder =
                com.nodecraft.hytale.flare.report.StackFrame.newBuilder()
                .setClassName(nullToEmpty(frame.className()))
                .setMethodName(nullToEmpty(frame.methodName()))
                .setFileName(nullToEmpty(frame.fileName()))
                .setLineNumber(frame.lineNumber());
        return builder.build();
    }

    private static long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : 0L;
    }

    private static long toMillis(Duration duration) {
        return duration != null ? duration.toMillis() : 0L;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
