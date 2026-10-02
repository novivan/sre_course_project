package dev.sre.auction.platform.web;

import java.time.Instant;

public record ApiError(
        String code,
        String message,
        String requestId,
        Instant timestamp
) {
}
