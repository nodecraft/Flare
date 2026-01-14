package com.nodecraft.hytale.flare.profiler;

public record WorldConfigDump(
    String worldName,
    String path,
    String contents,
    String error
) {}
