package pt.isel.gape.common.time;

import java.time.Clock;
import java.time.ZoneId;

public final class ApplicationClock {

    public static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");

    private ApplicationClock() {
    }

    public static Clock system() {
        return Clock.system(ZONE);
    }
}
