package com.nodecraft.hytale.flare.model;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated CPU profiling data containing stack trace samples and method hotspots.
 */
public record CpuProfileData(
    Instant startTime,
    Instant endTime,
    int samplingIntervalMs,
    List<StackSample> samples,
    Map<String, Long> methodHotspots,  // method -> sample count
    Map<String, Double> methodTimeMs,  // method -> estimated time in milliseconds
    Map<String, Double> methodPercentages  // method -> percentage of total time
) {
    public CpuProfileData {
        if (samples == null) samples = List.of();
        if (methodHotspots == null) methodHotspots = Map.of();
        if (methodTimeMs == null) methodTimeMs = Map.of();
        if (methodPercentages == null) methodPercentages = Map.of();
        if (samplingIntervalMs < 1) samplingIntervalMs = 10;
    }
    
    public CpuProfileData(Instant startTime, Instant endTime, int samplingIntervalMs, 
                         List<StackSample> samples, Map<String, Long> methodHotspots) {
        this(startTime, endTime, samplingIntervalMs, samples, methodHotspots, 
             calculateMethodTimes(methodHotspots, samplingIntervalMs),
             calculateMethodPercentages(methodHotspots));
    }
    
    private static Map<String, Double> calculateMethodTimes(Map<String, Long> hotspots, int samplingIntervalMs) {
        Map<String, Double> times = new HashMap<>();
        for (Map.Entry<String, Long> entry : hotspots.entrySet()) {
            // Estimated time = sample count * sampling interval
            // This is approximate since sampling is statistical
            double estimatedTimeMs = entry.getValue() * samplingIntervalMs;
            times.put(entry.getKey(), estimatedTimeMs);
        }
        return times;
    }
    
    private static Map<String, Double> calculateMethodPercentages(Map<String, Long> hotspots) {
        Map<String, Double> percentages = new HashMap<>();
        if (hotspots.isEmpty()) {
            return percentages;
        }
        
        // Calculate total samples
        long totalSamples = hotspots.values().stream().mapToLong(Long::longValue).sum();
        if (totalSamples == 0) {
            return percentages;
        }
        
        // Calculate percentage for each method
        for (Map.Entry<String, Long> entry : hotspots.entrySet()) {
            double percentage = (entry.getValue().doubleValue() / totalSamples) * 100.0;
            percentages.put(entry.getKey(), percentage);
        }
        
        return percentages;
    }
    
    public boolean isEmpty() {
        return samples.isEmpty() && methodHotspots.isEmpty();
    }
    
    /**
     * Gets the total profiling duration.
     */
    public Duration getDuration() {
        if (startTime == null || endTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Gets the total number of samples collected.
     */
    public long getTotalSamples() {
        return methodHotspots.values().stream().mapToLong(Long::longValue).sum();
    }
}
