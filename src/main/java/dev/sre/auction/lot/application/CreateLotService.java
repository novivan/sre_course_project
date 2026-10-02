package dev.sre.auction.lot.application;

import java.time.Clock;
import java.util.UUID;

import dev.sre.auction.lot.domain.Lot;
import dev.sre.auction.platform.error.EntityNotFoundException;
import dev.sre.auction.user.application.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateLotService {

    private final LotRepository lots;
    private final UserRepository users;
    private final Clock clock;

    public CreateLotService(LotRepository lots, UserRepository users, Clock clock) {
        this.lots = lots;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public Lot execute(UUID sellerId, String title, String description, String imageUrl) {
        users.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("User", sellerId));

        Lot lot = new Lot(
                UUID.randomUUID(),
                sellerId,
                title.trim(),
                normalize(description),
                normalize(imageUrl),
                clock.instant()
        );
        lots.insert(lot);
        return lot;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
