package dev.sre.auction.lot.application;

import java.util.UUID;

import dev.sre.auction.auction.application.AuctionRepository;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.BusinessConflictException;
import dev.sre.auction.platform.error.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateLotService {

    private final LotRepository lots;
    private final AuctionRepository auctions;

    public UpdateLotService(LotRepository lots, AuctionRepository auctions) {
        this.lots = lots;
        this.auctions = auctions;
    }

    @Transactional
    public Lot execute(
            UUID lotId,
            UUID sellerId,
            String title,
            String description,
            String imageUrl
    ) {
        Lot current = lots.findByIdForUpdate(lotId)
                .orElseThrow(() -> new EntityNotFoundException("Lot", lotId));
        ensureSeller(current, sellerId);

        if (auctions.existsAnyForLot(lotId)) {
            throw new BusinessConflictException(
                    "LOT_ALREADY_AUCTIONED",
                    "A lot cannot be edited after an auction has been created for it"
            );
        }

        Lot updated = new Lot(
                current.id(),
                current.sellerId(),
                title.trim(),
                normalize(description),
                normalize(imageUrl),
                current.createdAt()
        );
        lots.update(updated);
        return updated;
    }

    private static void ensureSeller(Lot lot, UUID sellerId) {
        if (!lot.sellerId().equals(sellerId)) {
            throw new BusinessConflictException(
                    "ONLY_SELLER_CAN_EDIT_LOT",
                    "Only the lot seller can edit the lot"
            );
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
