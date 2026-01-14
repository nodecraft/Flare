package com.nodecraft.hytale.flare.monitoring;

import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.nodecraft.hytale.flare.config.MonitorConfig;
import com.nodecraft.hytale.flare.model.NetworkMetrics;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkMonitor {
    private final MonitorConfig config;
    private final AtomicReference<NetworkTotals> baselineTotals = new AtomicReference<>();
    private final AtomicReference<NetworkTotals> profileBaseline = new AtomicReference<>();
    private volatile boolean profileActive;

    public NetworkMonitor(MonitorConfig config) {
        this.config = config;
        resetBaseline();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void resetBaseline() {
        baselineTotals.set(captureTotals());
    }

    public void beginProfile() {
        profileBaseline.set(captureTotals());
        profileActive = true;
    }

    public void endProfile() {
        profileActive = false;
    }

    public NetworkMetrics collect() {
        if (!isEnabled()) {
            return null;
        }

        NetworkTotals totals = captureTotals();
        NetworkTotals baseline = baselineTotals.get();
        NetworkTotals profileStart = profileBaseline.get();

        NetworkTotals sinceStart = baseline == null ? NetworkTotals.EMPTY : totals.diff(baseline);
        NetworkTotals sinceProfile = profileStart == null ? NetworkTotals.EMPTY : totals.diff(profileStart);

        return new NetworkMetrics(
            totals.sentPackets,
            totals.receivedPackets,
            totals.sentUncompressedBytes,
            totals.receivedUncompressedBytes,
            totals.sentCompressedBytes,
            totals.receivedCompressedBytes,
            sinceStart.sentPackets,
            sinceStart.receivedPackets,
            sinceStart.sentUncompressedBytes,
            sinceStart.receivedUncompressedBytes,
            sinceStart.sentCompressedBytes,
            sinceStart.receivedCompressedBytes,
            profileActive,
            sinceProfile.sentPackets,
            sinceProfile.receivedPackets,
            sinceProfile.sentUncompressedBytes,
            sinceProfile.receivedUncompressedBytes,
            sinceProfile.sentCompressedBytes,
            sinceProfile.receivedCompressedBytes
        );
    }

    private NetworkTotals captureTotals() {
        long sentPackets = 0;
        long receivedPackets = 0;
        long sentUncompressed = 0;
        long receivedUncompressed = 0;
        long sentCompressed = 0;
        long receivedCompressed = 0;

        List<PlayerRef> players = Universe.get().getPlayers();
        for (PlayerRef playerRef : players) {
            PacketStatsRecorder recorder = playerRef.getPacketHandler().getPacketStatsRecorder();
            if (recorder == null || recorder == PacketStatsRecorder.NOOP) {
                continue;
            }

            for (int i = 0; i < 512; i++) {
                PacketStatsRecorder.PacketStatsEntry entry = recorder.getEntry(i);
                if (entry == null || !entry.hasData()) {
                    continue;
                }
                sentPackets += entry.getSentCount();
                receivedPackets += entry.getReceivedCount();
                sentUncompressed += entry.getSentUncompressedTotal();
                receivedUncompressed += entry.getReceivedUncompressedTotal();
                sentCompressed += entry.getSentCompressedTotal();
                receivedCompressed += entry.getReceivedCompressedTotal();
            }
        }

        return new NetworkTotals(
            sentPackets,
            receivedPackets,
            sentUncompressed,
            receivedUncompressed,
            sentCompressed,
            receivedCompressed
        );
    }

    private static final class NetworkTotals {
        private static final NetworkTotals EMPTY = new NetworkTotals(0, 0, 0, 0, 0, 0);
        private final long sentPackets;
        private final long receivedPackets;
        private final long sentUncompressedBytes;
        private final long receivedUncompressedBytes;
        private final long sentCompressedBytes;
        private final long receivedCompressedBytes;

        private NetworkTotals(
            long sentPackets,
            long receivedPackets,
            long sentUncompressedBytes,
            long receivedUncompressedBytes,
            long sentCompressedBytes,
            long receivedCompressedBytes
        ) {
            this.sentPackets = sentPackets;
            this.receivedPackets = receivedPackets;
            this.sentUncompressedBytes = sentUncompressedBytes;
            this.receivedUncompressedBytes = receivedUncompressedBytes;
            this.sentCompressedBytes = sentCompressedBytes;
            this.receivedCompressedBytes = receivedCompressedBytes;
        }

        private NetworkTotals diff(NetworkTotals baseline) {
            return new NetworkTotals(
                Math.max(0, sentPackets - baseline.sentPackets),
                Math.max(0, receivedPackets - baseline.receivedPackets),
                Math.max(0, sentUncompressedBytes - baseline.sentUncompressedBytes),
                Math.max(0, receivedUncompressedBytes - baseline.receivedUncompressedBytes),
                Math.max(0, sentCompressedBytes - baseline.sentCompressedBytes),
                Math.max(0, receivedCompressedBytes - baseline.receivedCompressedBytes)
            );
        }
    }
}
