package dev.sre.auction.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.platform.error.BusinessConflictException;
import org.junit.jupiter.api.Test;

class AuctionTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Test
    void acceptsBidAndChangesLeader() {
        UUID bidder = UUID.randomUUID();
        Auction auction = activeAuction();

        auction.placeBid(bidder, UUID.randomUUID(), 1_100, NOW);

        assertThat(auction.currentPriceMinor()).isEqualTo(1_100);
        assertThat(auction.currentLeaderId()).isEqualTo(bidder);
        assertThat(auction.version()).isEqualTo(1);
    }

    @Test
    void rejectsBidBelowMinimumIncrement() {
        Auction auction = activeAuction();

        assertThatThrownBy(() -> auction.placeBid(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_099,
                NOW
        ))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code")
                .isEqualTo("BID_TOO_LOW");
    }

    @Test
    void finishesWithCurrentLeaderAsWinner() {
        UUID bidder = UUID.randomUUID();
        Auction auction = activeAuction();
        auction.placeBid(bidder, UUID.randomUUID(), 1_100, NOW);

        boolean changed = auction.finish(NOW.plusSeconds(3_601));

        assertThat(changed).isTrue();
        assertThat(auction.status()).isEqualTo(AuctionStatus.FINISHED);
        assertThat(auction.winnerId()).isEqualTo(bidder);
    }

    private static Auction activeAuction() {
        return new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuctionStatus.ACTIVE,
                "RUB",
                1_000,
                1_000,
                100,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3_600),
                null,
                null,
                0,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60)
        );
    }
}
