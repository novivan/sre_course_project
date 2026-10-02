package dev.sre.auction.auction.web;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.auction.domain.Bid;

public record BidResponse(
        UUID id,
        UUID auctionId,
        UUID bidderId,
        long amountMinor,
        UUID idempotencyKey,
        Instant createdAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.id(),
                bid.auctionId(),
                bid.bidderId(),
                bid.amountMinor(),
                bid.idempotencyKey(),
                bid.createdAt()
        );
    }
}
