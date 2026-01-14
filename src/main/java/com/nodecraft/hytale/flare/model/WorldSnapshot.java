package com.nodecraft.hytale.flare.model;

public record WorldSnapshot(
    String name,
    boolean ticking,
    boolean paused,
    int loadedChunks,
    int totalGeneratedChunks,
    int totalLoadedChunks,
    int entityCount,
    int archetypeChunkCount,
    double avgTickNanos
) {}
