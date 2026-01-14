package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.server.core.HytaleServerConfig;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ProfilerPreambleCollector {
    private ProfilerPreambleCollector() {}

    public static ProfilerPreamble collect() {
        ConfigFileDump serverConfig = readConfig(HytaleServerConfig.PATH);
        List<WorldConfigDump> worldConfigs = readWorldConfigs();
        return new ProfilerPreamble(Instant.now(), serverConfig, worldConfigs);
    }

    private static List<WorldConfigDump> readWorldConfigs() {
        Map<String, World> worlds = Universe.get().getWorlds();
        List<WorldConfigDump> dumps = new ArrayList<>(worlds.size());
        for (World world : worlds.values()) {
            Path configPath = world.getSavePath().resolve("config.json");
            ConfigFileDump dump = readConfig(configPath);
            dumps.add(new WorldConfigDump(world.getName(), dump.path(), dump.contents(), dump.error()));
        }
        dumps.sort(Comparator.comparing(WorldConfigDump::worldName));
        return dumps;
    }

    private static ConfigFileDump readConfig(Path path) {
        if (path == null) {
            return new ConfigFileDump(null, null, "path was null");
        }
        if (!Files.exists(path)) {
            return new ConfigFileDump(path.toString(), null, "file not found");
        }
        try {
            String contents = Files.readString(path, StandardCharsets.UTF_8);
            return new ConfigFileDump(path.toString(), contents, null);
        } catch (Exception e) {
            return new ConfigFileDump(path.toString(), null, e.getMessage());
        }
    }
}
