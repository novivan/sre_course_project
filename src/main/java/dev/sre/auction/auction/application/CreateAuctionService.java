package dev.sre.auction.auction.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.AuctionStatus;
import dev.sre.auction.lot.application.LotRepository;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.BusinessConflictException;
import dev.sre.auction.platform.error.EntityNotFoundException;
import dev.sre.auction.platform.error.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAuctionService {

    private final AuctionRepository auctions;
    private final LotRepository lots;
    private final Clock clock;

    public CreateAuctionService(AuctionRepository auctions, LotRepository lots, Clock clock) {
        this.auctions = auctions;
        this.lots = lots;
        this.clock = clock;
    }

    @Transactional
    public Auction execute(CreateAuctionCommand command) {
        Lot lot = lots.findByIdForUpdate(command.lotId())
                .orElseThrow(() -> new EntityNotFoundException("Lot", command.lotId()));

        if (!lot.sellerId().equals(command.sellerId())) {
            throw new BusinessConflictException(
                    "ONLY_SELLER_CAN_START_AUCTION",
                    "Only the lot seller can start an auction"
            );
        }
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new InvalidRequestException(
                    "INVALID_AUCTION_WINDOW",
                    "Auction end time must be after start time"
            );
        }
        if (command.startPriceMinor() < 0 || command.minIncrementMinor() <= 0) {
            throw new InvalidRequestException(
                    "INVALID_AUCTION_PRICE",
                    "Start price must be non-negative and minimum increment must be positive"
            );
        }
        if (auctions.existsOpenForLot(command.lotId())) {
            throw new BusinessConflictException(
                    "LOT_ALREADY_LISTED",
                    "Lot already has an open auction"
            );
        }

        Instant now = clock.instant();
        if (!command.endsAt().isAfter(now)) {
            throw new InvalidRequestException(
                    "AUCTION_ENDS_IN_PAST",
                    "Auction end time must be in the future"
            );
        }

        String currency = command.currency().trim().toUpperCase(Locale.ROOT);
        AuctionStatus status = command.startsAt().isAfter(now)
                ? AuctionStatus.SCHEDULED
                : AuctionStatus.ACTIVE;

        Auction auction = new Auction(
                UUID.randomUUID(),
                command.lotId(),
                status,
                currency,
                command.startPriceMinor(),
                command.startPriceMinor(),
                command.minIncrementMinor(),
                command.startsAt(),
                command.endsAt(),
                null,
                null,
                0,
                now,
                now
        );
        auctions.insert(auction);
        return auction;
    }
}
