package pt.isel.gape.web.controller;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ContentAssociationCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.model.ContentStorageContext;
import pt.isel.gape.learning.model.ReusableContentFile;
import pt.isel.gape.learning.model.UploadedContentFile;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.ContentFileService;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.AuditService;

@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 2L * 1024L * 1024L * 1024L,
        maxRequestSize = 2L * 1024L * 1024L * 1024L + 16L * 1024L * 1024L
)
public final class ContentUploadServlet extends HttpServlet {

    private final SessionManager sessionManager;
    private final ContentItemService contentItemService;
    private final ContentAssociationService contentAssociationService;
    private final AssessmentService assessmentService;
    private final ContentFileService contentFileService;
    private final PdfUploadService pdfUploadService;
    private final AuditService auditService;

    public ContentUploadServlet() {
        this(
                new SessionManager(),
                new ContentItemService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new ContentAssociationService(ConnectionProvider.defaultProvider(), Clock.systemUTC()),
                new AssessmentService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new ContentFileService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new PdfUploadService(),
                new AuditService(ConnectionProvider.defaultProvider(), ApplicationClock.system())
        );
    }

    ContentUploadServlet(
            SessionManager sessionManager,
            ContentItemService contentItemService,
            ContentAssociationService contentAssociationService,
            ContentFileService contentFileService,
            PdfUploadService pdfUploadService,
            AuditService auditService
    ) {
        this(
                sessionManager,
                contentItemService,
                contentAssociationService,
                new AssessmentService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                contentFileService,
                pdfUploadService,
                auditService
        );
    }

    ContentUploadServlet(
            SessionManager sessionManager,
            ContentItemService contentItemService,
            ContentAssociationService contentAssociationService,
            AssessmentService assessmentService,
            ContentFileService contentFileService,
            PdfUploadService pdfUploadService,
            AuditService auditService
    ) {
        this.sessionManager = sessionManager;
        this.contentItemService = contentItemService;
        this.contentAssociationService = contentAssociationService;
        this.assessmentService = assessmentService;
        this.contentFileService = contentFileService;
        this.pdfUploadService = pdfUploadService;
        this.auditService = auditService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<SessionUser> sessionUser = sessionManager.getSessionUser(request);
        if (sessionUser.isEmpty()) {
            auditService.record(null, null, "CONTENT_UPLOAD", "content_item", "new", "unauthorized", request.getRemoteAddr());
            writePlainError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required to upload content.");
            return;
        }
        Optional<AccessProfileType> profileType = sessionUser.get().primaryProfileType();
        if (profileType.isEmpty()) {
            auditService.record(sessionUser.get().userId(), sessionId(request), "CONTENT_UPLOAD",
                    "content_item", "new", "forbidden", request.getRemoteAddr());
            writePlainError(response, HttpServletResponse.SC_FORBIDDEN, "A valid profile is required to upload content.");
            return;
        }

        Long sessionId = sessionId(request);
        Long pendingFileContentItemId = null;
        try {
            ContentAssociationCommand associationCommand = associationCommand(request);
            Long repositorySourceContentItemId = optionalLongParameter(request, "repositorySourceContentItemId");
            if (repositorySourceContentItemId == null) {
                repositorySourceContentItemId = optionalLongParameter(request, "repositoryContentItemId");
            }
            if (repositorySourceContentItemId != null) {
                if (associationCommand == null) {
                    throw new IllegalArgumentException("Repository file requires a target context");
                }
                boolean useOriginalMetadata = useOriginalRepositoryMetadata(request);
                ReusableContentFile reusableFile = contentItemService.getReusableFileBackedContent(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        repositorySourceContentItemId,
                        associationCommand.type(),
                        associationCommand.targetId(),
                        request.getRemoteAddr()
                );
                if (isAssessmentRepositorySource(reusableFile.source())) {
                    if (associationCommand.type() != ContentAssociationType.CONTENT_BLOCK) {
                        throw new IllegalArgumentException("Assessment reuse requires a target content block");
                    }
                    long sourceAssessmentId = assessmentIdFromRepositorySource(reusableFile.source());
                    var clonedAssessment = assessmentService.cloneAssessmentToBlock(
                            sessionUser.get().userId(),
                            sessionId,
                            profileType.get(),
                            sourceAssessmentId,
                            associationCommand.targetId(),
                            request.getRemoteAddr()
                    );
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write(Long.toString(clonedAssessment.id()));
                    auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_ASSESSMENT_REUSE",
                            "assessment", sourceAssessmentId + ":" + clonedAssessment.id(),
                            "success", request.getRemoteAddr());
                    return;
                }
                ContentItem contentItem = contentItemService.createContentItem(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        new ContentItemCreateCommand(
                                useOriginalMetadata
                                        ? reusableFile.title()
                                        : requiredParameter(request, "title", "Content title is required"),
                                useOriginalMetadata
                                        ? reusableFile.description()
                                        : blankToNull(request.getParameter("description")),
                                reusableFile.format(),
                                reusableFile.source(),
                                ContentItemState.ACTIVE
                        ),
                        request.getRemoteAddr()
                );
                pendingFileContentItemId = contentItem.id();
                ContentAssociationCommand repositoryAssociationCommand = useOriginalMetadata
                        ? new ContentAssociationCommand(
                                associationCommand.type(),
                                associationCommand.targetId(),
                                associationCommand.role(),
                                associationCommand.orderNo(),
                                reusableFile.defaultMandatory()
                        )
                        : associationCommand;
                contentAssociationService.associateContent(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        contentItem.id(),
                        repositoryAssociationCommand,
                        request.getRemoteAddr()
                );
                pendingFileContentItemId = null;
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(Long.toString(contentItem.id()));
                auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_FILE_REUSE",
                        "content_item", contentItem.id() + ":" + repositorySourceContentItemId,
                        "success", request.getRemoteAddr());
                return;
            }

