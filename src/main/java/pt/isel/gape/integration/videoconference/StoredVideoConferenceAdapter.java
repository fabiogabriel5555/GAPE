package pt.isel.gape.integration.videoconference;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class StoredVideoConferenceAdapter implements VideoConferenceAdapter {

    private static final int PROVIDER_MAX_LENGTH = 40;
    private static final int URL_MAX_LENGTH = 255;
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");

    @Override
    public VideoConferenceAccess validateAccess(String provider, String accessUrl) {
        String normalizedUrl = normalizeUrl(accessUrl);
        String normalizedProvider = normalizeProvider(provider);
        requireProviderMatchesUrl(normalizedProvider, normalizedUrl);
        return new VideoConferenceAccess(normalizedProvider, normalizedUrl);
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Video conference provider is required");
        }
        String normalized = provider.trim();
        rejectControlCharacters(normalized, "Video conference provider");
        if (normalized.length() > PROVIDER_MAX_LENGTH) {
            throw new IllegalArgumentException("Video conference provider is too long");
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "zoom" -> "Zoom";
            case "teams", "microsoft teams" -> "Teams";
            case "meet", "google meet" -> "Meet";
            default -> throw new IllegalArgumentException("Unsupported video conference provider");
        };
    }

    private static String normalizeUrl(String accessUrl) {
        if (accessUrl == null || accessUrl.isBlank()) {
            throw new IllegalArgumentException("Video conference access URL is required");
        }
        String normalized = accessUrl.trim();
        rejectControlCharacters(normalized, "Video conference access URL");
        if (normalized.length() > URL_MAX_LENGTH) {
            throw new IllegalArgumentException("Video conference access URL is too long");
        }

        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Video conference access URL is invalid", exception);
        }
        if (!uri.isAbsolute() || uri.getScheme() == null || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Video conference access URL must be absolute");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Video conference access URL must use HTTPS");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Video conference access URL cannot contain user info");
        }
        rejectInternalHost(uri.getHost());
        return normalized;
    }

    private static void requireProviderMatchesUrl(String provider, String accessUrl) {
        URI uri = URI.create(accessUrl);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        switch (provider) {
            case "Zoom" -> requireHost(host, "zoom.us", "Zoom meeting links must use a Zoom domain");
            case "Teams" -> requireHost(host, "teams.microsoft.com", "Teams meeting links must use a Teams domain");
            case "Meet" -> requireHost(host, "meet.google.com", "Meet meeting links must use a Google Meet domain");
            default -> throw new IllegalArgumentException("Unsupported video conference provider");
        }
    }

    private static void requireHost(String host, String expectedHost, String message) {
        if (!host.equals(expectedHost) && !host.endsWith("." + expectedHost)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void rejectInternalHost(String host) {
        String normalizedHost = Objects.requireNonNull(host, "host is required")
                .trim()
                .toLowerCase(Locale.ROOT);
        rejectControlCharacters(normalizedHost, "Video conference host");
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("Video conference access URL cannot target localhost");
        }
        if (IPV4_PATTERN.matcher(normalizedHost).matches()) {
            rejectInternalIpv4(normalizedHost);
            return;
        }
        if (normalizedHost.contains(":")) {
            rejectInternalIpv6(normalizedHost);
        }
    }

    private static void rejectInternalIpv4(String host) {
        String[] parts = host.split("\\.");
        int[] octets = new int[4];
        for (int index = 0; index < parts.length; index++) {
            octets[index] = Integer.parseInt(parts[index]);
            if (octets[index] > 255) {
                throw new IllegalArgumentException("Video conference access URL host is invalid");
            }
        }
        boolean internal = octets[0] == 0
                || octets[0] == 10
                || octets[0] == 127
                || (octets[0] == 169 && octets[1] == 254)
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
        if (internal) {
            throw new IllegalArgumentException("Video conference access URL cannot target a private host");
        }
    }

    private static void rejectInternalIpv6(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Video conference access URL cannot target a private host");
            }
            if (address instanceof Inet6Address inet6Address) {
                byte firstByte = inet6Address.getAddress()[0];
                if ((firstByte & 0xfe) == 0xfc) {
                    throw new IllegalArgumentException("Video conference access URL cannot target a private host");
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Video conference access URL host is invalid", exception);
        }
    }

    private static void rejectControlCharacters(String value, String fieldLabel) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(fieldLabel + " cannot contain control characters");
            }
        }
    }
}
