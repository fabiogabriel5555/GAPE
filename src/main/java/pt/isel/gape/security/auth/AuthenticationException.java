package pt.isel.gape.security.auth;

public final class AuthenticationException extends RuntimeException {

    private final AuthenticationFailureReason reason;

    public AuthenticationException(AuthenticationFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AuthenticationFailureReason reason() {
        return reason;
    }
}
