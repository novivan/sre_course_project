package dev.sre.auction.auction.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.sre.auction.auction.domain.Bid;

public interface BidRepository {

    void insert(Bid bid);

    Optional<Bid> findByIdempotencyKey(UUID auctionId, UUID bidderId, UUID idempotencyKey);

    List<Bid> findByAuctionId(UUID auctionId, int limit);
}
