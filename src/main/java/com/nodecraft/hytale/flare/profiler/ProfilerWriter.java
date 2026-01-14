package com.nodecraft.hytale.flare.profiler;

import com.github.luben.zstd.Zstd;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ProfilerWriter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    private ProfilerWriter() {}

    public static Path writeCompressedReport(ProfilerData data, Path profilesDirectory) throws IOException {
        if (!Files.exists(profilesDirectory)) {
            Files.createDirectories(profilesDirectory);
        }

        Instant startTime = data.startTime() != null ? data.startTime() : Instant.now();
        String timestamp = DATE_FORMATTER.format(startTime);
        String filename = "profile_" + timestamp + ".flarereport";
        Path reportPath = profilesDirectory.resolve(filename);

        try {
            byte[] protoBytes = ProfilerReportMapper.toProto(data).toByteArray();
            byte[] compressed = Zstd.compress(protoBytes);
            Files.write(reportPath, compressed);
            return reportPath;
        } catch (Exception e) {
            return null;
        }
    }
}
