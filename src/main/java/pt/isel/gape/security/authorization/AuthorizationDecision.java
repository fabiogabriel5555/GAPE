package pt.isel.gape.security.authorization;

public record AuthorizationDecision(boolean allowed, String reason) {

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(true, "allowed");
    }

    public static AuthorizationDecision deny(String reason) {
        return new AuthorizationDecision(false, reason);
    }
}
