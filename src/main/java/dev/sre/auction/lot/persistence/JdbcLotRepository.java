package dev.sre.auction.lot.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.lot.application.LotRepository;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.database.DatabaseTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLotRepository implements LotRepository {

    private final JdbcClient jdbc;

    public JdbcLotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Lot lot) {
        jdbc.sql("""
                INSERT INTO lots (id, seller_id, title, description, image_url, created_at)
                VALUES (:id, :sellerId, :title, :description, :imageUrl, :createdAt)
                """)
                .param("id", lot.id())
                .param("sellerId", lot.sellerId())
                .param("title", lot.title())
                .param("description", lot.description(), Types.VARCHAR)
                .param("imageUrl", lot.imageUrl(), Types.VARCHAR)
                .param("createdAt", DatabaseTime.fromInstant(lot.createdAt()))
                .update();
    }

    @Override
    public Optional<Lot> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, seller_id, title, description, image_url, created_at
                FROM lots
                WHERE id = :id
                """)
                .param("id", id)
                .query(JdbcLotRepository::mapRow)
                .optional();
    }

    @Override
    public Optional<Lot> findByIdForUpdate(UUID id) {
        return jdbc.sql("""
                SELECT id, seller_id, title, description, image_url, created_at
                FROM lots
                WHERE id = :id
                FOR UPDATE
                """)
                .param("id", id)
                .query(JdbcLotRepository::mapRow)
                .optional();
    }

    @Override
    public List<Lot> findAll(int limit) {
        return jdbc.sql("""
                SELECT id, seller_id, title, description, image_url, created_at
                FROM lots
                ORDER BY created_at DESC
                LIMIT :limit
                """)
                .param("limit", limit)
                .query(JdbcLotRepository::mapRow)
                .list();
    }

    @Override
    public void update(Lot lot) {
        jdbc.sql("""
                UPDATE lots
                SET title = :title,
                    description = :description,
                    image_url = :imageUrl
                WHERE id = :id
                """)
                .param("title", lot.title())
                .param("description", lot.description(), Types.VARCHAR)
                .param("imageUrl", lot.imageUrl(), Types.VARCHAR)
                .param("id", lot.id())
                .update();
    }

    @Override
    public void delete(UUID id) {
        jdbc.sql("DELETE FROM lots WHERE id = :id")
                .param("id", id)
                .update();
    }

    private static Lot mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Lot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("seller_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("image_url"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
