package pt.isel.gape.learning.model;

public enum ClassGroupShift {
    MORNING("morning", "Morning"),
    AFTERNOON("afternoon", "Afternoon"),
    EVENING("evening", "Evening"),
    MIXED("mixed", "Mixed");

    private final String databaseValue;
    private final String label;

    ClassGroupShift(String databaseValue, String label) {
        this.databaseValue = databaseValue;
        this.label = label;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public String getCode() {
        return databaseValue;
    }

    public String getLabel() {
        return label;
    }

    public static ClassGroupShift fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Unknown class group shift: " + value);
        }
        String normalized = value.trim();
        for (ClassGroupShift shift : values()) {
            if (shift.databaseValue.equals(normalized) || shift.name().equalsIgnoreCase(normalized)) {
                return shift;
            }
        }
        throw new IllegalArgumentException("Unknown class group shift: " + value);
    }
}
