package pt.isel.gape.access.model;

import java.time.LocalDateTime;

public record Session(
        long id,
        long userId,
        String token,
        SessionState state,
        LocalDateTime startAt,
        LocalDateTime lastActivity,
        LocalDateTime endAt
) {
    public boolean isActive() {
        return state == SessionState.ACTIVE;
    }
}
