package ar.com.rotula.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
