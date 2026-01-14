package com.nodecraft.hytale.flare.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a single stack trace sample collected during profiling.
 */
public record StackSample(
    Instant timestamp,
    String threadName,
    long threadId,
    List<StackFrame> stackTrace,
    long sampleCount
) {
    public StackSample {
        if (threadName == null) threadName = "unknown";
        if (stackTrace == null) stackTrace = List.of();
        if (sampleCount < 1) sampleCount = 1;
    }
}
