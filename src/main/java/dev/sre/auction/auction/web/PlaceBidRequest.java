package dev.sre.auction.auction.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceBidRequest(
        @NotNull UUID bidderId,
        @Positive long amountMinor,
        @NotNull UUID idempotencyKey
) {
}
