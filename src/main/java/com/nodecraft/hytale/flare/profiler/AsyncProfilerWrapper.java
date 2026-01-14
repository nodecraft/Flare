package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.logger.HytaleLogger;
import com.nodecraft.hytale.flare.model.CpuProfileData;
import com.nodecraft.hytale.flare.model.StackFrame;
import com.nodecraft.hytale.flare.model.StackSample;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper around async-profiler that provides a clean API and handles
 * cases where async-profiler is not available (e.g., Windows, missing library).
 * 
 * Uses reflection to access async-profiler API to avoid hard dependency.
 */
public final class AsyncProfilerWrapper {
    private final HytaleLogger logger;
    private final boolean available;
    private final AtomicBoolean isProfiling = new AtomicBoolean(false);
    private final AtomicReference<Instant> startTime = new AtomicReference<>();
    private final AtomicReference<Integer> samplingInterval = new AtomicReference<>();
    
    // Reflection references to async-profiler classes
    private Object asyncProfilerInstance;
    private Method startMethod;
    private Method stopMethod;
    private Method executeMethod;
    private Method getInstanceMethod;

    private AsyncProfilerWrapper(HytaleLogger logger, boolean available) {
        this.logger = logger;
        this.available = available;
        
        if (available) {
            initializeAsyncProfiler();
        }
    }

    /**
     * Checks if async-profiler is available on this platform.
     */
    public static boolean isAvailable() {
        return NativeLibraryLoader.isPlatformSupported();
    }

    /**
     * Creates an AsyncProfilerWrapper instance.
     * Will attempt to load async-profiler if platform is supported.
     * With AP-Loader, native library loading is handled automatically.
     */
    public static AsyncProfilerWrapper create(HytaleLogger logger) {
        boolean platformSupported = NativeLibraryLoader.isPlatformSupported();
        
        if (!platformSupported) {
            logger.atInfo().log("Async-profiler not supported on this platform (Windows)");
            return new AsyncProfilerWrapper(logger, false);
        }

        // With AP-Loader, we don't need to manually load native libraries
        // AP-Loader handles extraction and loading automatically
        // Just create the wrapper and let initialization handle it
        return new AsyncProfilerWrapper(logger, true);
    }

