package com.nodecraft.hytale.flare.profiler;

import java.time.Instant;
import java.util.List;

public record ProfilerPreamble(
    Instant capturedAt,
    ConfigFileDump serverConfig,
    List<WorldConfigDump> worldConfigs
) {}
