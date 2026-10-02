package dev.sre.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import dev.sre.auction.auction.application.AuctionQueryService;
import dev.sre.auction.auction.application.CreateAuctionCommand;
import dev.sre.auction.auction.application.CreateAuctionService;
import dev.sre.auction.auction.application.PlaceBidCommand;
import dev.sre.auction.auction.application.PlaceBidService;
import dev.sre.auction.auction.domain.Auction;
import dev.sre.auction.auction.domain.Bid;
import dev.sre.auction.lot.application.CreateLotService;
import dev.sre.auction.lot.application.DeleteLotService;
import dev.sre.auction.lot.application.LotQueryService;
import dev.sre.auction.lot.application.UpdateLotService;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.BusinessConflictException;
import dev.sre.auction.platform.error.EntityNotFoundException;
import dev.sre.auction.user.application.CreateUserService;
import dev.sre.auction.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "app.role=api")
@Testcontainers(disabledWithoutDocker = true)
class AuctionFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    CreateUserService createUser;

    @Autowired
    CreateLotService createLot;

    @Autowired
    UpdateLotService updateLot;

    @Autowired
    DeleteLotService deleteLot;

    @Autowired
    LotQueryService lotQuery;

    @Autowired
    CreateAuctionService createAuction;

    @Autowired
    PlaceBidService placeBid;

    @Autowired
    AuctionQueryService query;

    @Test
    void createsAuctionAndKeepsBidIdempotent() {
        User seller = createUser.execute("seller-" + UUID.randomUUID());
        User bidder = createUser.execute("bidder-" + UUID.randomUUID());
        Lot lot = createLot.execute(seller.id(), "Rare card", "First edition", null);

        Instant now = Instant.now();
        Auction auction = createAuction.execute(new CreateAuctionCommand(
                lot.id(),
                seller.id(),
                "RUB",
                1_000,
                100,
                now.minusSeconds(5),
                now.plusSeconds(3_600)
        ));

        UUID idempotencyKey = UUID.randomUUID();
        PlaceBidCommand command = new PlaceBidCommand(
                auction.id(), bidder.id(), 1_100, idempotencyKey
        );
        Bid first = placeBid.execute(command);
        Bid retried = placeBid.execute(command);

        assertThat(retried.id()).isEqualTo(first.id());
        assertThat(query.get(auction.id()).currentPriceMinor()).isEqualTo(1_100);
        assertThat(query.bidHistory(auction.id(), 10)).hasSize(1);
    }

    @Test
    void supportsLotCrudBeforeAuctionStarts() {
        User seller = createUser.execute("seller-" + UUID.randomUUID());
        Lot created = createLot.execute(seller.id(), "Old title", null, null);

        Lot updated = updateLot.execute(
                created.id(),
                seller.id(),
                "New title",
                "Updated description",
                "https://example.test/item.jpg"
        );

        assertThat(lotQuery.get(created.id())).isEqualTo(updated);
        assertThat(lotQuery.list(100)).extracting(Lot::id).contains(created.id());

        deleteLot.execute(created.id(), seller.id());

        assertThatThrownBy(() -> lotQuery.get(created.id()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void preventsChangingLotAfterAuctionWasCreated() {
        User seller = createUser.execute("seller-" + UUID.randomUUID());
        Lot lot = createLot.execute(seller.id(), "Immutable lot", null, null);
        Instant now = Instant.now();

        createAuction.execute(new CreateAuctionCommand(
                lot.id(),
                seller.id(),
                "RUB",
                1_000,
                100,
                now.minusSeconds(5),
                now.plusSeconds(3_600)
        ));

        assertThatThrownBy(() -> updateLot.execute(
                lot.id(), seller.id(), "Changed", null, null
        ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("cannot be edited");

        assertThatThrownBy(() -> deleteLot.execute(lot.id(), seller.id()))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("cannot be deleted");
    }
}
