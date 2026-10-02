package dev.sre.auction.lot.application;

import java.util.List;
import java.util.UUID;

import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotQueryService {

    private final LotRepository lots;

    public LotQueryService(LotRepository lots) {
        this.lots = lots;
    }

    @Transactional(readOnly = true)
    public Lot get(UUID id) {
        return lots.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lot", id));
    }

    @Transactional(readOnly = true)
    public List<Lot> list(int limit) {
        return lots.findAll(Math.min(Math.max(limit, 1), 100));
    }
}
