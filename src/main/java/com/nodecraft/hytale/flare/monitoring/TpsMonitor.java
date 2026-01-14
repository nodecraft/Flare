package com.nodecraft.hytale.flare.monitoring;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.TpsMetrics;

import java.util.Map;

public final class TpsMonitor {
    private final MonitorConfig config;
    private double lastTps = Double.NaN;
    private double minTps = Double.NaN;
    private double maxTps = Double.NaN;
    private double sumTps = 0.0;
    private int tpsSamples = 0;
    private double lastAvgTickNanos = 0.0;
    private long lastTickStepNanos = 0L;
    private HytaleLogger logger;

    public TpsMonitor(MonitorConfig config, HytaleLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public TpsMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        Map<String, World> worlds = Universe.get().getWorlds();
        if (worlds.isEmpty()) {
            logger.atWarning().log("No worlds found. TPS will be set to 20.0");
            return new TpsMetrics(20.0, 20.0, 20.0, 20.0);
        }

        // Get TPS from default world or first available world
        World defaultWorld = Universe.get().getDefaultWorld();
        if (defaultWorld == null && !worlds.isEmpty()) {
            defaultWorld = worlds.values().iterator().next();
        }

        if (defaultWorld == null) {
            logger.atWarning().log("No default world found. TPS will be set to 20.0");
            return new TpsMetrics(20.0, 20.0, 20.0, 20.0);
        }

        double currentTps = getTPS(defaultWorld);
        lastTps = currentTps;
        if (tpsSamples == 0) {
            minTps = currentTps;
            maxTps = currentTps;
        } else {
            minTps = Math.min(minTps, currentTps);
            maxTps = Math.max(maxTps, currentTps);
        }
        sumTps += currentTps;
        tpsSamples++;

        double averageTps = tpsSamples > 0 ? sumTps / tpsSamples : currentTps;

        return new TpsMetrics(currentTps, averageTps, minTps, maxTps);
    }

    private double getTPS(World world) {
        long tickStepNanos = world.getTickStepNanos();
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        if (metrics == null) {
            logger.atWarning().log("No tick metrics found. TPS will use last known value.");
            return getFallbackTps(tickStepNanos);
        }

        double avgTickNanos = metrics.getAverage(0);
        if (avgTickNanos <= 0.0) {
            logger.atWarning().log("Invalid tick length metric. TPS will use last known value.");
            return getFallbackTps(tickStepNanos);
        }

        lastAvgTickNanos = avgTickNanos;
        lastTickStepNanos = tickStepNanos;
        return nanosToTps(Math.max(avgTickNanos, lastTickStepNanos));
    }

    private double getFallbackTps(long tickStepNanos) {
        if (Double.isNaN(lastTps)) {
            return nanosToTps(tickStepNanos);
        }
        return lastTps;
    }

    public double getLastAvgTickNanos() {
        return lastAvgTickNanos;
    }

    public long getLastTickStepNanos() {
        return lastTickStepNanos;
    }

    private static double nanosToTps(double tickNanos) {
        return 1_000_000_000.0 / tickNanos;
    }
}
