package dev.sre.auction.auction.application;

import java.util.UUID;

public record PlaceBidCommand(
        UUID auctionId,
        UUID bidderId,
        long amountMinor,
        UUID idempotencyKey
) {
}
