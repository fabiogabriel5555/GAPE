package pt.isel.gape.web.controller;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.common.config.DatabaseConfig;

@WebServlet(name = "mediaServlet", urlPatterns = "/media/*")
public final class MediaServlet extends HttpServlet {

    private static final int CACHE_SECONDS = 3600;
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif");

    private final String configuredUploadDir;
    private Path uploadRoot;

    public MediaServlet() {
        this(DatabaseConfig.getProperty("gape.upload.dir", "uploads"));
    }

    MediaServlet(String configuredUploadDir) {
        this.configuredUploadDir = configuredUploadDir;
    }

    MediaServlet(Path uploadRoot) {
        this.configuredUploadDir = null;
        this.uploadRoot = uploadRoot.toAbsolutePath().normalize();
    }

    @Override
    public void init() throws ServletException {
        if (uploadRoot == null) {
            uploadRoot = resolveUploadRoot(configuredUploadDir, getServletContext());
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

        String contentType = getServletContext().getMimeType(mediaPath.getFileName().toString());
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS);
        response.setContentLengthLong(Files.size(mediaPath));
        Files.copy(mediaPath, response.getOutputStream());
    }

    private Path resolveMediaPath(String rawRelativePath) {
        try {
            Path relativePath = Path.of(rawRelativePath).normalize();
            if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
                return null;
            }

            Path resolved = uploadRoot.resolve(relativePath).normalize();
            if (!resolved.startsWith(uploadRoot)) {
                return null;
            }
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }

            return resolveImagePathWithAvailableExtension(resolved);
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private Path resolveImagePathWithAvailableExtension(Path requestedPath) {
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
            if (candidate.startsWith(uploadRoot) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path candidate : stream) {
                String candidateFileName = candidate.getFileName().toString();
                String candidateExtension = extension(candidateFileName);
                if (IMAGE_EXTENSIONS.contains(candidateExtension)
                        && baseName(candidateFileName).equalsIgnoreCase(requestedBaseName)
                        && candidate.normalize().startsWith(uploadRoot)
                        && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        } catch (IOException ignored) {
            return requestedPath;
        }

        return requestedPath;
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

    private static Path resolveUploadRoot(String configuredPath, ServletContext servletContext) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }

        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(System.getProperty("user.dir")).resolve(path));

        String realPath = servletContext.getRealPath("/");
        if (realPath != null && !realPath.isBlank()) {
            Path current = Path.of(realPath).toAbsolutePath().normalize();
            for (int depth = 0; depth < 8 && current != null; depth++) {
                candidates.add(current.resolve(path));
                current = current.getParent();
            }
        }

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                return normalized;
            }
        }

        return candidates.getFirst().toAbsolutePath().normalize();
    }
}
