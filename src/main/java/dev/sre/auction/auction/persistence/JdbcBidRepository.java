package dev.sre.auction.auction.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.sre.auction.auction.application.BidRepository;
import dev.sre.auction.auction.domain.Bid;
import dev.sre.auction.platform.database.DatabaseTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBidRepository implements BidRepository {

    private final JdbcClient jdbc;

    public JdbcBidRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Bid bid) {
        jdbc.sql("""
                INSERT INTO bids (
                    id, auction_id, bidder_id, amount_minor, idempotency_key, created_at
                ) VALUES (
                    :id, :auctionId, :bidderId, :amountMinor, :idempotencyKey, :createdAt
                )
                """)
                .param("id", bid.id())
                .param("auctionId", bid.auctionId())
                .param("bidderId", bid.bidderId())
                .param("amountMinor", bid.amountMinor())
                .param("idempotencyKey", bid.idempotencyKey())
                .param("createdAt", DatabaseTime.fromInstant(bid.createdAt()))
                .update();
    }

    @Override
    public Optional<Bid> findByIdempotencyKey(
            UUID auctionId,
            UUID bidderId,
            UUID idempotencyKey
    ) {
        return jdbc.sql("""
                SELECT id, auction_id, bidder_id, amount_minor, idempotency_key, created_at
                FROM bids
                WHERE auction_id = :auctionId
                  AND bidder_id = :bidderId
                  AND idempotency_key = :idempotencyKey
                """)
                .param("auctionId", auctionId)
                .param("bidderId", bidderId)
                .param("idempotencyKey", idempotencyKey)
                .query(JdbcBidRepository::mapRow)
                .optional();
    }

    @Override
    public List<Bid> findByAuctionId(UUID auctionId, int limit) {
        return jdbc.sql("""
                SELECT id, auction_id, bidder_id, amount_minor, idempotency_key, created_at
                FROM bids
                WHERE auction_id = :auctionId
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
                """)
                .param("auctionId", auctionId)
                .param("limit", limit)
                .query(JdbcBidRepository::mapRow)
                .list();
    }

    private static Bid mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Bid(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("auction_id", UUID.class),
                resultSet.getObject("bidder_id", UUID.class),
                resultSet.getLong("amount_minor"),
                resultSet.getObject("idempotency_key", UUID.class),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
