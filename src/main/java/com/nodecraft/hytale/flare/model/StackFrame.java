package com.nodecraft.hytale.flare.model;

/**
 * Represents a single frame in a stack trace.
 */
public record StackFrame(
    String className,
    String methodName,
    String fileName,
    int lineNumber
) {
    public StackFrame {
        if (className == null) className = "unknown";
        if (methodName == null) methodName = "unknown";
        if (fileName == null) fileName = "unknown";
    }
    
    @Override
    public String toString() {
        return className + "." + methodName + "(" + fileName + ":" + lineNumber + ")";
    }
}