    /**
     * Initializes async-profiler using AP-Loader or direct API.
     * AP-Loader handles native library loading automatically.
     */
    private void initializeAsyncProfiler() {
        try {
            // First, try to use AP-Loader (recommended approach)
            try {
                Class<?> loaderClass = Class.forName("one.profiler.AsyncProfilerLoader");
                Method loadMethod = loaderClass.getMethod("load");
                asyncProfilerInstance = loadMethod.invoke(null);
                logger.atInfo().log("Async-profiler loaded via AP-Loader");
            } catch (ClassNotFoundException e) {
                // AP-Loader not available, try direct AsyncProfiler
                Class<?> profilerClass = Class.forName("one.profiler.AsyncProfiler");
                getInstanceMethod = profilerClass.getMethod("getInstance");
                asyncProfilerInstance = getInstanceMethod.invoke(null);
                logger.atInfo().log("Async-profiler loaded directly (AP-Loader not found)");
            }
            
            if (asyncProfilerInstance == null) {
                logger.atWarning().log("Failed to obtain async-profiler instance");
                return;
            }
            
            // Get methods from the profiler instance
            Class<?> profilerClass = asyncProfilerInstance.getClass();
            
            // Get start() method - signature: start(String event, long interval)
            startMethod = profilerClass.getMethod("start", String.class, long.class);
            
            // Get stop() method
            stopMethod = profilerClass.getMethod("stop");
            
            // Get execute() method - signature: execute(String command)
            executeMethod = profilerClass.getMethod("execute", String.class);
            
            logger.atInfo().log("Async-profiler initialized successfully");
        } catch (ClassNotFoundException e) {
            logger.atWarning().log("Async-profiler classes not found. CPU profiling will be disabled.");
            logger.atWarning().log("Note: ap-loader dependency may need to be added to build.gradle.kts");
        } catch (Exception e) {
            logger.atWarning().log("Failed to initialize async-profiler: %s", e.getMessage());
            // Log full exception details at warning level
            logger.atWarning().log("Exception details: %s", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Starts CPU profiling with the specified sampling interval.
     * 
     * @param intervalMs Sampling interval in milliseconds (1-100ms recommended)
     * @return true if profiling started successfully, false otherwise
     */
    public boolean start(int intervalMs) {
        if (!available || asyncProfilerInstance == null) {
            return false;
        }

        if (isProfiling.get()) {
            logger.atWarning().log("Profiling already in progress");
            return false;
        }

        try {
            // async-profiler start("cpu", intervalMs)
            startMethod.invoke(asyncProfilerInstance, "cpu", (long) intervalMs);
            isProfiling.set(true);
            startTime.set(Instant.now());
            samplingInterval.set(intervalMs);
            logger.atInfo().log("Started async-profiler with %dms sampling interval", intervalMs);
            return true;
        } catch (Exception e) {
            logger.atSevere().log("Failed to start async-profiler: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Stops profiling and returns the collected data.
     * 
     * @return CpuProfileData containing stack traces, or null if unavailable
     */
    public CpuProfileData stop() {
        if (!available || !isProfiling.get() || asyncProfilerInstance == null) {
            return null;
        }

        try {
            Instant endTime = Instant.now();
            Instant start = startTime.get();
            int interval = samplingInterval.get() != null ? samplingInterval.get() : 20;
            
            // Get output in collapsed format BEFORE stopping
            String output = null;
            try {
                // Get output while still profiling
                output = (String) executeMethod.invoke(asyncProfilerInstance, "collapsed");
            } catch (Exception e) {
                logger.atWarning().log("Failed to get collapsed output: %s", e.getMessage());
            }
            
            // Now stop the profiler
            try {
                stopMethod.invoke(asyncProfilerInstance);
            } catch (Exception e) {
                logger.atWarning().log("Failed to stop profiler: %s", e.getMessage());
            }
            
            // Parse output into stack samples
            List<StackSample> samples = new ArrayList<>();
            Map<String, Long> hotspots = new HashMap<>();
            
            if (output != null && !output.trim().isEmpty()) {
                samples = parseCollapsedFormat(output, start, interval);
                hotspots = calculateHotspots(samples);
                logger.atInfo().log("Stopped async-profiler. Collected %d stack samples", samples.size());
            } else {
                logger.atWarning().log("No profiling output received from async-profiler");
            }
            
            isProfiling.set(false);
            
            // CpuProfileData constructor will automatically calculate time and percentages
            return new CpuProfileData(start, endTime, interval, samples, hotspots);
        } catch (Exception e) {
            logger.atSevere().log("Failed to stop async-profiler: %s", e.getMessage());
            // Try to stop anyway
            try {
                if (stopMethod != null && asyncProfilerInstance != null) {
                    stopMethod.invoke(asyncProfilerInstance);
                }
            } catch (Exception stopException) {
                // Ignore
            }
            isProfiling.set(false);
            return null;
        }
    }

    /**
     * Parses async-profiler's collapsed stack format into StackSample objects.
     * 
     * Format: stackTrace;sampleCount
     * Example: com.example.Class.method(File.java:123);42
     */
    private List<StackSample> parseCollapsedFormat(String output, Instant startTime, int intervalMs) {
        List<StackSample> samples = new ArrayList<>();
        
        if (output == null || output.trim().isEmpty()) {
            return samples;
        }

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Handle different formats from async-profiler:
            // 1. Collapsed format: frame1;frame2;frame3;count (semicolon-separated)
            // 2. Single frame format: frame count (space-separated, often for native code)
            // 3. Some lines may have spaces in frame names, so we need to be careful
            
            String stackTraceStr = null;
            String countStr = null;
            boolean parsed = false;
            
            // Try to find the count - it should be the last token that's a number
            // First, try semicolon format (standard collapsed format)
            int lastSemicolonIndex = line.lastIndexOf(';');
            if (lastSemicolonIndex != -1) {
                String afterSemicolon = line.substring(lastSemicolonIndex + 1).trim();
                try {
                    // Check if it's a number
                    Long.parseLong(afterSemicolon);
                    // It's a number, so format is: stackTrace;count
                    stackTraceStr = line.substring(0, lastSemicolonIndex);
                    countStr = afterSemicolon;
                    parsed = true;
                } catch (NumberFormatException e) {
                    // Not a number after semicolon, might be part of frame name
                    // Will try space-separated format below
                }
            }
            
            // If semicolon format didn't work, try space-separated format
            if (!parsed) {
                int lastSpaceIndex = line.lastIndexOf(' ');
                if (lastSpaceIndex == -1) {
                    // No separator found, skip this line
                    continue;
                }
                
                String afterSpace = line.substring(lastSpaceIndex + 1).trim();
                try {
                    // Check if it's a number
                    Long.parseLong(afterSpace);
                    // It's a number, so format is: frame count
                    stackTraceStr = line.substring(0, lastSpaceIndex);
                    countStr = afterSpace;
                    parsed = true;
                } catch (NumberFormatException e) {
                    // Not a number, can't parse this line
                    continue;
                }
            }
            
            // At this point, we should have both variables set, but check to be safe
            if (!parsed || stackTraceStr == null || countStr == null) {
                continue;
            }
            
            try {
                long count = Long.parseLong(countStr);
                
                // Parse stack trace frames (separated by semicolons in collapsed format)
                List<StackFrame> frames = parseStackTrace(stackTraceStr);
                
                // Create sample - use current time as timestamp (approximate)
                Instant sampleTime = startTime.plusMillis(samples.size() * intervalMs);
                StackSample sample = new StackSample(
                    sampleTime,
                    "unknown", // Thread name not in collapsed format
                    0, // Thread ID not in collapsed format
                    frames,
                    count
                );
                
                samples.add(sample);
            } catch (NumberFormatException e) {
                // Skip invalid lines
                logger.atWarning().log("Failed to parse sample count: %s", countStr);
            }
        }

        return samples;
    }

    /**
     * Parses a stack trace string into StackFrame objects.
     * Format: className.methodName(fileName:lineNumber)
     */
    private List<StackFrame> parseStackTrace(String stackTraceStr) {
        List<StackFrame> frames = new ArrayList<>();
        
        if (stackTraceStr == null || stackTraceStr.isEmpty()) {
            return frames;
        }

        // Split by semicolon (collapsed format uses semicolons between frames)
        String[] frameStrs = stackTraceStr.split(";");
        
        for (String frameStr : frameStrs) {
            frameStr = frameStr.trim();
            if (frameStr.isEmpty()) {
                continue;
            }

            // Parse format: className.methodName(fileName:lineNumber)
            StackFrame frame = parseFrame(frameStr);
            if (frame != null) {
                frames.add(frame);
            }
        }

        return frames;
    }

    /**
     * Parses a single frame string into a StackFrame.
     * Format: className.methodName(fileName:lineNumber)
     */
    private StackFrame parseFrame(String frameStr) {
        try {
            // Find the last dot (separates class and method)
            int lastDot = frameStr.lastIndexOf('.');
            if (lastDot == -1) {
                return null;
            }

            String className = frameStr.substring(0, lastDot);
            String methodAndFile = frameStr.substring(lastDot + 1);

            // Find opening parenthesis
            int openParen = methodAndFile.indexOf('(');
            if (openParen == -1) {
                return new StackFrame(className, methodAndFile, "unknown", 0);
            }

            String methodName = methodAndFile.substring(0, openParen);
            String fileAndLine = methodAndFile.substring(openParen + 1);
            
            // Remove closing parenthesis
            if (fileAndLine.endsWith(")")) {
                fileAndLine = fileAndLine.substring(0, fileAndLine.length() - 1);
            }

            // Split file and line number
            int colonIndex = fileAndLine.lastIndexOf(':');
            String fileName;
            int lineNumber = 0;
            
            if (colonIndex != -1) {
                fileName = fileAndLine.substring(0, colonIndex);
                try {
                    lineNumber = Integer.parseInt(fileAndLine.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    // Line number parsing failed, use 0
                }
            } else {
                fileName = fileAndLine.isEmpty() ? "unknown" : fileAndLine;
            }

            return new StackFrame(className, methodName, fileName, lineNumber);
        } catch (Exception e) {
            logger.atWarning().log("Failed to parse frame: %s", frameStr);
            return null;
        }
    }

    /**
     * Calculates method hotspots from stack samples.
     */
    private Map<String, Long> calculateHotspots(List<StackSample> samples) {
        Map<String, Long> hotspots = new HashMap<>();
        
        for (StackSample sample : samples) {
            for (StackFrame frame : sample.stackTrace()) {
                String methodKey = frame.className() + "." + frame.methodName();
                hotspots.merge(methodKey, sample.sampleCount(), Long::sum);
            }
        }
        
        return hotspots;
    }

    /**
     * Checks if profiling is currently active.
     */
    public boolean isProfiling() {
        return isProfiling.get();
    }

    /**
     * Checks if async-profiler is available and initialized.
     */
    public boolean isInitialized() {
        return available && asyncProfilerInstance != null;
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (isProfiling.get()) {
            stop();
        }
        // Native library cleanup is handled by NativeLibraryLoader
    }
}
