package dev.sre.auction.auction.web;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.AuctionStatus;

public record AuctionResponse(
        UUID id,
        UUID lotId,
        AuctionStatus status,
        String currency,
        long startPriceMinor,
        long currentPriceMinor,
        long minIncrementMinor,
        Instant startsAt,
        Instant endsAt,
        UUID currentLeaderId,
        UUID winnerId,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.id(),
                auction.lotId(),
                auction.status(),
                auction.currency(),
                auction.startPriceMinor(),
                auction.currentPriceMinor(),
                auction.minIncrementMinor(),
                auction.startsAt(),
                auction.endsAt(),
                auction.currentLeaderId(),
                auction.winnerId(),
                auction.version(),
                auction.createdAt(),
                auction.updatedAt()
        );
    }
}
