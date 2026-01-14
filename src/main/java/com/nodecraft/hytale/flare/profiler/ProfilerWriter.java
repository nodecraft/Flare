package com.nodecraft.hytale.flare.profiler;

import com.github.luben.zstd.Zstd;
import com.nodecraft.hytale.flare.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ProfilerWriter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    private ProfilerWriter() {}

    public static Path writeProfilerData(ProfilerData data, Path profilesDirectory) throws IOException {
        if (!Files.exists(profilesDirectory)) {
            Files.createDirectories(profilesDirectory);
        }

        String timestamp = DATE_FORMATTER.format(data.startTime());
        String filename = "profile_" + timestamp + ".json";
        Path filePath = profilesDirectory.resolve(filename);

        String json = JsonUtil.toJson(data);
        Files.writeString(filePath, json);

        return filePath;
    }

    public static Path writeCompressedReport(ProfilerData data, Path profilesDirectory) throws IOException {
        Path jsonPath = writeProfilerData(data, profilesDirectory);
        try {
            byte[] input = Files.readAllBytes(jsonPath);
            byte[] compressed = Zstd.compress(input);
            String baseName = jsonPath.getFileName().toString().replaceAll("\\.json$", "");
            Path reportPath = jsonPath.resolveSibling(baseName + ".flarereport");
            Files.write(reportPath, compressed);
            Files.deleteIfExists(jsonPath);
            return reportPath;
        } catch (Exception e) {
            return null;
        }
    }
}
