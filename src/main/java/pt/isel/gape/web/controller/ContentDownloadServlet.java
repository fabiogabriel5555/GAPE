package pt.isel.gape.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.AuditService;

public final class ContentDownloadServlet extends HttpServlet {

    private final SessionManager sessionManager;
    private final ContentItemService contentItemService;
    private final PdfUploadService pdfUploadService;
    private final AuditService auditService;

    public ContentDownloadServlet() {
        this(
                new SessionManager(),
                new ContentItemService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new PdfUploadService(),
                new AuditService(ConnectionProvider.defaultProvider(), ApplicationClock.system())
        );
    }

    ContentDownloadServlet(
            SessionManager sessionManager,
            ContentItemService contentItemService,
            PdfUploadService pdfUploadService,
            AuditService auditService
    ) {
        this.sessionManager = sessionManager;
        this.contentItemService = contentItemService;
        this.pdfUploadService = pdfUploadService;
        this.auditService = auditService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<SessionUser> sessionUser = sessionManager.getSessionUser(request);
        if (sessionUser.isEmpty()) {
            auditService.record(null, null, "CONTENT_DOWNLOAD", "content_item", "unknown",
                    "unauthorized", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long sessionId = sessionId(request);
        Optional<AccessProfileType> profileType = sessionUser.get().primaryProfileType();
        if (profileType.isEmpty()) {
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_DOWNLOAD",
                    "content_item", "unknown", "forbidden", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long contentItemId = -1L;
        try {
            contentItemId = resolveContentItemId(request);
            ContentItem contentItem = contentItemService.getContentItem(
                    sessionUser.get().userId(),
                    sessionId,
                    profileType.get(),
                    contentItemId,
                    request.getRemoteAddr()
            );
            boolean thumbnailVariant = isThumbnailVariant(request);
            if (!thumbnailVariant && !isDownloadableFile(contentItem.format())) {
                response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            String storedRelativePath = thumbnailVariant
                    ? contentItemService.getContentThumbnailPath(
                            sessionUser.get().userId(),
                            sessionId,
                            profileType.get(),
                            contentItemId,
                            request.getRemoteAddr()
                    ).orElse(null)
                    : contentItem.source();
            if (storedRelativePath == null || storedRelativePath.isBlank()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path storedFile = pdfUploadService.resolveStoredContentFile(storedRelativePath);
            if (!Files.isRegularFile(storedFile)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(thumbnailVariant ? thumbnailContentType(storedFile) : contentType(contentItem.format(), storedFile));
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store");
            response.setHeader("Content-Disposition",
                    requestedDisposition(request, thumbnailVariant) + "; filename=\"" + safeFileName(contentItem, storedFile) + "\"");
            response.setContentLengthLong(Files.size(storedFile));
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_DOWNLOAD",
                    "content_item", Long.toString(contentItemId), "success", request.getRemoteAddr());
            Files.copy(storedFile, response.getOutputStream());
        } catch (IllegalArgumentException exception) {
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_DOWNLOAD",
                    "content_item", auditIdentifier(contentItemId), "failure", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SecurityException exception) {
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_DOWNLOAD",
                    "content_item", auditIdentifier(contentItemId), "denied", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (RuntimeException exception) {
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_DOWNLOAD",
                    "content_item", auditIdentifier(contentItemId), "failure", request.getRemoteAddr());
            throw new ServletException("Failed to download content", exception);
        }
    }

    private static boolean isThumbnailVariant(HttpServletRequest request) {
        return "thumbnail".equalsIgnoreCase(request.getParameter("variant"));
    }

    private static String requestedDisposition(HttpServletRequest request, boolean forceInline) {
        if (forceInline) {
            return "inline";
        }
        String disposition = request.getParameter("disposition");
        return "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";
    }

    private static boolean isDownloadableFile(ContentFormat format) {
        return format == ContentFormat.PDF
                || format == ContentFormat.TEXT
                || format == ContentFormat.IMAGE
                || format == ContentFormat.VIDEO
                || format == ContentFormat.AUDIO;
    }

    private static String contentType(ContentFormat format, Path storedFile) throws IOException {
        String detected = Files.probeContentType(storedFile);
        if (detected != null && !detected.isBlank()) {
            return detected;
        }
        return switch (format) {
            case PDF -> "application/pdf";
            case TEXT -> "text/plain;charset=UTF-8";
            case IMAGE -> "image/webp";
            case VIDEO -> "video/mp4";
            case AUDIO -> "audio/mp4";
            default -> "application/octet-stream";
        };
    }

    private static String thumbnailContentType(Path storedFile) throws IOException {
        String detected = Files.probeContentType(storedFile);
        return detected == null || detected.isBlank() ? "image/webp" : detected;
    }

    private long resolveContentItemId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String rawId = pathInfo != null && pathInfo.length() > 1
                ? pathInfo.substring(1)
                : request.getParameter("id");
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("content id is required");
        }
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("content id must be numeric", exception);
        }
    }

    private Long sessionId(HttpServletRequest request) {
        OptionalLong optionalSessionId = sessionManager.getDatabaseSessionId(request);
        return optionalSessionId.isPresent() ? optionalSessionId.getAsLong() : null;
    }

    private static String auditIdentifier(long contentItemId) {
        return contentItemId > 0 ? Long.toString(contentItemId) : "unknown";
    }

    private static String safeFileName(ContentItem contentItem, Path storedFile) {
        String normalized = contentItem.title() == null || contentItem.title().isBlank()
                ? "content-" + contentItem.id()
                : contentItem.title().trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "content-" + contentItem.id();
        }
        String extension = extension(storedFile);
        if (!extension.isBlank() && !normalized.endsWith(extension)) {
            normalized += extension;
        }
        return normalized.replace("\"", "");
    }

    private static String extension(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        String value = fileName.toString().toLowerCase(Locale.ROOT);
        int dot = value.lastIndexOf('.');
        return dot >= 0 ? value.substring(dot) : "";
    }
}
