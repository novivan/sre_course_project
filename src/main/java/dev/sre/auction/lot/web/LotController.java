package dev.sre.auction.lot.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.lot.application.CreateLotService;
import dev.sre.auction.lot.application.DeleteLotService;
import dev.sre.auction.lot.application.LotQueryService;
import dev.sre.auction.lot.application.UpdateLotService;
import dev.sre.auction.lot.domain.Lot;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class LotController {

    private final CreateLotService createLot;
    private final UpdateLotService updateLot;
    private final DeleteLotService deleteLot;
    private final LotQueryService query;

    public LotController(
            CreateLotService createLot,
            UpdateLotService updateLot,
            DeleteLotService deleteLot,
            LotQueryService query
    ) {
        this.createLot = createLot;
        this.updateLot = updateLot;
        this.deleteLot = deleteLot;
        this.query = query;
    }

    @PostMapping
    public ResponseEntity<LotResponse> create(@Valid @RequestBody CreateLotRequest request) {
        Lot lot = createLot.execute(
                request.sellerId(),
                request.title(),
                request.description(),
                request.imageUrl()
        );
        return ResponseEntity
                .created(URI.create("/api/v1/lots/" + lot.id()))
                .body(LotResponse.from(lot));
    }

    @GetMapping("/{id}")
    public LotResponse get(@PathVariable UUID id) {
        return LotResponse.from(query.get(id));
    }

    @GetMapping
    public List<LotResponse> list(@RequestParam(defaultValue = "100") int limit) {
        return query.list(limit).stream().map(LotResponse::from).toList();
    }

    @PutMapping("/{id}")
    public LotResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLotRequest request
    ) {
        return LotResponse.from(updateLot.execute(
                id,
                request.sellerId(),
                request.title(),
                request.description(),
                request.imageUrl()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestParam UUID sellerId
    ) {
        deleteLot.execute(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
