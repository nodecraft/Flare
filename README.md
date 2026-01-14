# Flare

A comprehensive performance monitoring and profiling plugin for Hytale servers that provides real-time diagnostics and historical profiling data.

## Note

This project is still in early stages, much like Hytale. Things may not work properly, and there may be some bugs.

## Features

### Real-Time Monitoring
- **Heap Memory**: Track heap usage, committed memory, and usage ratios
- **Garbage Collection**: Monitor GC events, pause times, and collector statistics
- **Threads**: Track thread counts, states, and detect deadlocks
- **TPS**: Monitor server ticks per second with min/max/average tracking
- **CPU**: Monitor process and system CPU usage (when available via JMX)

### Performance Profiling
- Start/stop profiling sessions to collect metrics over time
- Configurable sampling intervals (default: 1 second)
- Automatic session limits (max duration, max snapshots)
- JSON output for easy analysis and visualization
- Profile files saved to `mods/Flare/profiles/`

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
- `/flare heap` - Show detailed heap memory status
- `/flare gc` - Show garbage collection statistics
- `/flare threads` - Show thread information and deadlock detection
- `/flare tps` - Show TPS information
- `/flare cpu` - Show CPU usage (if available)
- `/flare profile start [duration]` - Start a profiling session
- `/flare profile stop` - Stop the current profiling session
- `/flare profile status` - Show profiling session status

### Profiling

To start profiling:
```
/flare profile start
```

This will begin collecting performance snapshots at the configured interval (default: 1 second). The profiler will automatically stop when:
- Maximum duration is reached (default: 1 hour)
- Maximum snapshots are reached (default: 3600)
- You manually stop it with `/flare profile stop`

Profile data is saved as JSON files in `mods/Flare/profiles/` with filenames like `profile_YYYY-MM-DD_HH-MM-SS.json`.

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
  "Profiler": {
    "SamplingIntervalSeconds": 1,
    "MaxDurationSeconds": 3600,
    "MaxSnapshots": 3600
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

**Option 2: Install to Local Maven Repository**
```bash
mvn install:install-file \
  -Dfile=HytaleServer.jar \
  -DgroupId=com.hypixel.hytale \
  -DartifactId=HytaleServer-parent \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar
```

### Build Commands

```bash
# Build the plugin JAR
./gradlew shadowJar

# Clean build artifacts
./gradlew clean

# Build and clean
./gradlew clean shadowJar
```

The built JAR will be in `build/libs/Flare-1.0.0-SNAPSHOT.jar`.

## Project Structure

```
src/main/java/com/nodecraft/hytale/performance_diagnostics/
├── PerformanceDiagnosticsPlugin.java    # Main plugin class
├── commands/
│   └── DiagnosticsCommand.java         # Command handler
├── config/
│   ├── MonitorConfig.java               # Monitor configuration
│   ├── PerformanceDiagnosticsConfig.java # Main configuration
│   └── ProfilerConfig.java              # Profiler configuration
├── model/
│   ├── CpuMetrics.java                  # CPU metrics data model
│   ├── GcMetrics.java                   # GC metrics data model
│   ├── HeapMetrics.java                 # Heap metrics data model
│   ├── PerformanceSnapshot.java         # Complete snapshot model
│   ├── ThreadMetrics.java               # Thread metrics data model
│   └── TpsMetrics.java                  # TPS metrics data model
├── monitoring/
│   ├── CpuMonitor.java                  # CPU monitoring
│   ├── GcMonitor.java                   # GC monitoring
│   ├── HeapMonitor.java                 # Heap monitoring
│   ├── ThreadMonitor.java               # Thread monitoring
│   └── TpsMonitor.java                  # TPS monitoring
├── profiler/
│   ├── PerformanceProfiler.java        # Main profiler coordinator
│   ├── ProfilerData.java                # Profiler data model
│   ├── ProfilerSession.java             # Active session management
│   └── ProfilerWriter.java              # JSON file writer
└── util/
    ├── InstantAdapter.java              # Gson adapter for Instant
    ├── JmxUtil.java                     # JMX utility functions
    └── JsonUtil.java                     # JSON serialization utilities
```

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
