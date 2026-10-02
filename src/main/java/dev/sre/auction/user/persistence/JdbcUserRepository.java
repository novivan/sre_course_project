package dev.sre.auction.user.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.platform.database.DatabaseTime;
import dev.sre.auction.user.application.UserRepository;
import dev.sre.auction.user.domain.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcClient jdbc;

    public JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(User user) {
        jdbc.sql("""
                INSERT INTO users (id, username, created_at)
                VALUES (:id, :username, :createdAt)
                """)
                .param("id", user.id())
                .param("username", user.username())
                .param("createdAt", DatabaseTime.fromInstant(user.createdAt()))
                .update();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, username, created_at
                FROM users
                WHERE id = :id
                """)
                .param("id", id)
                .query(JdbcUserRepository::mapRow)
                .optional();
    }

    @Override
    public List<User> findAll(int limit) {
        return jdbc.sql("""
                SELECT id, username, created_at
                FROM users
                ORDER BY created_at DESC
                LIMIT :limit
                """)
                .param("limit", limit)
                .query(JdbcUserRepository::mapRow)
                .list();
    }

    private static User mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new User(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("username"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
