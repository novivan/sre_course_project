package dev.sre.auction.platform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuctionMetrics {

    private final Counter usersCreated;
    private final Counter lotsCreated;
    private final Counter auctionsCreated;
    private final Counter bidsPlaced;
    private final Counter auctionsActivated;
    private final Counter auctionsFinished;

    public AuctionMetrics(MeterRegistry registry) {
        usersCreated = counter(registry, "auction.users.created", "Created users");
        lotsCreated = counter(registry, "auction.lots.created", "Created lots");
        auctionsCreated = counter(registry, "auction.auctions.created", "Created auctions");
        bidsPlaced = counter(registry, "auction.bids.placed", "New bids placed");
        auctionsActivated = counter(registry, "auction.auctions.activated", "Activated auctions");
        auctionsFinished = counter(registry, "auction.auctions.finished", "Finished auctions");
    }

    public void userCreated() {
        usersCreated.increment();
    }

    public void lotCreated() {
        lotsCreated.increment();
    }

    public void auctionCreated() {
        auctionsCreated.increment();
    }

    public void bidPlaced() {
        bidsPlaced.increment();
    }

    public void auctionsActivated(int count) {
        increment(auctionsActivated, count);
    }

    public void auctionsFinished(int count) {
        increment(auctionsFinished, count);
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name)
                .description(description)
                .register(registry);
    }

    private static void increment(Counter counter, int count) {
        if (count > 0) {
            counter.increment(count);
        }
    }
}
