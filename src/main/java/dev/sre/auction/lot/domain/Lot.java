package dev.sre.auction.lot.domain;

import java.time.Instant;
import java.util.UUID;

public record Lot(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        String imageUrl,
        Instant createdAt
) {
}
