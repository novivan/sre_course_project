package dev.sre.auction.auction.domain;

import java.time.Instant;
import java.util.UUID;

public record Bid(
        UUID id,
        UUID auctionId,
        UUID bidderId,
        long amountMinor,
        UUID idempotencyKey,
        Instant createdAt
) {
}
