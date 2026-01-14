# Flare

A comprehensive performance monitoring and profiling plugin for Hytale servers that provides real-time diagnostics and historical profiling data.

> [!WARNING]  
> This project is still in early development.

## Features

### Real-Time Monitoring
- **Heap Memory**: Track heap usage, committed memory, and usage ratios
- **Garbage Collection**: Monitor GC events, pause times, and collector statistics
- **Threads**: Track thread counts, states, and detect deadlocks
- **TPS**: Monitor server ticks per second with min/max/average tracking
- **CPU**: Monitor process and system CPU usage (when available via JMX)
- **Worlds**: Capture loaded chunks, entities, and per-world tick info
- **Network**: Track sent/received packets and byte counts

### Performance Profiling
- Start/stop profiling sessions to collect metrics over time
- Configurable snapshot intervals (default: 1 second)
- Automatic session limits (max duration, max snapshots)
- Async-profiler CPU sampling (default: 4ms) with CPU/WALL event support
- Profile files saved to `mods/Flare/profiles/` as compressed `.flarereport`
- Preamble/postamble capture of server + world configs (sensitive fields redacted)

## Installation

1. Build the plugin:
   ```bash
   ./gradlew build
   ```

2. Place the generated JAR file from `build/libs/` into your server's `mods/` folder.

## Usage

### Commands

The plugin provides a `/flare` command with the following subcommands:

- `/flare` or `/flare status` - Show current performance metrics snapshot
- `/flare info` - Show plugin information
- `/flare heap` - Show detailed heap memory status
- `/flare gc` - Show garbage collection statistics
- `/flare threads` - Show thread information and deadlock detection
- `/flare tps` - Show TPS information
- `/flare cpu` - Show CPU usage (if available)
- `/flare network` - Show network traffic statistics
- `/flare profile start [timeoutSeconds]` - Start a profiling session (auto-stop after N seconds)
- `/flare profile stop` - Stop the current profiling session
- `/flare profile status` - Show profiling session status

### Profiling

To start profiling:
```
/flare profile start
```

To auto-stop after 30 seconds:
```
/flare profile start 30
```

This will begin collecting performance snapshots at the configured interval (default: 1 second). The profiler will automatically stop when:
- Maximum duration is reached (default: 1 hour)
- Maximum snapshots are reached (default: 3600)
- You manually stop it with `/flare profile stop`

Profile data is saved as `.flarereport` files in `mods/Flare/profiles/` with filenames like `profile_YYYY-MM-DD_HH-MM-SS.flarereport`. Reports are zstd-compressed protobuf payloads.

To convert a report to JSON for debugging:
```bash
python scripts/convert_flarereport.py mods/Flare/profiles/profile_*.flarereport out.json
```

## Configuration

The plugin can be configured via a JSON configuration file at `mods/Flare/config.json`. If the file doesn't exist, default values will be used.

### Configuration Structure

```json
{
  "HeapMonitor": {
    "Enabled": true
  },
  "GcMonitor": {
    "Enabled": true
  },
  "ThreadMonitor": {
    "Enabled": true
  },
  "TpsMonitor": {
    "Enabled": true
  },
  "CpuMonitor": {
    "Enabled": true
  },
  "NetworkMonitor": {
    "Enabled": true
  },
  "WorldMonitor": {
    "Enabled": true
  },
  "Profiler": {
    "SamplingIntervalSeconds": 1,
    "MaxDurationSeconds": 3600,
    "MaxSnapshots": 3600,
    "CpuProfilingEnabled": true,
    "CpuProfilingEvent": "CPU",
    "CpuSamplingIntervalMs": 4,
    "SystemMetricsIntervalMs": 1000,
    "MaxStackDepth": 128,
    "DebugEnvLogging": false
  }
}
```

### Configuration Options

#### Monitor Configs (HeapMonitor, GcMonitor, ThreadMonitor, TpsMonitor, CpuMonitor)
- `Enabled` (boolean, default: `true`) - Enable/disable the monitor

