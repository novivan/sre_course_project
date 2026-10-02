package dev.sre.auction.platform.error;

public final class EntityNotFoundException extends ApplicationException {

    public EntityNotFoundException(String entity, Object id) {
        super("ENTITY_NOT_FOUND", entity + " not found: " + id);
    }
}
