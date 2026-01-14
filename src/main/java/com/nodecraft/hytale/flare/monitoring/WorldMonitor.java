package com.nodecraft.hytale.flare.monitoring;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.WorldMetrics;
import com.nodecraft.hytale.flare.model.WorldSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class WorldMonitor {
    private final MonitorConfig config;

    public WorldMonitor(MonitorConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public WorldMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        Map<String, World> worlds = Universe.get().getWorlds();
        if (worlds.isEmpty()) {
            return new WorldMetrics(0, 0, 0, List.of());
        }

        List<CompletableFuture<WorldSnapshot>> futures = new ArrayList<>(worlds.size());
        for (World world : worlds.values()) {
            futures.add(CompletableFuture.supplyAsync(() -> collectWorld(world), world)
                .exceptionally(ex -> null));
        }

        List<WorldSnapshot> snapshots = new ArrayList<>(worlds.size());
        for (CompletableFuture<WorldSnapshot> future : futures) {
            WorldSnapshot snapshot = future.join();
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }

        snapshots.sort(Comparator.comparing(WorldSnapshot::name));

        int totalLoadedChunks = 0;
        int totalEntities = 0;
        for (WorldSnapshot snapshot : snapshots) {
            totalLoadedChunks += snapshot.loadedChunks();
            totalEntities += snapshot.entityCount();
        }

        return new WorldMetrics(snapshots.size(), totalLoadedChunks, totalEntities, snapshots);
    }

    private WorldSnapshot collectWorld(World world) {
        ChunkStore chunkStore = world.getChunkStore();
        int loadedChunks = chunkStore.getLoadedChunksCount();
        int totalGeneratedChunks = chunkStore.getTotalGeneratedChunksCount();
        int totalLoadedChunks = chunkStore.getTotalLoadedChunksCount();

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        int entityCount = entityStore.getEntityCount();
        int archetypeChunkCount = entityStore.getArchetypeChunkCount();

        HistoricMetric tickMetrics = world.getBufferedTickLengthMetricSet();
        double avgTickNanos = tickMetrics != null ? tickMetrics.getAverage(0) : 0.0;

        return new WorldSnapshot(
            world.getName(),
            world.isTicking(),
            world.isPaused(),
            loadedChunks,
            totalGeneratedChunks,
            totalLoadedChunks,
            entityCount,
            archetypeChunkCount,
            avgTickNanos
        );
    }
}
