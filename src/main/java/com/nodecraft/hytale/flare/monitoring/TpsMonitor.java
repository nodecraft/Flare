package com.nodecraft.hytale.flare.monitoring;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.commands.world.perf.WorldPerfCommand;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.TpsMetrics;

import java.util.Map;

public final class TpsMonitor {
    private final MonitorConfig config;
    private double lastTps = 20.0;
    private double minTps = 20.0;
    private double maxTps = 20.0;
    private double sumTps = 0.0;
    private int tpsSamples = 0;

    public TpsMonitor(MonitorConfig config) {
        this.config = config;
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
            return new TpsMetrics(20.0, 20.0, 20.0, 20.0);
        }

        // Get TPS from default world or first available world
        World defaultWorld = Universe.get().getDefaultWorld();
        if (defaultWorld == null && !worlds.isEmpty()) {
            defaultWorld = worlds.values().iterator().next();
        }

        if (defaultWorld == null) {
            return new TpsMetrics(20.0, 20.0, 20.0, 20.0);
        }

        double currentTps = getTPS(defaultWorld);
        lastTps = currentTps;
        minTps = Math.min(minTps, currentTps);
        maxTps = Math.max(maxTps, currentTps);
        sumTps += currentTps;
        tpsSamples++;

        double averageTps = tpsSamples > 0 ? sumTps / tpsSamples : currentTps;

        return new TpsMetrics(currentTps, averageTps, minTps, maxTps);
    }

    private double getTPS(World world) {
        final long tickStepNanos = world.getTickStepNanos();
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        return WorldPerfCommand.tpsFromDelta(metrics.getAverage(0), tickStepNanos);
    }
}
