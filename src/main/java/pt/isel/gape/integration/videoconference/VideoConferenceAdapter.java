package pt.isel.gape.integration.videoconference;

public interface VideoConferenceAdapter {

    VideoConferenceAccess validateAccess(String provider, String accessUrl);
}
