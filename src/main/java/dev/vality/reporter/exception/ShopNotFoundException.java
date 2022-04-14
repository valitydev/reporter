package dev.vality.reporter.exception;

public class ShopNotFoundException extends RuntimeException {

    public ShopNotFoundException(String message) {
        super(message);
    }

    public ShopNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShopNotFoundException(Throwable cause) {
        super(cause);
    }

    public ShopNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
