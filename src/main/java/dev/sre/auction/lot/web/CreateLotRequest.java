package dev.sre.auction.lot.web;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLotRequest(
        @NotNull UUID sellerId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10_000) String description,
        @Size(max = 2_000) String imageUrl
) {
}
