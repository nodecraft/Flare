# Native Libraries Directory

This directory is for manually bundling async-profiler native libraries if AP-Loader is not used.

## AP-Loader (Recommended)

The project uses **AP-Loader** (`one.profiler:ap-loader`) which automatically handles native library extraction and loading. You typically don't need to place files here when using AP-Loader.

## Manual Bundling (Fallback)

If you need to bundle native libraries manually (e.g., if AP-Loader is not available), place the appropriate library files here:

### Linux
- `libasyncProfiler-linux-x64.so` (for x86_64/amd64)
- `libasyncProfiler-linux-aarch64.so` (for ARM64)

### macOS
- `libasyncProfiler.dylib` (universal binary - works on both Intel and Apple Silicon)

### Downloading Libraries

1. Download async-profiler from: https://github.com/async-profiler/async-profiler/releases
2. Extract the archive
3. Copy the appropriate `.so` or `.dylib` file to this directory
4. The library will be automatically extracted and loaded at runtime

## Notes

- Windows is not supported by async-profiler
- The `NativeLibraryLoader` class will automatically detect and load the correct library for your platform
- Libraries are extracted to a temporary directory at runtime and cleaned up on shutdown
