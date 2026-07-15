package pt.isel.gape.common.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ApplicationDateTimeFormatTest {

    @Test
    void usesLisbonAndTheApplicationPresentationConvention() {
        LocalDate date = LocalDate.of(2026, 2, 9);
        LocalTime time = LocalTime.of(3, 4, 5);
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        assertEquals(ZoneId.of("Europe/Lisbon"), ApplicationClock.ZONE);
        assertEquals("09-02-2026", ApplicationDateTimeFormat.date(date));
        assertEquals("03-04-05", ApplicationDateTimeFormat.time(time));
        assertEquals("09-02-2026 03-04-05", ApplicationDateTimeFormat.dateTime(dateTime));
        assertEquals(
                "09-07-2026 13-04-05",
                ApplicationDateTimeFormat.dateTime(Instant.parse("2026-07-09T12:04:05Z"))
        );
    }

    @Test
    void acceptsDisplayedAndTechnicalRequestValues() {
        assertEquals(
                LocalDate.of(2026, 2, 9),
                ApplicationDateTimeFormat.parseUserDate("09-02-2026")
        );
        assertEquals(
                LocalDate.of(2026, 2, 9),
                ApplicationDateTimeFormat.parseUserDate("2026-02-09")
        );
        assertEquals(
                LocalDateTime.of(2026, 2, 9, 3, 4, 5),
                ApplicationDateTimeFormat.parseUserDateTime("09-02-2026 03-04-05")
        );
        assertEquals(
                LocalDateTime.of(2026, 2, 9, 3, 4),
                ApplicationDateTimeFormat.parseUserDateTime("2026-02-09T03:04")
        );
    }
}
