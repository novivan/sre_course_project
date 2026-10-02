package dev.sre.auction.auction.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import dev.sre.auction.platform.error.BusinessConflictException;

public final class Auction {

    private final UUID id;
    private final UUID lotId;
    private AuctionStatus status;
    private final String currency;
    private final long startPriceMinor;
    private long currentPriceMinor;
    private final long minIncrementMinor;
    private final Instant startsAt;
    private final Instant endsAt;
    private UUID currentLeaderId;
    private UUID winnerId;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Auction(
            UUID id,
            UUID lotId,
            AuctionStatus status,
            String currency,
            long startPriceMinor,
            long currentPriceMinor,
            long minIncrementMinor,
            Instant startsAt,
            Instant endsAt,
            UUID currentLeaderId,
            UUID winnerId,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.lotId = Objects.requireNonNull(lotId);
        this.status = Objects.requireNonNull(status);
        this.currency = Objects.requireNonNull(currency);
        this.startPriceMinor = startPriceMinor;
        this.currentPriceMinor = currentPriceMinor;
        this.minIncrementMinor = minIncrementMinor;
        this.startsAt = Objects.requireNonNull(startsAt);
        this.endsAt = Objects.requireNonNull(endsAt);
        this.currentLeaderId = currentLeaderId;
        this.winnerId = winnerId;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void placeBid(UUID bidderId, UUID sellerId, long amountMinor, Instant now) {
        if (status != AuctionStatus.ACTIVE) {
            throw new BusinessConflictException(
                    "AUCTION_NOT_ACTIVE",
                    "Auction is not active"
            );
        }
        if (!now.isBefore(endsAt)) {
            throw new BusinessConflictException(
                    "AUCTION_ALREADY_ENDED",
                    "Auction has already ended"
            );
        }
        if (bidderId.equals(sellerId)) {
            throw new BusinessConflictException(
                    "SELLER_CANNOT_BID",
                    "Seller cannot bid on their own lot"
            );
        }

        long minimumAllowed;
        try {
            minimumAllowed = Math.addExact(currentPriceMinor, minIncrementMinor);
        } catch (ArithmeticException exception) {
            throw new BusinessConflictException(
                    "PRICE_LIMIT_REACHED",
                    "Auction price limit has been reached"
            );
        }

        if (amountMinor < minimumAllowed) {
            throw new BusinessConflictException(
                    "BID_TOO_LOW",
                    "Bid must be at least " + minimumAllowed + " " + currency + " minor units"
            );
        }

        currentPriceMinor = amountMinor;
        currentLeaderId = bidderId;
        version++;
        updatedAt = now;
    }

    public boolean activate(Instant now) {
        if (status != AuctionStatus.SCHEDULED || now.isBefore(startsAt)) {
            return false;
        }
        status = AuctionStatus.ACTIVE;
        version++;
        updatedAt = now;
        return true;
    }

    public boolean finish(Instant now) {
        if (status != AuctionStatus.ACTIVE || now.isBefore(endsAt)) {
            return false;
        }
        status = AuctionStatus.FINISHED;
        winnerId = currentLeaderId;
        version++;
        updatedAt = now;
        return true;
    }

    public UUID id() {
        return id;
    }

    public UUID lotId() {
        return lotId;
    }

    public AuctionStatus status() {
        return status;
    }

    public String currency() {
        return currency;
    }

    public long startPriceMinor() {
        return startPriceMinor;
    }

    public long currentPriceMinor() {
        return currentPriceMinor;
    }

    public long minIncrementMinor() {
        return minIncrementMinor;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public UUID currentLeaderId() {
        return currentLeaderId;
    }

    public UUID winnerId() {
        return winnerId;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
