package com.nodecraft.hytale.flare.model;

public record NetworkMetrics(
    long totalSentPackets,
    long totalReceivedPackets,
    long totalSentUncompressedBytes,
    long totalReceivedUncompressedBytes,
    long totalSentCompressedBytes,
    long totalReceivedCompressedBytes,
    long sinceStartSentPackets,
    long sinceStartReceivedPackets,
    long sinceStartSentUncompressedBytes,
    long sinceStartReceivedUncompressedBytes,
    long sinceStartSentCompressedBytes,
    long sinceStartReceivedCompressedBytes,
    boolean profileActive,
    long sinceProfileSentPackets,
    long sinceProfileReceivedPackets,
    long sinceProfileSentUncompressedBytes,
    long sinceProfileReceivedUncompressedBytes,
    long sinceProfileSentCompressedBytes,
    long sinceProfileReceivedCompressedBytes
) {}
