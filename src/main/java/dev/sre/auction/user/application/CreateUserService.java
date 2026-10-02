package dev.sre.auction.user.application;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

import dev.sre.auction.platform.error.InvalidRequestException;
import dev.sre.auction.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserService {

    private final UserRepository users;
    private final Clock clock;

    public CreateUserService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public User execute(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim().toLowerCase(Locale.ROOT);
        if (username.length() < 3 || username.length() > 64) {
            throw new InvalidRequestException(
                    "INVALID_USERNAME",
                    "Username length must be between 3 and 64 characters"
            );
        }

        User user = new User(UUID.randomUUID(), username, clock.instant());
        users.insert(user);
        return user;
    }
}
