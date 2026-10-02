package dev.sre.auction.lot.web;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.lot.domain.Lot;

public record LotResponse(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        String imageUrl,
        Instant createdAt
) {
    public static LotResponse from(Lot lot) {
        return new LotResponse(
                lot.id(),
                lot.sellerId(),
                lot.title(),
                lot.description(),
                lot.imageUrl(),
                lot.createdAt()
        );
    }
}
