package pt.isel.gape.integration.videoconference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StoredVideoConferenceAdapterTest {

    private final StoredVideoConferenceAdapter adapter = new StoredVideoConferenceAdapter();

    @Test
    void validatesKnownProviderAndExpectedHost() {
        VideoConferenceAccess access = adapter.validateAccess("google meet", " https://meet.google.com/abc-defg-hij ");

        assertEquals("Meet", access.provider());
        assertEquals("https://meet.google.com/abc-defg-hij", access.accessUrl());
    }

    @Test
    void rejectsInvalidProviderAndUnsafeHosts() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.validateAccess("Webex", "https://meet.google.com/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.validateAccess("Meet", "https://user:pass@meet.google.com/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.validateAccess("Meet", "https://10.0.0.10/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.validateAccess("Meet", "https://[::1]/abc-defg-hij"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.validateAccess("Zoom", "https://meet.google.com/abc-defg-hij"));
    }
}
