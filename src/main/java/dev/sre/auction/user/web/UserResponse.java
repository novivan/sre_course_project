package dev.sre.auction.user.web;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.user.domain.User;

public record UserResponse(UUID id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.createdAt());
    }
}
