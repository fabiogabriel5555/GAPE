package pt.isel.gape.access.model;

public enum DeletionRequestState {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    COMPLETED;

    public static DeletionRequestState fromDatabaseValue(String value) {
        return DeletionRequestState.valueOf(value.toUpperCase().replace('-', '_'));
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == COMPLETED;
    }
}
