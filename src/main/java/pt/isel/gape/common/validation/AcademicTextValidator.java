package pt.isel.gape.common.validation;

public final class AcademicTextValidator {

    private static final char CONTEXT_SEPARATOR = '|';

    private AcademicTextValidator() {
    }

    public static void requireName(String value, String message) {
        requireText(value, message);
        rejectContextSeparator(value, "Name");
    }

    public static void requireAcronym(String value, String message) {
        requireText(value, message);
        rejectContextSeparator(value, "Acronym");
    }

    public static void rejectContextSeparator(String value, String fieldLabel) {
        if (value != null && value.indexOf(CONTEXT_SEPARATOR) >= 0) {
            throw new IllegalArgumentException(fieldLabel + " cannot contain '|'");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
