package pt.isel.gape.common.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pt.isel.gape.common.config.DatabaseConfig;

public final class UploadRootResolver {

    private static final String UPLOAD_BASE_DIR_PROPERTY = "gape.upload.base-dir";

    private UploadRootResolver() {
    }

    public static Path resolve(String configuredPath) {
        return resolve(configuredPath, null);
    }

    public static Path resolve(String configuredPath, String webappRealPath) {
        String normalizedConfiguredPath = configuredPath == null || configuredPath.isBlank()
                ? "uploads"
                : configuredPath.trim();
        Path configured = Path.of(normalizedConfiguredPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        List<Path> candidates = new ArrayList<>();
        configuredUploadBaseDirectory().ifPresent(base -> candidates.add(base.resolve(configured)));
        projectRootFromCodeSource().ifPresent(projectRoot -> candidates.add(projectRoot.resolve(configured)));
        addUserDirCandidate(candidates, configured);
        addWebappCandidates(candidates, webappRealPath, configured);
        localAppDataDirectory().ifPresent(localAppData -> candidates.add(localAppData.resolve("GAPE").resolve(configured)));
        candidates.add(Path.of(System.getProperty("user.home", ".")).resolve(".gape").resolve(configured));

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (isUsableDirectory(normalized)) {
                return normalized;
            }
        }

        throw new IllegalStateException("No writable upload directory could be resolved for " + normalizedConfiguredPath);
    }

    public static String configuredUploadDirectory(String propertyName, String fallback) {
        String systemValue = System.getProperty(propertyName);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        return DatabaseConfig.getProperty(propertyName, fallback);
    }

    private static java.util.Optional<Path> configuredUploadBaseDirectory() {
        String systemValue = System.getProperty(UPLOAD_BASE_DIR_PROPERTY);
        String configuredValue = systemValue == null || systemValue.isBlank()
                ? DatabaseConfig.getProperty(UPLOAD_BASE_DIR_PROPERTY, "")
                : systemValue;
        if (configuredValue == null || configuredValue.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Path.of(configuredValue).toAbsolutePath().normalize());
    }

    private static void addWebappCandidates(List<Path> candidates, String webappRealPath, Path configured) {
        if (webappRealPath == null || webappRealPath.isBlank()) {
            return;
        }
        Path current = Path.of(webappRealPath).toAbsolutePath().normalize();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            candidates.add(current.resolve(configured));
            current = current.getParent();
        }
    }

    private static java.util.Optional<Path> projectRootFromCodeSource() {
        CodeSource codeSource = UploadRootResolver.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return java.util.Optional.empty();
        }
        try {
            URI uri = codeSource.getLocation().toURI();
            Path current = Path.of(uri).toAbsolutePath().normalize();
            if (Files.isRegularFile(current)) {
                current = current.getParent();
            }
            for (int depth = 0; depth < 10 && current != null; depth++) {
                if (Files.isRegularFile(current.resolve("pom.xml"))) {
                    return java.util.Optional.of(current);
                }
                current = current.getParent();
            }
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    private static void addUserDirCandidate(List<Path> candidates, Path configured) {
        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (!looksLikeProgramInstallation(userDir)) {
            candidates.add(userDir.resolve(configured));
        }
    }

    private static boolean looksLikeProgramInstallation(Path path) {
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/program files/");
    }

    private static java.util.Optional<Path> localAppDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Path.of(localAppData).toAbsolutePath().normalize());
    }

    private static boolean isUsableDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
            Path probe = Files.createTempFile(directory, ".gape-write-", ".tmp");
            Files.deleteIfExists(probe);
            return true;
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }
}
