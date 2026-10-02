package dev.sre.auction.auction.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.sre.auction.auction.domain.Auction;

public interface AuctionRepository {

    void insert(Auction auction);

    void update(Auction auction);

    Optional<Auction> findById(UUID id);

    Optional<Auction> findByIdForUpdate(UUID id);

    List<Auction> findAll(int limit);

    List<Auction> findScheduledForUpdate(Instant now, int limit);

    List<Auction> findExpiredForUpdate(Instant now, int limit);

    boolean existsOpenForLot(UUID lotId);

    boolean existsAnyForLot(UUID lotId);
}
