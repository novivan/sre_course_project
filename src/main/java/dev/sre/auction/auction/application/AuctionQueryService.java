package dev.sre.auction.auction.application;

import java.util.List;
import java.util.UUID;

import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.Bid;
import dev.sre.auction.platform.error.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionQueryService {

    private final AuctionRepository auctions;
    private final BidRepository bids;

    public AuctionQueryService(AuctionRepository auctions, BidRepository bids) {
        this.auctions = auctions;
        this.bids = bids;
    }

    @Transactional(readOnly = true)
    public Auction get(UUID id) {
        return auctions.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Auction", id));
    }

    @Transactional(readOnly = true)
    public List<Auction> list(int limit) {
        return auctions.findAll(Math.min(Math.max(limit, 1), 100));
    }

    @Transactional(readOnly = true)
    public List<Bid> bidHistory(UUID auctionId, int limit) {
        get(auctionId);
        return bids.findByAuctionId(auctionId, Math.min(Math.max(limit, 1), 100));
    }
}
