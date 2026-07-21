package pt.isel.gape.security.transport;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTPS deployment policy. Reverse-proxy headers are ignored unless an
 * operator explicitly trusts the proxy, preventing a client from spoofing
 * transport security with {@code X-Forwarded-Proto}.
 */
public final class HttpsConfiguration {

    public static final String REQUIRE_HTTPS_ENVIRONMENT_VARIABLE = "GAPE_REQUIRE_HTTPS";
    public static final String TRUST_FORWARDED_HEADERS_ENVIRONMENT_VARIABLE = "GAPE_TRUST_FORWARDED_HEADERS";
    public static final String PUBLIC_HTTPS_ORIGIN_ENVIRONMENT_VARIABLE = "GAPE_PUBLIC_HTTPS_ORIGIN";

    private final boolean requireHttps;
    private final boolean trustForwardedHeaders;
    private final String publicHttpsOrigin;

    private HttpsConfiguration(boolean requireHttps, boolean trustForwardedHeaders, String publicHttpsOrigin) {
        this.requireHttps = requireHttps;
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.publicHttpsOrigin = publicHttpsOrigin;
    }

    public static HttpsConfiguration fromRuntimeConfiguration() {
        return RuntimeConfigurationHolder.CONFIGURATION;
    }

    public static void validateRuntimeConfiguration() {
        fromRuntimeConfiguration();
    }

    public boolean requiresHttps() {
        return requireHttps;
    }

    public boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        return trustForwardedHeaders && forwardedProtoIsHttps(request);
    }

    public String redirectTarget(HttpServletRequest request) {
        if (!requireHttps || publicHttpsOrigin == null) {
            throw new IllegalStateException("HTTPS redirect target is not configured");
        }
        String requestUri = request.getRequestURI();
        String query = request.getQueryString();
        return publicHttpsOrigin + (requestUri == null ? "/" : requestUri)
                + (query == null || query.isBlank() ? "" : "?" + query);
    }

    private static boolean forwardedProtoIsHttps(HttpServletRequest request) {
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null) {
            for (String element : forwarded.split(",")) {
                for (String parameter : element.split(";")) {
                    String[] keyValue = parameter.trim().split("=", 2);
                    if (keyValue.length == 2 && "proto".equalsIgnoreCase(keyValue[0].trim())
                            && "https".equalsIgnoreCase(stripQuotes(keyValue[1].trim()))) {
                        return true;
                    }
                }
            }
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null) {
            return false;
        }
        String firstValue = forwardedProto.split(",", 2)[0].trim();
        return "https".equalsIgnoreCase(firstValue);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static final class RuntimeConfigurationHolder {
        private static final HttpsConfiguration CONFIGURATION = load();

        private RuntimeConfigurationHolder() {
        }

        private static HttpsConfiguration load() {
            boolean requireHttps = strictBoolean(REQUIRE_HTTPS_ENVIRONMENT_VARIABLE, false);
            boolean trustForwardedHeaders = strictBoolean(TRUST_FORWARDED_HEADERS_ENVIRONMENT_VARIABLE, false);
            String origin = optionalEnvironment(PUBLIC_HTTPS_ORIGIN_ENVIRONMENT_VARIABLE);
            if (requireHttps && origin == null) {
                throw new IllegalStateException(PUBLIC_HTTPS_ORIGIN_ENVIRONMENT_VARIABLE
                        + " is required when " + REQUIRE_HTTPS_ENVIRONMENT_VARIABLE + " is true");
            }
            if (origin != null) {
                validateHttpsOrigin(origin);
            }
            return new HttpsConfiguration(requireHttps, trustForwardedHeaders, origin);
        }

        private static boolean strictBoolean(String environmentVariable, boolean defaultValue) {
            String value = optionalEnvironment(environmentVariable);
            if (value == null) {
                return defaultValue;
            }
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            throw new IllegalStateException(environmentVariable + " must be true or false");
        }

        private static String optionalEnvironment(String environmentVariable) {
            String value = System.getenv(environmentVariable);
            return value == null || value.isBlank() ? null : value.trim();
        }

        private static void validateHttpsOrigin(String origin) {
            try {
                URI uri = new URI(origin);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || uri.getHost() == null
                        || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                        || uri.getRawQuery() != null
                        || uri.getRawFragment() != null
                        || uri.getUserInfo() != null) {
                    throw invalidOrigin();
                }
            } catch (URISyntaxException exception) {
                throw invalidOrigin();
            }
        }

        private static IllegalStateException invalidOrigin() {
            return new IllegalStateException(PUBLIC_HTTPS_ORIGIN_ENVIRONMENT_VARIABLE
                    + " must be an origin such as https://gape.example.pt");
        }
    }
}
