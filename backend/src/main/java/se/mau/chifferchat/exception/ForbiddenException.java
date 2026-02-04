package se.mau.chifferchat.exception;

/**
 * Exception thrown when a user is authenticated but not authorized
 * to perform a specific action (e.g., non-admin trying admin operation).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
