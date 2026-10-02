package dev.sre.auction.user.domain;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String username, Instant createdAt) {
}
