package com.nodecraft.hytale.flare.profiler;

import com.hypixel.hytale.server.core.HytaleServerConfig;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProfilerPreambleCollector {
    private static final String REDACTED_VALUE = "***redacted***";
    private static final Set<String> SENSITIVE_KEYS = Set.of("password");
    private static final Gson GSON = new Gson();

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
            String redacted = redactSensitiveJson(contents);
            return new ConfigFileDump(path.toString(), redacted, null);
        } catch (Exception e) {
            return new ConfigFileDump(path.toString(), null, e.getMessage());
        }
    }

    private static String redactSensitiveJson(String contents) {
        try {
            JsonElement element = JsonParser.parseString(contents);
            redactSensitiveFields(element);
            return GSON.toJson(element);
        } catch (Exception e) {
            return contents;
        }
    }

    private static void redactSensitiveFields(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Set<String> keys = new HashSet<>(obj.keySet());
            for (String key : keys) {
                String lowered = key.toLowerCase();
                JsonElement value = obj.get(key);
                if (SENSITIVE_KEYS.contains(lowered)) {
                    obj.add(key, new JsonPrimitive(REDACTED_VALUE));
                } else {
                    redactSensitiveFields(value);
                }
            }
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                redactSensitiveFields(entry);
            }
        }
    }
}
