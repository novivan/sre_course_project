package dev.sre.auction.lot.application;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import dev.sre.auction.lot.domain.Lot;

public interface LotRepository {

    void insert(Lot lot);

    Optional<Lot> findById(UUID id);

    Optional<Lot> findByIdForUpdate(UUID id);

    List<Lot> findAll(int limit);

    void update(Lot lot);

    void delete(UUID id);
}
