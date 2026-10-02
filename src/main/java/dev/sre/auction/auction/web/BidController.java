package dev.sre.auction.auction.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.auction.application.AuctionQueryService;
import dev.sre.auction.auction.application.PlaceBidCommand;
import dev.sre.auction.auction.application.PlaceBidService;
import dev.sre.auction.auction.domain.Bid;
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
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class BidController {

    private final PlaceBidService placeBid;
    private final AuctionQueryService query;

    public BidController(PlaceBidService placeBid, AuctionQueryService query) {
        this.placeBid = placeBid;
        this.query = query;
    }

    @PostMapping
    public ResponseEntity<BidResponse> place(
            @PathVariable UUID auctionId,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        Bid bid = placeBid.execute(new PlaceBidCommand(
                auctionId,
                request.bidderId(),
                request.amountMinor(),
                request.idempotencyKey()
        ));
        return ResponseEntity
                .created(URI.create("/api/v1/auctions/" + auctionId + "/bids/" + bid.id()))
                .body(BidResponse.from(bid));
    }

    @GetMapping
    public List<BidResponse> history(
            @PathVariable UUID auctionId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return query.bidHistory(auctionId, limit).stream().map(BidResponse::from).toList();
    }
}