            ContentFormat format = contentFormat(request);
            String source;
            String fallbackTitle;
            UploadedContentFile uploadedFile = null;
            ContentItem contentItem = null;
            if (format == ContentFormat.PDF
                    || format == ContentFormat.TEXT
                    || format == ContentFormat.IMAGE
                    || format == ContentFormat.VIDEO
                    || format == ContentFormat.AUDIO
                    || format == ContentFormat.ARCHIVE) {
                Part filePart = requiredFilePart(request);
                source = pendingContentSource();
                fallbackTitle = submittedFileName(filePart);
                contentItem = contentItemService.createContentItem(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        new ContentItemCreateCommand(
                                parameterOrDefault(request, "title", fallbackTitle),
                                blankToNull(request.getParameter("description")),
                                format,
                                source,
                                ContentItemState.ACTIVE
                        ),
                        request.getRemoteAddr()
                );
                pendingFileContentItemId = contentItem.id();
                ContentStorageContext storageContext = ContentStorageContext.forContentItem(
                        contentItem.id(),
                        sessionUser.get().userId()
                );
                uploadedFile = pdfUploadService.saveContentFile(format, filePart, storageContext);
                contentItem = contentItemService.replaceContentSource(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        contentItem.id(),
                        format,
                        uploadedFile.relativePath(),
                        request.getRemoteAddr()
                );
                pendingFileContentItemId = null;
                recordContentFileMetadata(contentItem.id(), uploadedFile, sessionUser.get(), sessionId, request);
            } else if (format == ContentFormat.URL || format == ContentFormat.EMBED) {
                source = requiredParameter(request, "source", "Content source is required");
                fallbackTitle = switch (format) {
                    case URL -> "External link";
                    case EMBED -> "Embed content";
                    default -> "Content";
                };
            } else {
                throw new IllegalArgumentException("This content format is not supported by the upload endpoint");
            }
            if (uploadedFile == null) {
                contentItem = contentItemService.createContentItem(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        new ContentItemCreateCommand(
                                parameterOrDefault(request, "title", fallbackTitle),
                                blankToNull(request.getParameter("description")),
                                format,
                                source,
                                ContentItemState.ACTIVE
                        ),
                        request.getRemoteAddr()
                );
            }

