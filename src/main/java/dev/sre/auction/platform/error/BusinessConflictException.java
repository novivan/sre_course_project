package dev.sre.auction.platform.error;

public final class BusinessConflictException extends ApplicationException {

    public BusinessConflictException(String code, String message) {
        super(code, message);
    }
}
