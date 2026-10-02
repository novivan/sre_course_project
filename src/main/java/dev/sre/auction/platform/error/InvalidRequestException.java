package dev.sre.auction.platform.error;

public final class InvalidRequestException extends ApplicationException {

    public InvalidRequestException(String code, String message) {
        super(code, message);
    }
}
