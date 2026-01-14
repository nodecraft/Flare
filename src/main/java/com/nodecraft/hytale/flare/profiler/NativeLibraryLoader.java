package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Handles loading of native libraries for async-profiler.
 * Extracts native libraries from JAR resources and loads them at runtime.
 */
public final class NativeLibraryLoader {
    private static final String NATIVE_RESOURCE_PREFIX = "native/";
    private static volatile boolean libraryLoaded = false;
    private static Path tempLibraryPath = null;

    private NativeLibraryLoader() {}

    /**
     * Attempts to load the async-profiler native library for the current platform.
     * 
     * Note: With AP-Loader, this method may not be needed as AP-Loader handles
     * native library loading automatically. This is kept for fallback scenarios.
     * 
     * @param logger Logger for error messages
     * @return true if the library was successfully loaded, false otherwise
     */
    public static boolean loadAsyncProfiler(HytaleLogger logger) {
        if (libraryLoaded) {
            return true;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String libName = determineLibraryName(os, arch);
        if (libName == null) {
            logger.atWarning().log("Async-profiler not supported on platform: %s %s", os, arch);
            return false;
        }

        try {
            // Try to load from system library path first
            try {
                System.loadLibrary("asyncProfiler");
                libraryLoaded = true;
                logger.atInfo().log("Loaded async-profiler from system library path");
                return true;
            } catch (UnsatisfiedLinkError e) {
                // System library not found, try extracting from JAR
            }

            // Extract and load from JAR resources (fallback if AP-Loader not used)
            Path extractedLib = extractLibraryFromJar(libName, logger);
            if (extractedLib != null) {
                System.load(extractedLib.toAbsolutePath().toString());
                tempLibraryPath = extractedLib;
                libraryLoaded = true;
                logger.atInfo().log("Loaded async-profiler from extracted library: %s", extractedLib);
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            logger.atWarning().log("Failed to load async-profiler native library: %s", e.getMessage());
            logger.atInfo().log("Note: AP-Loader should handle native library loading automatically");
        } catch (Exception e) {
            logger.atWarning().log("Error loading async-profiler: %s", e.getMessage());
        }

        return false;
    }

    /**
     * Determines the native library name based on OS and architecture.
     */
    private static String determineLibraryName(String os, String arch) {
        boolean isLinux = os.contains("linux");
        boolean isMac = os.contains("mac") || os.contains("darwin");
        boolean isWindows = os.contains("windows");

        if (isWindows) {
            // Windows not supported by async-profiler
            return null;
        }

        // Normalize architecture
        String normalizedArch = normalizeArchitecture(arch);

        if (isLinux) {
            return "libasyncProfiler-linux-" + normalizedArch + ".so";
        } else if (isMac) {
            // macOS uses a universal binary that works on both x64 and aarch64
            return "libasyncProfiler.dylib";
        }

        return null;
    }

    /**
     * Normalizes architecture names to match async-profiler naming conventions.
     */
    private static String normalizeArchitecture(String arch) {
        // Handle common architecture aliases
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "x64";
        }
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "aarch64";
        }
        return arch;
    }

    /**
     * Extracts a native library from JAR resources to a temporary file.
     */
    private static Path extractLibraryFromJar(String libName, HytaleLogger logger) {
        String resourcePath = NATIVE_RESOURCE_PREFIX + libName;
        
        try (InputStream inputStream = NativeLibraryLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            
            if (inputStream == null) {
                logger.atWarning().log("Native library not found in resources: %s", resourcePath);
                return null;
            }

            // Create temp file with proper extension
            String extension = libName.contains(".so") ? ".so" : ".dylib";
            Path tempFile = Files.createTempFile("flare-asyncprofiler-", extension);
            tempFile.toFile().deleteOnExit();

            // Copy from JAR to temp file
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Make executable on Unix systems
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("chmod", "+x", tempFile.toString());
                    pb.start().waitFor();
                } catch (Exception e) {
                    // chmod failed, but library might still work
                    logger.atWarning().log("Failed to make library executable: %s", e.getMessage());
                }
            }

            return tempFile;
        } catch (IOException e) {
            logger.atWarning().log("Failed to extract native library: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Cleans up extracted native library files.
     */
    public static void cleanup() {
        if (tempLibraryPath != null) {
            try {
                Files.deleteIfExists(tempLibraryPath);
                tempLibraryPath = null;
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
        libraryLoaded = false;
    }

    /**
     * Checks if async-profiler is supported on the current platform.
     */
    public static boolean isPlatformSupported() {
        String os = System.getProperty("os.name").toLowerCase();
        return !os.contains("windows");
    }
}
