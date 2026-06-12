package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CurricularTerm {
    ANNUAL("annual"),
    SEMESTER_1("semester_1"),
    SEMESTER_2("semester_2"),
    TRIMESTER_1("trimester_1"),
    TRIMESTER_2("trimester_2"),
    TRIMESTER_3("trimester_3");

    private final String databaseValue;

    CurricularTerm(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CurricularTerm fromDatabaseValue(String value) {
        for (CurricularTerm term : values()) {
            if (term.databaseValue.equalsIgnoreCase(value)) {
                return term;
            }
        }
        throw new IllegalArgumentException("Unsupported curricular term: " + value);
    }

    public static CurricularTerm parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
