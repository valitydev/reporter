package dev.vality.reporter.exception;

public class SignException extends RuntimeException {

    public SignException(String message) {
        super(message);
    }

    public SignException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignException(Throwable cause) {
        super(cause);
    }

    public SignException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
