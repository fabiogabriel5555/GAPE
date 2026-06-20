package pt.isel.gape.web.controller;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.common.storage.UploadRootResolver;

public final class MediaServlet extends HttpServlet {

    private static final int CACHE_SECONDS = 3600;
    private static final String ASSETS_PREFIX = "assets/";
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif");

    private final String configuredUploadDir;
    private Path uploadRoot;
    private Path webappRoot;

    public MediaServlet() {
        this(UploadRootResolver.configuredUploadDirectory("gape.upload.dir", "uploads"));
    }

    MediaServlet(String configuredUploadDir) {
        this.configuredUploadDir = configuredUploadDir;
    }

    MediaServlet(Path uploadRoot) {
        this(uploadRoot, null);
    }

    MediaServlet(Path uploadRoot, Path webappRoot) {
        this.configuredUploadDir = null;
        this.uploadRoot = uploadRoot.toAbsolutePath().normalize();
        this.webappRoot = webappRoot == null ? null : webappRoot.toAbsolutePath().normalize();
    }

    @Override
    public void init() throws ServletException {
        if (uploadRoot == null) {
            uploadRoot = resolveUploadRoot(configuredUploadDir, getServletContext());
        }
        if (webappRoot == null) {
            webappRoot = resolveWebappRoot(getServletContext());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path mediaPath = resolveMediaPath(pathInfo.substring(1));
        if (mediaPath == null || !Files.isRegularFile(mediaPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(contentTypeFor(mediaPath));
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS);
        response.setContentLengthLong(Files.size(mediaPath));
        Files.copy(mediaPath, response.getOutputStream());
    }

    private String contentTypeFor(Path mediaPath) {
        String fileName = mediaPath.getFileName().toString();
        if ("webp".equals(extension(fileName))) {
            return "image/webp";
        }

        String contentType = getServletContext().getMimeType(fileName);
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private Path resolveMediaPath(String rawRelativePath) {
        try {
            String normalizedRelativePath = normalizeMediaRelativePath(rawRelativePath);
            if (normalizedRelativePath.isBlank()) {
                return null;
            }
            if (isPrivateUploadReference(normalizedRelativePath)) {
                return null;
            }

            Path uploadedFile = resolveFromRoot(uploadRoot, normalizedRelativePath);
            if (uploadedFile != null) {
                return uploadedFile;
            }

            if (isWebappAssetReference(normalizedRelativePath)) {
                return resolveFromRoot(webappRoot, normalizedRelativePath);
            }

            return null;
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private Path resolveFromRoot(Path root, String normalizedRelativePath) {
        if (root == null) {
            return null;
        }

        try {
            Path relativePath = Path.of(normalizedRelativePath).normalize();
            if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
                return null;
            }

            Path resolved = root.resolve(relativePath).normalize();
            if (!resolved.startsWith(root)) {
                return null;
            }
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }

            Path resolvedWithAvailableExtension = resolveImagePathWithAvailableExtension(resolved, root);
            return Files.isRegularFile(resolvedWithAvailableExtension) ? resolvedWithAvailableExtension : null;
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static String normalizeMediaRelativePath(String rawRelativePath) {
        String normalized = rawRelativePath == null ? "" : rawRelativePath.trim().replace('\\', '/');
        normalized = stripAfter(normalized, '?');
        normalized = stripAfter(normalized, '#');
        normalized = stripBeforeKnownDirectory(normalized, "/media/");
        normalized = stripToKnownDirectory(normalized, "assets");
        normalized = stripBeforeKnownDirectory(normalized, "/uploads/");
        normalized = stripLeadingPathMarkers(normalized);
        normalized = stripKnownPrefix(normalized, "media/");
        normalized = stripKnownPrefix(normalized, "uploads/");
        normalized = stripToKnownDirectory(normalized, "assets");
        return stripLeadingPathMarkers(normalized);
    }

    private Path resolveImagePathWithAvailableExtension(Path requestedPath, Path allowedRoot) {
        Path parent = requestedPath.getParent();
        String requestedFileName = requestedPath.getFileName().toString();
        String requestedBaseName = baseName(requestedFileName);
        String requestedExtension = extension(requestedFileName);

        if (parent == null || requestedBaseName.isBlank()) {
            return requestedPath;
        }
        if (!requestedExtension.isBlank() && !IMAGE_EXTENSIONS.contains(requestedExtension)) {
            return requestedPath;
        }

        for (String imageExtension : IMAGE_EXTENSIONS) {
            Path candidate = parent.resolve(requestedBaseName + "." + imageExtension).normalize();
            if (candidate.startsWith(allowedRoot) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path candidate : stream) {
                String candidateFileName = candidate.getFileName().toString();
                String candidateExtension = extension(candidateFileName);
                if (IMAGE_EXTENSIONS.contains(candidateExtension)
                        && baseName(candidateFileName).equalsIgnoreCase(requestedBaseName)
                        && candidate.normalize().startsWith(allowedRoot)
                        && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        } catch (IOException ignored) {
            return requestedPath;
        }

        return requestedPath;
    }

    private static boolean isWebappAssetReference(String relativePath) {
        return relativePath.toLowerCase(Locale.ROOT).startsWith(ASSETS_PREFIX);
    }

    private static boolean isPrivateUploadReference(String relativePath) {
        String normalized = relativePath.toLowerCase(Locale.ROOT);
        return normalized.startsWith("contents/")
                || normalized.startsWith("messages/")
                || normalized.startsWith("justifications/")
                || normalized.startsWith("quarantine/")
                || normalized.startsWith("tmp/");
    }

    private static String baseName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex <= 0 ? fileName : fileName.substring(0, extensionIndex);
    }

    private static String extension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripAfter(String value, char marker) {
        int markerIndex = value.indexOf(marker);
        return markerIndex < 0 ? value : value.substring(0, markerIndex);
    }

    private static String stripBeforeKnownDirectory(String value, String directory) {
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        int directoryIndex = lowerCaseValue.indexOf(directory);
        if (directoryIndex < 0) {
            return value;
        }
        return value.substring(directoryIndex + directory.length());
    }

    private static String stripToKnownDirectory(String value, String directory) {
        String marker = "/" + directory + "/";
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        int directoryIndex = lowerCaseValue.indexOf(marker);
        if (directoryIndex < 0) {
            return value;
        }
        return value.substring(directoryIndex + 1);
    }

    private static String stripLeadingPathMarkers(String value) {
        String normalized = value;
        while (normalized.startsWith("/") || normalized.startsWith("./")) {
            normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized.substring(2);
        }
        return normalized;
    }

    private static String stripKnownPrefix(String value, String prefix) {
        String normalized = value;
        while (normalized.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            normalized = normalized.substring(prefix.length());
        }
        return normalized;
    }

    private static Path resolveUploadRoot(String configuredPath, ServletContext servletContext) {
        String realPath = servletContext == null ? null : servletContext.getRealPath("/");
        return UploadRootResolver.resolve(configuredPath, realPath);
    }

    private static Path resolveWebappRoot(ServletContext servletContext) {
        String realPath = servletContext.getRealPath("/");
        if (realPath == null || realPath.isBlank()) {
            return null;
        }
        return Path.of(realPath).toAbsolutePath().normalize();
    }
}
