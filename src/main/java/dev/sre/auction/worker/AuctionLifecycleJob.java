package dev.sre.auction.worker;

import dev.sre.auction.auction.application.AuctionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
public class AuctionLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(AuctionLifecycleJob.class);

    private final AuctionLifecycleService lifecycle;
    private final int batchSize;

    public AuctionLifecycleJob(
            AuctionLifecycleService lifecycle,
            @Value("${app.worker.batch-size:100}") int batchSize
    ) {
        this.lifecycle = lifecycle;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.worker.poll-delay:1s}")
    public void run() {
        AuctionLifecycleService.LifecycleResult result = lifecycle.advance(batchSize);
        if (result.activated() > 0 || result.finished() > 0) {
            log.info(
                    "Auction lifecycle advanced: activated={}, finished={}",
                    result.activated(),
                    result.finished()
            );
        }
    }
}