            if (associationCommand != null) {
                if (contentItem == null) {
                    throw new IllegalStateException("Content item was not created");
                }
                contentAssociationService.associateContent(
                        sessionUser.get().userId(),
                        sessionId,
                        profileType.get(),
                        contentItem.id(),
                        associationCommand,
                        request.getRemoteAddr()
                );
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(Long.toString(contentItem.id()));
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_UPLOAD",
                    "content_item", Long.toString(contentItem.id()), "success", request.getRemoteAddr());
        } catch (IllegalArgumentException exception) {
            cleanupPendingContentItem(pendingFileContentItemId, sessionUser.get(), sessionId, profileType.get(), request);
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_UPLOAD",
                    "content_item", "new", "failure", request.getRemoteAddr());
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SecurityException exception) {
            cleanupPendingContentItem(pendingFileContentItemId, sessionUser.get(), sessionId, profileType.get(), request);
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_UPLOAD",
                    "content_item", "new", "denied", request.getRemoteAddr());
            writePlainError(response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (IOException | ServletException exception) {
            cleanupPendingContentItem(pendingFileContentItemId, sessionUser.get(), sessionId, profileType.get(), request);
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_UPLOAD",
                    "content_item", "new", "failure", request.getRemoteAddr());
            getServletContext().log("Content file upload failed", exception);
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    uploadProcessingMessage(exception));
        } catch (RuntimeException exception) {
            cleanupPendingContentItem(pendingFileContentItemId, sessionUser.get(), sessionId, profileType.get(), request);
            auditService.record(sessionUser.get().userId(), sessionId, "CONTENT_UPLOAD",
                    "content_item", "new", "failure", request.getRemoteAddr());
            getServletContext().log("Content upload could not be saved", exception);
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Content could not be saved. Please try again.");
        }
    }

    private void cleanupPendingContentItem(
            Long contentItemId,
            SessionUser sessionUser,
            Long sessionId,
            AccessProfileType profileType,
            HttpServletRequest request
    ) {
        if (contentItemId == null) {
            return;
        }
        try {
            contentItemService.discardPendingDetachedContentItem(
                    sessionUser.userId(),
                    sessionId,
                    profileType,
                    contentItemId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException cleanupException) {
            getServletContext().log("Pending content item cleanup failed: " + contentItemId, cleanupException);
        }
    }

    private void recordContentFileMetadata(
            long contentItemId,
            UploadedContentFile uploadedFile,
            SessionUser sessionUser,
            Long sessionId,
            HttpServletRequest request
    ) {
        try {
            contentFileService.recordReadyFile(contentItemId, uploadedFile);
        } catch (RuntimeException exception) {
            auditService.record(sessionUser.userId(), sessionId, "CONTENT_FILE_METADATA",
                    "content_item", Long.toString(contentItemId), "failure", request.getRemoteAddr());
        }
    }

    private ContentAssociationCommand associationCommand(HttpServletRequest request) {
        String contextType = blankToNull(request.getParameter("contextType"));
        String contextId = blankToNull(request.getParameter("contextId"));
        if (contextType == null && contextId == null) {
            return null;
        }
        if (contextType == null || contextId == null) {
            throw new IllegalArgumentException("Both contextType and contextId are required");
        }
        ContentAssociationType type = ContentAssociationType.fromDatabaseValue(contextType);
        long targetId = parseLong(contextId, "contextId");
        String role = parameterOrDefault(request, "role", "support_material");
        Integer orderNo = parseOptionalInteger(request.getParameter("orderNo"), "orderNo");
        boolean mandatory = Boolean.parseBoolean(request.getParameter("mandatory"));
        return new ContentAssociationCommand(type, targetId, role, orderNo, mandatory);
    }

    private static ContentFormat contentFormat(HttpServletRequest request) {
        String value = parameterOrDefault(request, "format", ContentFormat.PDF.toDatabaseValue());
        return ContentFormat.fromDatabaseValue(value);
    }

    private Long sessionId(HttpServletRequest request) {
        OptionalLong optionalSessionId = sessionManager.getDatabaseSessionId(request);
        return optionalSessionId.isPresent() ? optionalSessionId.getAsLong() : null;
    }

    private static long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be numeric", exception);
        }
    }

    private static Integer parseOptionalInteger(String value, String fieldName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be numeric", exception);
        }
    }

    private static Long optionalLongParameter(HttpServletRequest request, String name) {
        String normalized = blankToNull(request.getParameter(name));
        return normalized == null ? null : parseLong(normalized, name);
    }

    private static boolean useOriginalRepositoryMetadata(HttpServletRequest request) {
        return "original".equalsIgnoreCase(parameterOrDefault(request, "repositoryMetadataMode", "custom"));
    }

    private static String parameterOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = blankToNull(request.getParameter(name));
        return value == null ? fallback : value;
    }

    private static String requiredParameter(HttpServletRequest request, String name, String message) {
        String value = blankToNull(request.getParameter(name));
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Part requiredFilePart(HttpServletRequest request) throws IOException, ServletException {
        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() <= 0) {
            throw new IllegalArgumentException("Content file is required");
        }
        return filePart;
    }

    private static String submittedFileName(Part filePart) {
        String submittedFileName = blankToNull(filePart.getSubmittedFileName());
        return submittedFileName == null ? "Content file" : submittedFileName;
    }

    private static String pendingContentSource() {
        return "contents/pending/" + UUID.randomUUID() + "/content.pending";
    }

    private static boolean isAssessmentRepositorySource(String source) {
        return source != null && source.trim().startsWith("assessment:");
    }

    private static long assessmentIdFromRepositorySource(String source) {
        String value = source == null ? "" : source.trim().substring("assessment:".length());
        return parseLong(value, "assessmentRepositorySource");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message == null || message.isBlank()
                ? "Content could not be saved."
                : message);
    }

    private static String uploadProcessingMessage(Exception exception) {
        return "Content file could not be processed. Please check the file and try again.";
    }
}
