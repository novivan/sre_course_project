package dev.sre.auction.auction.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAuctionRequest(
        @NotNull UUID lotId,
        @NotNull UUID sellerId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @PositiveOrZero long startPriceMinor,
        @Positive long minIncrementMinor,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
