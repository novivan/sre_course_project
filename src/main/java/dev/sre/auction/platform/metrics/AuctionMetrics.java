package dev.sre.auction.platform.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import dev.sre.auction.platform.error.ApplicationException;
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
    private final Map<BusinessOperation, Map<OperationResult, Counter>> businessOperations;

    public AuctionMetrics(MeterRegistry registry) {
        usersCreated = counter(registry, "auction.users.created", "Created users");
        lotsCreated = counter(registry, "auction.lots.created", "Created lots");
        auctionsCreated = counter(registry, "auction.auctions.created", "Created auctions");
        bidsPlaced = counter(registry, "auction.bids.placed", "New bids placed");
        auctionsActivated = counter(registry, "auction.auctions.activated", "Activated auctions");
        auctionsFinished = counter(registry, "auction.auctions.finished", "Finished auctions");
        businessOperations = createBusinessOperationCounters(registry);
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

    public <T> T recordBusinessOperation(BusinessOperation operation, Supplier<T> action) {
        try {
            T result = action.get();
            businessCounter(operation, OperationResult.SUCCESS).increment();
            return result;
        } catch (ApplicationException exception) {
            businessCounter(operation, OperationResult.REJECTED).increment();
            throw exception;
        } catch (RuntimeException exception) {
            businessCounter(operation, OperationResult.FAILURE).increment();
            throw exception;
        }
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

    private static Map<BusinessOperation, Map<OperationResult, Counter>>
            createBusinessOperationCounters(MeterRegistry registry) {
        Map<BusinessOperation, Map<OperationResult, Counter>> counters =
                new EnumMap<>(BusinessOperation.class);
        for (BusinessOperation operation : BusinessOperation.values()) {
            Map<OperationResult, Counter> results = new EnumMap<>(OperationResult.class);
            for (OperationResult result : OperationResult.values()) {
                Counter counter = Counter.builder("auction.business.operations")
                        .description("Results of customer-facing auction operations")
                        .tag("operation", operation.tagValue())
                        .tag("result", result.tagValue())
                        .register(registry);
                results.put(result, counter);
            }
            counters.put(operation, results);
        }
        return counters;
    }

    private Counter businessCounter(BusinessOperation operation, OperationResult result) {
        return businessOperations.get(operation).get(result);
    }

    public enum BusinessOperation {
        CREATE_LOT("create_lot"),
        CREATE_AUCTION("create_auction"),
        PLACE_BID("place_bid");

        private final String tagValue;

        BusinessOperation(String tagValue) {
            this.tagValue = tagValue;
        }

        public String tagValue() {
            return tagValue;
        }
    }

    private enum OperationResult {
        SUCCESS("success"),
        REJECTED("rejected"),
        FAILURE("failure");

        private final String tagValue;

        OperationResult(String tagValue) {
            this.tagValue = tagValue;
        }

        String tagValue() {
            return tagValue;
        }
    }
}
