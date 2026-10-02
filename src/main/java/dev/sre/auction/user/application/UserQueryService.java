package dev.sre.auction.user.application;

import java.util.List;

import dev.sre.auction.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {

    private final UserRepository users;

    public UserQueryService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<User> list(int limit) {
        return users.findAll(Math.min(Math.max(limit, 1), 100));
    }
}
