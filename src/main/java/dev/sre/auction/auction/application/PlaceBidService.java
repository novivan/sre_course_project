package dev.sre.auction.auction.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.Bid;
import dev.sre.auction.lot.application.LotRepository;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.EntityNotFoundException;
import dev.sre.auction.user.application.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceBidService {

    private final AuctionRepository auctions;
    private final BidRepository bids;
    private final LotRepository lots;
    private final UserRepository users;
    private final Clock clock;

    public PlaceBidService(
            AuctionRepository auctions,
            BidRepository bids,
            LotRepository lots,
            UserRepository users,
            Clock clock
    ) {
        this.auctions = auctions;
        this.bids = bids;
        this.lots = lots;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public Bid execute(PlaceBidCommand command) {
        Auction auction = auctions.findByIdForUpdate(command.auctionId())
                .orElseThrow(() -> new EntityNotFoundException("Auction", command.auctionId()));

        var existing = bids.findByIdempotencyKey(
                command.auctionId(),
                command.bidderId(),
                command.idempotencyKey()
        );
        if (existing.isPresent()) {
            return existing.get();
        }

        users.findById(command.bidderId())
                .orElseThrow(() -> new EntityNotFoundException("User", command.bidderId()));
        Lot lot = lots.findById(auction.lotId())
                .orElseThrow(() -> new EntityNotFoundException("Lot", auction.lotId()));

        Instant now = clock.instant();
        auction.placeBid(command.bidderId(), lot.sellerId(), command.amountMinor(), now);

        Bid bid = new Bid(
                UUID.randomUUID(),
                auction.id(),
                command.bidderId(),
                command.amountMinor(),
                command.idempotencyKey(),
                now
        );
        bids.insert(bid);
        auctions.update(auction);
        return bid;
    }
}
