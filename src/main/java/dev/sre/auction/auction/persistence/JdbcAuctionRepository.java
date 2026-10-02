package dev.sre.auction.auction.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.sre.auction.auction.application.AuctionRepository;
import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.AuctionStatus;
import dev.sre.auction.platform.database.DatabaseTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuctionRepository implements AuctionRepository {

    private static final String SELECT_COLUMNS = """
            id, lot_id, status, currency, start_price_minor, current_price_minor,
            min_increment_minor, starts_at, ends_at, current_leader_id, winner_id,
            version, created_at, updated_at
            """;

    private final JdbcClient jdbc;

    public JdbcAuctionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Auction auction) {
        jdbc.sql("""
                INSERT INTO auctions (
                    id, lot_id, status, currency, start_price_minor, current_price_minor,
                    min_increment_minor, starts_at, ends_at, current_leader_id, winner_id,
                    version, created_at, updated_at
                ) VALUES (
                    :id, :lotId, :status, :currency, :startPriceMinor, :currentPriceMinor,
                    :minIncrementMinor, :startsAt, :endsAt, :currentLeaderId, :winnerId,
                    :version, :createdAt, :updatedAt
                )
                """)
                .param("id", auction.id())
                .param("lotId", auction.lotId())
                .param("status", auction.status().name())
                .param("currency", auction.currency())
                .param("startPriceMinor", auction.startPriceMinor())
                .param("currentPriceMinor", auction.currentPriceMinor())
                .param("minIncrementMinor", auction.minIncrementMinor())
                .param("startsAt", DatabaseTime.fromInstant(auction.startsAt()))
                .param("endsAt", DatabaseTime.fromInstant(auction.endsAt()))
                .param("currentLeaderId", auction.currentLeaderId(), Types.OTHER)
                .param("winnerId", auction.winnerId(), Types.OTHER)
                .param("version", auction.version())
                .param("createdAt", DatabaseTime.fromInstant(auction.createdAt()))
                .param("updatedAt", DatabaseTime.fromInstant(auction.updatedAt()))
                .update();
    }

    @Override
    public void update(Auction auction) {
        long previousVersion = auction.version() - 1;
        int updated = jdbc.sql("""
                UPDATE auctions
                SET status = :status,
                    current_price_minor = :currentPriceMinor,
                    current_leader_id = :currentLeaderId,
                    winner_id = :winnerId,
                    version = :version,
                    updated_at = :updatedAt
                WHERE id = :id
                  AND version = :previousVersion
                """)
                .param("status", auction.status().name())
                .param("currentPriceMinor", auction.currentPriceMinor())
                .param("currentLeaderId", auction.currentLeaderId(), Types.OTHER)
                .param("winnerId", auction.winnerId(), Types.OTHER)
                .param("version", auction.version())
                .param("updatedAt", DatabaseTime.fromInstant(auction.updatedAt()))
                .param("id", auction.id())
                .param("previousVersion", previousVersion)
                .update();

        if (updated != 1) {
            throw new IllegalStateException(
                    "Auction was concurrently updated: " + auction.id()
            );
        }
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return jdbc.sql("SELECT " + SELECT_COLUMNS + " FROM auctions WHERE id = :id")
                .param("id", id)
                .query(JdbcAuctionRepository::mapRow)
                .optional();
    }

    @Override
    public Optional<Auction> findByIdForUpdate(UUID id) {
        return jdbc.sql("SELECT " + SELECT_COLUMNS + " FROM auctions WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(JdbcAuctionRepository::mapRow)
                .optional();
    }

    @Override
    public List<Auction> findAll(int limit) {
        return jdbc.sql("""
                SELECT %s
                FROM auctions
                ORDER BY created_at DESC
                LIMIT :limit
                """.formatted(SELECT_COLUMNS))
                .param("limit", limit)
                .query(JdbcAuctionRepository::mapRow)
                .list();
    }

    @Override
    public List<Auction> findScheduledForUpdate(Instant now, int limit) {
        return jdbc.sql("""
                SELECT %s
                FROM auctions
                WHERE status = 'SCHEDULED'
                  AND starts_at <= :now
                ORDER BY starts_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """.formatted(SELECT_COLUMNS))
                .param("now", DatabaseTime.fromInstant(now))
                .param("limit", limit)
                .query(JdbcAuctionRepository::mapRow)
                .list();
    }

    @Override
    public List<Auction> findExpiredForUpdate(Instant now, int limit) {
        return jdbc.sql("""
                SELECT %s
                FROM auctions
                WHERE status = 'ACTIVE'
                  AND ends_at <= :now
                ORDER BY ends_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """.formatted(SELECT_COLUMNS))
                .param("now", DatabaseTime.fromInstant(now))
                .param("limit", limit)
                .query(JdbcAuctionRepository::mapRow)
                .list();
    }

    @Override
    public boolean existsOpenForLot(UUID lotId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM auctions
                    WHERE lot_id = :lotId
                      AND status IN ('SCHEDULED', 'ACTIVE')
                )
                """)
                .param("lotId", lotId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean existsAnyForLot(UUID lotId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM auctions
                    WHERE lot_id = :lotId
                )
                """)
                .param("lotId", lotId)
                .query(Boolean.class)
                .single();
    }

    private static Auction mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Auction(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("lot_id", UUID.class),
                AuctionStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("currency").trim(),
                resultSet.getLong("start_price_minor"),
                resultSet.getLong("current_price_minor"),
                resultSet.getLong("min_increment_minor"),
                resultSet.getObject("starts_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("ends_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("current_leader_id", UUID.class),
                resultSet.getObject("winner_id", UUID.class),
                resultSet.getLong("version"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
