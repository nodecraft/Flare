package com.nodecraft.hytale.flare.profiler;

public record ConfigFileDump(
    String path,
    String contents,
    String error
) {}
