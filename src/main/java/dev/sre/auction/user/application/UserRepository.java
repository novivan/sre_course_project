package dev.sre.auction.user.application;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.user.domain.User;

public interface UserRepository {

    void insert(User user);

    Optional<User> findById(UUID id);

    List<User> findAll(int limit);
}
