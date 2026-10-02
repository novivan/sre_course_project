package dev.sre.auction.auction.application;

import java.time.Instant;
import java.util.UUID;

public record CreateAuctionCommand(
        UUID lotId,
        UUID sellerId,
        String currency,
        long startPriceMinor,
        long minIncrementMinor,
        Instant startsAt,
        Instant endsAt
) {
}
