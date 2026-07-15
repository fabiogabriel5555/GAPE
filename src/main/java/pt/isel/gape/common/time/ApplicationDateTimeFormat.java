package pt.isel.gape.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Presentation and request-value conventions used by the application.
 */
public final class ApplicationDateTimeFormat {

    public static final Locale LOCALE = Locale.forLanguageTag("pt-PT");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy", LOCALE);
    public static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH-mm-ss", LOCALE);
    public static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss", LOCALE);

    /** HTML date and datetime-local values remain ISO by contract. */
    public static final DateTimeFormatter TECHNICAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TECHNICAL_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private ApplicationDateTimeFormat() {
    }

    public static String date(LocalDate value) {
        return DISPLAY_DATE.format(value);
    }

    public static String time(LocalTime value) {
        return DISPLAY_TIME.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return DISPLAY_DATE_TIME.format(value);
    }

    public static String dateTime(Instant value) {
        return DISPLAY_DATE_TIME.format(value.atZone(ApplicationClock.ZONE));
    }

    public static LocalDate parseUserDate(String value) {
        return parse(value, DISPLAY_DATE, TECHNICAL_DATE, LocalDate::parse);
    }

    public static LocalDateTime parseUserDateTime(String value) {
        return parse(value, DISPLAY_DATE_TIME, DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::parse);
    }

    private static <T> T parse(
            String value,
            DateTimeFormatter displayFormatter,
            DateTimeFormatter technicalFormatter,
            Parser<T> parser
    ) {
        try {
            return parser.parse(value, displayFormatter);
        } catch (DateTimeParseException ignored) {
            return parser.parse(value, technicalFormatter);
        }
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(String value, DateTimeFormatter formatter);
    }
}
