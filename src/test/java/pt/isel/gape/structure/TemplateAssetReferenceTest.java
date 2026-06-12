package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TemplateAssetReferenceTest {

    private static final List<String> APPLICATION_ENDPOINT_PREFIXES = List.of(
            "auth/",
            "/auth/",
            "admin/courses/",
            "/admin/courses/",
            "admin/organizations/",
            "/admin/organizations/",
            "admin/subjects/",
            "/admin/subjects/",
            "admin/users/",
            "/admin/users/",
            "courses/",
            "/courses/",
            "student/enrollments/",
            "/student/enrollments/"
    );
    private static final Set<String> APPLICATION_ENDPOINTS = Set.of(
            "account/deletion-requests",
            "/account/deletion-requests",
            "admin/activity-log",
            "/admin/activity-log",
            "admin/deletion-requests",
            "/admin/deletion-requests",
            "admin/courses",
            "/admin/courses",
            "admin/organizations",
            "/admin/organizations",
            "admin/subjects",
            "/admin/subjects",
            "admin/users",
            "/admin/users",
            "courses",
            "/courses",
            "dashboard",
            "/dashboard",
            "profile",
            "/profile",
            "student/enrollments",
            "/student/enrollments"
    );

    private enum ReferenceMode {
        WEBAPP_RELATIVE,
        FILE_RELATIVE
    }

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Pattern HTML_REF_PATTERN =
            Pattern.compile("(?:href|src)\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECTIVE_INCLUDE_PATTERN =
            Pattern.compile("<%@\\s*include\\s+file\\s*=\\s*['\"]([^'\"]+)['\"]\\s*%>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_INCLUDE_PATTERN =
            Pattern.compile("<jsp:include\\b[^>]*\\bpage\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_URL_PATTERN =
            Pattern.compile("url\\((['\"]?)([^)'\"\\s]+)\\1\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PUBLIC_AVATAR_LINK_PATTERN = Pattern.compile(
            "<a\\s+href\\s*=\\s*\"([^\"]+)\"\\s+class\\s*=\\s*\"info-action gape-user-avatar-trigger\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final String PUBLIC_AVATAR_LINK_TARGET =
            "${pageContext.request.contextPath}${sessionScope['gape.auth.authenticated'] eq true ? '/profile' : '/login.jsp'}";

    @Test
    void cssReferencesResolve() throws IOException {
        List<String> missing = new ArrayList<>();

        for (Path file : markupFiles()) {
            for (String reference : findReferences(file, HTML_REF_PATTERN, 1)) {
                if (isCssReference(reference)) {
                    validateResolvedPath(file, reference, missing, ReferenceMode.WEBAPP_RELATIVE);
                }
            }
        }

        assertNoMissing("Missing CSS references", missing);
    }

    @Test
    void javaScriptReferencesResolve() throws IOException {
        List<String> missing = new ArrayList<>();

        for (Path file : markupFiles()) {
            for (String reference : findReferences(file, HTML_REF_PATTERN, 1)) {
                if (isJavaScriptReference(reference)) {
                    validateResolvedPath(file, reference, missing, ReferenceMode.WEBAPP_RELATIVE);
                }
            }
        }

        assertNoMissing("Missing JavaScript references", missing);
    }

    @Test
    void imageReferencesResolve() throws IOException {
        List<String> missing = new ArrayList<>();

        for (Path file : markupFiles()) {
            for (String reference : findReferences(file, HTML_REF_PATTERN, 1)) {
                if (isImageReference(reference)) {
                    validateResolvedPath(file, reference, missing, ReferenceMode.WEBAPP_RELATIVE);
                }
            }
        }

        for (Path cssFile : cssFiles()) {
            for (String reference : findReferences(cssFile, CSS_URL_PATTERN, 2)) {
                if (isImageReference(reference)) {
                    validateResolvedPath(cssFile, reference, missing, ReferenceMode.FILE_RELATIVE);
                }
            }
        }

        assertNoMissing("Missing image references", missing);
    }

    @Test
    void jspIncludesResolveWhenPresent() throws IOException {
        List<String> missing = new ArrayList<>();

        for (Path file : markupFiles()) {
            for (String reference : findReferences(file, DIRECTIVE_INCLUDE_PATTERN, 1)) {
                validateResolvedPath(file, reference, missing, ReferenceMode.FILE_RELATIVE);
            }
            for (String reference : findReferences(file, ACTION_INCLUDE_PATTERN, 1)) {
                validateResolvedPath(file, reference, missing, ReferenceMode.FILE_RELATIVE);
            }
        }

        assertNoMissing("Missing JSP include targets", missing);
    }

    @Test
    void noObviousBrokenLocalPathsRemain() throws IOException {
        List<String> missing = new ArrayList<>();

        for (Path file : markupFiles()) {
            for (String reference : findReferences(file, HTML_REF_PATTERN, 1)) {
                if (shouldValidateAsLocalPath(reference)) {
                    validateResolvedPath(file, reference, missing, ReferenceMode.WEBAPP_RELATIVE);
                }
            }
        }

        for (Path cssFile : cssFiles()) {
            for (String reference : findReferences(cssFile, CSS_URL_PATTERN, 2)) {
                if (shouldValidateAsLocalPath(reference)) {
                    validateResolvedPath(cssFile, reference, missing, ReferenceMode.FILE_RELATIVE);
                }
            }
        }

        assertNoMissing("Found broken local template paths", missing);
    }

    @Test
    void mediaServletIsExplicitlyMappedForUserImages() throws IOException {
        String webXml = Files.readString(WEBAPP_DIR.resolve("WEB-INF/web.xml"));

        assertTrue(webXml.contains("pt.isel.gape.web.controller.MediaServlet"));
        assertTrue(webXml.contains("<url-pattern>/media/*</url-pattern>"));
    }

    @Test
    void profileServletIsExplicitlyMappedForAvatarNavigation() throws IOException {
        String webXml = Files.readString(WEBAPP_DIR.resolve("WEB-INF/web.xml"));

        assertTrue(webXml.contains("pt.isel.gape.web.controller.ProfileServlet"));
        assertTrue(webXml.contains("<url-pattern>/profile</url-pattern>"));
    }

    @Test
    void publicAvatarLinksGoToLoginUntilSessionExists() throws IOException {
        List<String> unexpected = new ArrayList<>();
        int linkCount = 0;

        for (Path file : markupFiles()) {
            String content = Files.readString(file);
            Matcher matcher = PUBLIC_AVATAR_LINK_PATTERN.matcher(content);
            while (matcher.find()) {
                linkCount++;
                String href = matcher.group(1);
                if (!PUBLIC_AVATAR_LINK_TARGET.equals(href)) {
                    unexpected.add(WEBAPP_DIR.relativize(file) + " -> " + href);
                }
            }
        }

        assertTrue(linkCount > 0, "No public avatar links were found");
        assertNoMissing("Public avatar links with unexpected targets", unexpected);
    }

    private static List<Path> markupFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(WEBAPP_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".jsp") || name.endsWith(".jspf");
                    })
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> cssFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(WEBAPP_DIR.resolve("assets/css"))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".css"))
                    .sorted()
                    .toList();
        }
    }

    private static List<String> findReferences(Path file, Pattern pattern, int groupIndex) throws IOException {
        String content = Files.readString(file);
        Matcher matcher = pattern.matcher(content);
        return matcher.results()
                .map(result -> result.group(groupIndex))
                .toList();
    }

    private static boolean isCssReference(String reference) {
        return normalizedReferenceForTypeChecks(reference).toLowerCase(Locale.ROOT).endsWith(".css");
    }

    private static boolean isJavaScriptReference(String reference) {
        return normalizedReferenceForTypeChecks(reference).toLowerCase(Locale.ROOT).endsWith(".js");
    }

    private static boolean isImageReference(String reference) {
        String normalized = normalizedReferenceForTypeChecks(reference).toLowerCase(Locale.ROOT);
        return normalized.endsWith(".png")
                || normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg")
                || normalized.endsWith(".gif")
                || normalized.endsWith(".webp")
                || normalized.endsWith(".svg")
                || normalized.endsWith(".ico");
    }

    private static String normalizedReferenceForTypeChecks(String reference) {
        return stripQueryAndFragment(stripContextPathPrefix(reference));
    }

    private static boolean shouldValidateAsLocalPath(String reference) {
        String normalized = stripContextPathPrefix(reference).trim();

        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("#")) {
            return false;
        }
        if (normalized.startsWith("javascript:")) {
            return false;
        }
        if (normalized.startsWith("data:")) {
            return false;
        }
        if (normalized.startsWith("mailto:") || normalized.startsWith("tel:")) {
            return false;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return false;
        }
        if (isApplicationEndpoint(normalized)) {
            return false;
        }

        return !normalized.contains("${") && !normalized.contains("<%");
    }

    private static void validateResolvedPath(
            Path sourceFile,
            String reference,
            List<String> missing,
            ReferenceMode referenceMode
    ) {
        if (!shouldValidateAsLocalPath(reference)) {
            return;
        }

        String normalized = stripQueryAndFragment(stripContextPathPrefix(reference));
        Path target = resolveReference(sourceFile, normalized, referenceMode);

        if (!Files.exists(target)) {
            missing.add(WEBAPP_DIR.relativize(sourceFile) + " -> " + normalized);
        }
    }

    private static Path resolveReference(Path sourceFile, String reference, ReferenceMode referenceMode) {
        if (reference.startsWith("/")) {
            return WEBAPP_DIR.resolve(reference.substring(1)).normalize();
        }
        if (referenceMode == ReferenceMode.WEBAPP_RELATIVE
                && !reference.startsWith("./")
                && !reference.startsWith("../")) {
            return WEBAPP_DIR.resolve(reference).normalize();
        }
        return sourceFile.getParent().resolve(reference).normalize();
    }

    private static String stripContextPathPrefix(String reference) {
        String prefix = "${pageContext.request.contextPath}";
        if (reference.startsWith(prefix + "/")) {
            return reference.substring(prefix.length() + 1);
        }
        if (reference.startsWith(prefix)) {
            return reference.substring(prefix.length());
        }
        return reference;
    }

    private static String stripQueryAndFragment(String reference) {
        int queryIndex = reference.indexOf('?');
        int fragmentIndex = reference.indexOf('#');
        int end = reference.length();

        if (queryIndex >= 0) {
            end = Math.min(end, queryIndex);
        }
        if (fragmentIndex >= 0) {
            end = Math.min(end, fragmentIndex);
        }

        return reference.substring(0, end);
    }

    private static boolean isApplicationEndpoint(String reference) {
        return APPLICATION_ENDPOINTS.contains(reference)
                || APPLICATION_ENDPOINT_PREFIXES.stream().anyMatch(reference::startsWith);
    }

    private static void assertNoMissing(String heading, List<String> missing) {
        List<String> sorted = missing.stream()
                .sorted(Comparator.naturalOrder())
                .distinct()
                .toList();

        assertTrue(
                sorted.isEmpty(),
                () -> heading + ":\n - " + String.join("\n - ", sorted)
        );
    }
}
