package dev.sre.auction.auction.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.platform.metrics.AuctionMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionLifecycleService {

    private final AuctionRepository auctions;
    private final Clock clock;
    private final AuctionMetrics metrics;

    public AuctionLifecycleService(
            AuctionRepository auctions,
            Clock clock,
            AuctionMetrics metrics
    ) {
        this.auctions = auctions;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public LifecycleResult advance(int batchSize) {
        Instant now = clock.instant();
        int activated = updateScheduled(now, batchSize);
        int finished = updateExpired(now, batchSize);
        metrics.auctionsActivated(activated);
        metrics.auctionsFinished(finished);
        return new LifecycleResult(activated, finished);
    }

    private int updateScheduled(Instant now, int batchSize) {
        List<Auction> scheduled = auctions.findScheduledForUpdate(now, batchSize);
        int activated = 0;
        for (Auction auction : scheduled) {
            if (auction.activate(now)) {
                auctions.update(auction);
                activated++;
            }
        }
        return activated;
    }

    private int updateExpired(Instant now, int batchSize) {
        List<Auction> expired = auctions.findExpiredForUpdate(now, batchSize);
        int finished = 0;
        for (Auction auction : expired) {
            if (auction.finish(now)) {
                auctions.update(auction);
                finished++;
            }
        }
        return finished;
    }

    public record LifecycleResult(int activated, int finished) {
    }
}
