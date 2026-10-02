package dev.sre.auction.user.web;

import java.net.URI;
import java.util.List;

import dev.sre.auction.user.application.CreateUserService;
import dev.sre.auction.user.application.UserQueryService;
import dev.sre.auction.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class UserController {

    private final CreateUserService createUser;
    private final UserQueryService query;

    public UserController(CreateUserService createUser, UserQueryService query) {
        this.createUser = createUser;
        this.query = query;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = createUser.execute(request.username());
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.id()))
                .body(UserResponse.from(user));
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(defaultValue = "100") int limit) {
        return query.list(limit).stream().map(UserResponse::from).toList();
    }
}
