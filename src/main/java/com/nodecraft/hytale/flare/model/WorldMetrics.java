package com.nodecraft.hytale.flare.model;

import java.util.List;

public record WorldMetrics(
    int worldCount,
    int totalLoadedChunks,
    int totalEntities,
    List<WorldSnapshot> worlds
) {}
