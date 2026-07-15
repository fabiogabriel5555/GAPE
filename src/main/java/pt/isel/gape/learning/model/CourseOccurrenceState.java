package pt.isel.gape.learning.model;

import java.time.LocalDate;
import java.util.Locale;

public enum CourseOccurrenceState {
    DRAFT("draft"),
    SCHEDULED("scheduled"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String databaseValue;

    CourseOccurrenceState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CourseOccurrenceState forDates(LocalDate startsAt, LocalDate endsAt, LocalDate today) {
        if (startsAt == null || endsAt == null || today == null) {
            throw new IllegalArgumentException("Occurrence dates and current date are required");
        }
        if (endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Occurrence end date cannot be before the start date");
        }
        if (today.isBefore(startsAt)) {
            return SCHEDULED;
        }
        if (today.isAfter(endsAt)) {
            return COMPLETED;
        }
        return ACTIVE;
    }

    public static CourseOccurrenceState fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Course occurrence state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CourseOccurrenceState state : values()) {
            if (state.databaseValue.equals(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported course occurrence state: " + value);
    }
}
