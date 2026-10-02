package dev.sre.auction.lot.application;

import java.util.UUID;

import dev.sre.auction.auction.application.AuctionRepository;
import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.BusinessConflictException;
import dev.sre.auction.platform.error.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteLotService {

    private final LotRepository lots;
    private final AuctionRepository auctions;

    public DeleteLotService(LotRepository lots, AuctionRepository auctions) {
        this.lots = lots;
        this.auctions = auctions;
    }

    @Transactional
    public void execute(UUID lotId, UUID sellerId) {
        Lot lot = lots.findByIdForUpdate(lotId)
                .orElseThrow(() -> new EntityNotFoundException("Lot", lotId));

        if (!lot.sellerId().equals(sellerId)) {
            throw new BusinessConflictException(
                    "ONLY_SELLER_CAN_DELETE_LOT",
                    "Only the lot seller can delete the lot"
            );
        }
        if (auctions.existsAnyForLot(lotId)) {
            throw new BusinessConflictException(
                    "LOT_ALREADY_AUCTIONED",
                    "A lot cannot be deleted after an auction has been created for it"
            );
        }

        lots.delete(lotId);
    }
}