#### Profiler Config
- `SamplingIntervalSeconds` (integer, default: `1`) - How often to collect snapshots (minimum: 1 second)
- `MaxDurationSeconds` (integer, default: `3600`) - Maximum duration for a profiling session
- `MaxSnapshots` (integer, default: `3600`) - Maximum number of snapshots to collect (minimum: 100)
- `CpuProfilingEnabled` (boolean, default: `true`) - Enable async-profiler CPU sampling
- `CpuProfilingEvent` (`CPU` or `WALL`, default: `CPU`) - Profiling event type (auto-falls back to `WALL` when perf is restricted)
- `CpuSamplingIntervalMs` (integer, default: `4`) - Sampling interval for async-profiler
- `SystemMetricsIntervalMs` (integer, default: `1000`) - System metrics polling interval
- `MaxStackDepth` (integer, default: `128`) - Max stack depth for sampled traces
- `DebugEnvLogging` (boolean, default: `false`) - Log detailed perf/container environment info

## Building

### Prerequisites
- Java 25 JDK
- Gradle 8.5+ (or use the included Gradle wrapper)
- HytaleServer JAR file

### Setting Up HytaleServer Dependency

The HytaleServer API is not available in public Maven repositories. You need to provide it locally:

**Option 1: Place JAR in libs/ directory (Recommended)**
1. Obtain `HytaleServer.jar` from your Hytale server installation
2. Place it in the `libs/` directory: `libs/HytaleServer.jar`
3. The build will automatically use this local JAR


### Build Commands

```bash
# Build the plugin JAR
./gradlew build

# Clean build artifacts
./gradlew clean

# Build and clean
./gradlew clean build
```

If you're using IntelliJ IDEA, we automatically create a build configuration that lets you run a server locally with the plugin preloaded.

The built JAR will be in `build/libs/Flare-1.0.0.jar`.

## Project Structure

```
src/main/java/com/nodecraft/hytale/flare/
├── FlarePlugin.java                      # Main plugin class
├── commands/
│   └── DiagnosticsCommand.java           # Command handler
├── config/
│   ├── FlareConfig.java                  # Main configuration
│   ├── MonitorConfig.java                # Monitor configuration
│   └── ProfilerConfig.java               # Profiler configuration
├── model/
│   ├── CpuMetrics.java                   # CPU metrics data model
│   ├── GcMetrics.java                    # GC metrics data model
│   ├── HeapMetrics.java                  # Heap metrics data model
│   ├── NetworkMetrics.java               # Network metrics data model
│   ├── PerformanceSnapshot.java          # Complete snapshot model
│   ├── ThreadMetrics.java                # Thread metrics data model
│   ├── TpsMetrics.java                   # TPS metrics data model
│   ├── WorldMetrics.java                 # World metrics data model
│   └── WorldSnapshot.java                # Per-world snapshot data
├── monitoring/
│   ├── CpuMonitor.java                   # CPU monitoring
│   ├── GcMonitor.java                    # GC monitoring
│   ├── HeapMonitor.java                  # Heap monitoring
│   ├── NetworkMonitor.java               # Network monitoring
│   ├── ThreadMonitor.java                # Thread monitoring
│   ├── TpsMonitor.java                   # TPS monitoring
│   └── WorldMonitor.java                 # World monitoring
├── profiler/
│   ├── PerformanceProfiler.java          # Main profiler coordinator
│   ├── ProfilerData.java                 # Profiler data model
│   ├── ProfilerPreambleCollector.java    # Config dumps + redaction
│   ├── ProfilerReportMapper.java         # Protobuf mapping
│   ├── ProfilerSession.java              # Active session management
│   └── ProfilerWriter.java               # Report writer
└── util/
    ├── InstantAdapter.java               # Gson adapter for Instant
    ├── JmxUtil.java                      # JMX utility functions
    └── JsonUtil.java                     # JSON serialization utilities
```

## License

Flare is licensed follow the [MIT](LICENSE) license

## Contributing

Follow our [contribution guidelines](CONTRIBUTING.md) to get started!
