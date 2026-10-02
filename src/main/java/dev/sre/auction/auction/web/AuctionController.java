package dev.sre.auction.auction.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.auction.application.AuctionQueryService;
import dev.sre.auction.auction.application.CreateAuctionCommand;
import dev.sre.auction.auction.application.CreateAuctionService;
import dev.sre.auction.auction.domain.Auction;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auctions")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class AuctionController {

    private final CreateAuctionService createAuction;
    private final AuctionQueryService query;

    public AuctionController(CreateAuctionService createAuction, AuctionQueryService query) {
        this.createAuction = createAuction;
        this.query = query;
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> create(
            @Valid @RequestBody CreateAuctionRequest request
    ) {
        Auction auction = createAuction.execute(new CreateAuctionCommand(
                request.lotId(),
                request.sellerId(),
                request.currency(),
                request.startPriceMinor(),
                request.minIncrementMinor(),
                request.startsAt(),
                request.endsAt()
        ));
        return ResponseEntity
                .created(URI.create("/api/v1/auctions/" + auction.id()))
                .body(AuctionResponse.from(auction));
    }

    @GetMapping("/{id}")
    public AuctionResponse get(@PathVariable UUID id) {
        return AuctionResponse.from(query.get(id));
    }

    @GetMapping
    public List<AuctionResponse> list(@RequestParam(defaultValue = "50") int limit) {
        return query.list(limit).stream().map(AuctionResponse::from).toList();
    }
}
