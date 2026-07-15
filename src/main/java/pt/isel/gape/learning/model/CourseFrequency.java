package pt.isel.gape.learning.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum CourseFrequency {
    MONTHLY("monthly", "Monthly", 12, 1),
    BIMONTHLY("bimonthly", "Bimonthly", 6, 2),
    TRIMESTER("trimester", "Trimester", 4, 3),
    QUADRIMESTER("quadrimester", "Quadrimester", 3, 4),
    SEMESTER("semester", "Semester", 2, 6),
    ANNUAL("annual", "Annual", 1, 12);

    private final String databaseValue;
    private final String label;
    private final int periodsPerYear;
    private final int monthsPerPeriod;

    CourseFrequency(String databaseValue, String label, int periodsPerYear, int monthsPerPeriod) {
        this.databaseValue = databaseValue;
        this.label = label;
        this.periodsPerYear = periodsPerYear;
        this.monthsPerPeriod = monthsPerPeriod;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public String label() {
        return label;
    }

    public int periodsPerYear() {
        return periodsPerYear;
    }

    public int monthsPerPeriod() {
        return monthsPerPeriod;
    }

    public List<CurricularTerm> terms() {
        return Arrays.stream(CurricularTerm.values())
                .filter(term -> term.belongsTo(this))
                .sorted(java.util.Comparator.comparingInt(CurricularTerm::position))
                .toList();
    }

    public static CourseFrequency fromDatabaseValue(String value) {
        for (CourseFrequency frequency : values()) {
            if (frequency.databaseValue.equalsIgnoreCase(value)) {
                return frequency;
            }
        }
        throw new IllegalArgumentException("Unsupported course frequency: " + value);
    }

    public static CourseFrequency parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Course frequency is required");
        }
        String normalized = value.trim();
        for (CourseFrequency frequency : values()) {
            if (frequency.name().equalsIgnoreCase(normalized)
                    || frequency.databaseValue.equalsIgnoreCase(normalized)) {
                return frequency;
            }
        }
        return fromDatabaseValue(normalized.toLowerCase(Locale.ROOT));
    }
}
