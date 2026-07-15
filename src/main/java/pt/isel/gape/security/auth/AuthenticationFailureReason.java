package pt.isel.gape.security.auth;

public enum AuthenticationFailureReason {
    INVALID_CREDENTIALS,
    USER_INACTIVE,
    USER_BLOCKED,
    USER_WITHOUT_PROFILE,
    TOO_MANY_ATTEMPTS
}
