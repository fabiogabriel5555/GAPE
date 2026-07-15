package pt.isel.gape.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.UploadedContentFile;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.model.CommunicationSnapshot;
import pt.isel.gape.transversal.model.DirectMessageContent;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessageSummary;
import pt.isel.gape.transversal.service.CommunicationReadService;
import pt.isel.gape.transversal.service.DirectMessageService;
import pt.isel.gape.transversal.service.MessageService;
import pt.isel.gape.web.view.CommunicationPageData;

@WebServlet(name = "communicationServlet", urlPatterns = {"/messages", "/messages/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 50L * 1024L * 1024L,
        maxRequestSize = 251L * 1024L * 1024L
)
public final class CommunicationServlet extends DashboardServletSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationServlet.class);
    private static final String MESSAGES_JSP = "/WEB-INF/views/transversal/messages.jsp";
    private static final String THREAD_JSP = "/WEB-INF/views/transversal/messages-thread.jsp";
    private static final String THREAD_MESSAGES_JSP = "/WEB-INF/views/transversal/messages-thread-messages.jsp";
    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;

    private final CommunicationReadService readService;
    private final DirectMessageService directMessageService;
    private final MessageService messageService;
    private final PdfUploadService pdfUploadService;
    private final CommunicationViewFactory viewFactory;
    private final Clock clock;

    public CommunicationServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private CommunicationServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new CommunicationReadService(connectionProvider),
                new DirectMessageService(connectionProvider, clock),
                new MessageService(connectionProvider, clock),
                new PdfUploadService(),
                new CommunicationViewFactory(),
                clock
        );
    }

    CommunicationServlet(
            CommunicationReadService readService,
            DirectMessageService directMessageService,
            MessageService messageService,
            PdfUploadService pdfUploadService,
            CommunicationViewFactory viewFactory,
            Clock clock
    ) {
        this.readService = readService;
        this.directMessageService = directMessageService;
        this.messageService = messageService;
        this.pdfUploadService = pdfUploadService;
        this.viewFactory = viewFactory;
        this.clock = clock;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length != 0) {
            if (segments.length == 1 && "thread".equals(segments[0])) {
                showThread(request, response);
                return;
            }
            if (segments.length == 2 && "thread".equals(segments[0]) && "messages".equals(segments[1])) {
                showOlderThreadMessages(request, response);
                return;
            }
            if (segments.length == 2 && "attachments".equals(segments[0])) {
                Long messageId = safeLong(segments[1]);
                if (messageId == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                downloadAttachment(request, response, messageId);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        showMessages(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length == 1 && "send".equals(segments[0])) {
            sendMessage(request, response);
            return;
        }
        if (segments.length == 1 && "read".equals(segments[0])) {
            markRead(request, response);
            return;
        }
        if (segments.length == 1 && "read-conversation".equals(segments[0])) {
            markConversationRead(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showMessages(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser currentUser = requireCurrentUser(request);
        messageService.processScheduledMessages(request.getRemoteAddr());
        CommunicationSnapshot snapshot = readService.loadSnapshot(
                currentUser.userId(),
                optionalLongParameter(request, "userId"),
                optionalLongParameter(request, "messageId")
        );
        CommunicationPageData pageData = viewFactory.pageData(snapshot, currentUser.userId());
        request.setAttribute("communicationPage", pageData);
        request.setAttribute("notificationUnreadCount", snapshot.notificationUnreadCount());
        request.setAttribute("notificationItems", pageData.getNotifications());
        request.setAttribute("messageUnreadCount", pageData.getUnreadCount());
        prepareDashboard(request, "message", "Messages");
        forward(request, response, MESSAGES_JSP);
    }

    private void showThread(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser currentUser = requireCurrentUser(request);
        CommunicationSnapshot snapshot = readService.loadThread(
                currentUser.userId(),
                optionalLongParameter(request, "userId"),
                optionalLongParameter(request, "messageId")
        );
        CommunicationPageData pageData = viewFactory.pageData(snapshot, currentUser.userId());
        request.setAttribute("communicationPage", pageData);
        response.setContentType("text/html;charset=UTF-8");
        forward(request, response, THREAD_JSP);
    }

    private void showOlderThreadMessages(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser currentUser = requireCurrentUser(request);
        CommunicationSnapshot snapshot = readService.loadOlderThreadMessages(
                currentUser.userId(),
                longParameter(request, "userId"),
                longParameter(request, "beforeMessageId")
        );
        CommunicationPageData pageData = viewFactory.pageData(snapshot, currentUser.userId());
        request.setAttribute("communicationPage", pageData);
        response.setContentType("text/html;charset=UTF-8");
        forward(request, response, THREAD_MESSAGES_JSP);
    }

    private void sendMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser currentUser = requireCurrentUser(request);
        Long sessionId = currentSessionId(request);
        long recipientUserId = longParameter(request, "recipientUserId");
        List<UploadedContentFile> uploadedFiles = new ArrayList<>();
        boolean messagesCommitted = false;
        try {
            List<Part> attachmentParts = submittedFileParts(request, "attachmentFile");
            if (attachmentParts.size() > MAX_ATTACHMENTS_PER_MESSAGE) {
                throw new IllegalArgumentException("You can send up to 5 files at once.");
            }
            for (Part attachmentPart : attachmentParts) {
                ContentFormat format = detectAttachmentFormat(attachmentPart);
                uploadedFiles.add(pdfUploadService.saveContentFile(format, attachmentPart));
            }

            String body = text(request, "body");
            List<DirectMessageContent> contents = new ArrayList<>();
            if (uploadedFiles.isEmpty()) {
                contents.add(DirectMessageContent.text(body));
            } else if (uploadedFiles.size() == 1) {
                UploadedContentFile uploadedFile = uploadedFiles.get(0);
                contents.add(DirectMessageContent.attachment(
                        body,
                        uploadedFile.relativePath(),
                        uploadedFile.originalFileName()
                ));
            } else {
                for (UploadedContentFile uploadedFile : uploadedFiles) {
                    contents.add(DirectMessageContent.attachment(
                            null,
                            uploadedFile.relativePath(),
                            uploadedFile.originalFileName()
                    ));
                }
                if (body != null) {
                    contents.add(DirectMessageContent.text(body));
                }
            }
            List<Message> messages = directMessageService.sendDirectMessages(
                    currentUser.userId(),
                    sessionId,
                    primaryProfile(currentUser),
                    recipientUserId,
                    contents,
                    request.getRemoteAddr()
            );
            messagesCommitted = true;
            deleteUploadedAttachmentAuxiliaryArtifacts(uploadedFiles);
            Message message = messages.get(messages.size() - 1);

            try {
                messageService.markDirectConversationRead(
                        currentUser.userId(),
                        sessionId,
                        recipientUserId,
                        LocalDateTime.now(clock),
                        request.getRemoteAddr()
                );
            } catch (RuntimeException ignored) {
                // Sending is already committed; read state can be retried by the normal conversation refresh.
            }

            if (isAjax(request)) {
                renderConversationUpdate(request, response, currentUser, recipientUserId, message == null ? null : message.id());
                return;
            }
            redirect(request, response, "/messages?userId=" + recipientUserId
                    + "&messageId=" + message.id());
        } catch (RuntimeException exception) {
            if (!messagesCommitted) {
                deleteUploadedAttachments(uploadedFiles);
            }
            if (isAjax(request)) {
                writeAjaxError(response, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
            redirect(request, response, "/messages?userId=" + recipientUserId);
        } catch (IOException | ServletException exception) {
            if (!messagesCommitted) {
                deleteUploadedAttachments(uploadedFiles);
            }
            if (isAjax(request)) {
                writeAjaxError(response, "The uploaded file could not be processed.");
                return;
            }
            flashError(request, "The uploaded file could not be processed.");
            redirect(request, response, "/messages?userId=" + recipientUserId);
        }
    }

    private void renderThread(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser currentUser,
            long recipientUserId,
            Long messageId
    ) throws ServletException, IOException {
        CommunicationSnapshot snapshot = readService.loadThread(currentUser.userId(), recipientUserId, messageId);
        CommunicationPageData pageData = viewFactory.pageData(snapshot, currentUser.userId());
        request.setAttribute("communicationPage", pageData);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        forward(request, response, THREAD_JSP);
    }

    private void renderConversationUpdate(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser currentUser,
            long recipientUserId,
            Long messageId
    ) throws ServletException, IOException {
        CommunicationSnapshot snapshot = readService.loadSnapshot(currentUser.userId(), recipientUserId, messageId);
        CommunicationPageData pageData = viewFactory.pageData(snapshot, currentUser.userId());
        request.setAttribute("communicationPage", pageData);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        forward(request, response, THREAD_JSP);
    }

    private void markRead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser currentUser = requireCurrentUser(request);
        Long sessionId = currentSessionId(request);
        long messageId = longParameter(request, "messageId");
        Long peerUserId = optionalLongParameter(request, "userId");
        try {
            messageService.markRead(
                    currentUser.userId(),
                    sessionId,
                    messageId,
                    LocalDateTime.now(clock),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Message marked as read.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, readRedirect(peerUserId, messageId));
    }

    private void markConversationRead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser currentUser = requireCurrentUser(request);
        Long sessionId = currentSessionId(request);
        long peerUserId = longParameter(request, "userId");
        try {
            int markedRead = messageService.markDirectConversationRead(
                    currentUser.userId(),
                    sessionId,
                    peerUserId,
                    LocalDateTime.now(clock),
                    request.getRemoteAddr()
            );
            int messageUnreadCount = readService.countUnreadDirectMessages(currentUser.userId());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"markedReadCount":%d,"messageUnreadCount":%d}
                    """.formatted(markedRead, messageUnreadCount));
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"error":"%s"}
                    """.formatted(jsonString(messageFor(exception))));
        }
    }

    private void downloadAttachment(HttpServletRequest request, HttpServletResponse response, long messageId)
            throws IOException, ServletException {
        SessionUser currentUser = requireCurrentUser(request);
        Optional<MessageSummary> optionalMessage = readService.findDirectMessageForUser(currentUser.userId(), messageId);
        if (optionalMessage.isEmpty()
                || optionalMessage.get().attachment() == null
                || optionalMessage.get().attachment().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        MessageSummary message = optionalMessage.get();
        Path storedFile;
        try {
            storedFile = pdfUploadService.resolveStoredContentFile(message.attachment());
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!Files.isRegularFile(storedFile)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(storedFile);
        boolean inline = "1".equals(request.getParameter("inline"))
                && isInlinePreviewType(contentType, message.attachment());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Content-Disposition",
                (inline ? "inline" : "attachment")
                        + "; filename=\"" + headerSafeFilename(attachmentFileName(message, storedFile)) + "\"");
        response.setContentLengthLong(Files.size(storedFile));
        Files.copy(storedFile, response.getOutputStream());
    }

    private static String readRedirect(Long peerUserId, long messageId) {
        if (peerUserId == null) {
            return "/messages?messageId=" + messageId;
        }
        return "/messages?userId=" + peerUserId + "&messageId=" + messageId;
    }

    private void deleteUploadedAttachments(List<UploadedContentFile> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return;
        }
        List<String> paths = uploadedFiles.stream()
                .flatMap(file -> attachmentArtifactPaths(file, true).stream())
                .distinct()
                .toList();
        if (paths.isEmpty()) {
            return;
        }
        try {
            pdfUploadService.deleteStoredContentFiles(paths);
        } catch (IOException exception) {
            LOGGER.warn("Could not remove failed message upload artifacts", exception);
        }
    }

    private void deleteUploadedAttachmentAuxiliaryArtifacts(List<UploadedContentFile> uploadedFiles) {
        List<String> paths = uploadedFiles.stream()
                .flatMap(file -> attachmentArtifactPaths(file, false).stream())
                .distinct()
                .toList();
        if (paths.isEmpty()) {
            return;
        }
        try {
            pdfUploadService.deleteStoredContentFiles(paths);
        } catch (IOException exception) {
            LOGGER.warn("Could not remove unreferenced message upload derivatives", exception);
        }
    }

    private static List<String> attachmentArtifactPaths(UploadedContentFile file, boolean includeFinalFile) {
        return java.util.stream.Stream.of(
                        includeFinalFile ? file.relativePath() : null,
                        file.originalRelativePath(),
                        file.thumbnailRelativePath()
                )
                .filter(path -> path != null && !path.isBlank())
                .filter(path -> includeFinalFile || !path.equals(file.relativePath()))
                .toList();
    }

    private static List<Part> submittedFileParts(HttpServletRequest request, String name)
            throws IOException, ServletException {
        List<Part> parts = new ArrayList<>();
        for (Part part : request.getParts()) {
            if (!name.equals(part.getName())
                    || part.getSize() <= 0
                    || part.getSubmittedFileName() == null
                    || part.getSubmittedFileName().isBlank()) {
                continue;
            }
            parts.add(part);
        }
        return parts;
    }

    private static ContentFormat detectAttachmentFormat(Part part) {
        String fileName = part.getSubmittedFileName();
        if (hasExtension(fileName, ".pdf")) {
            return ContentFormat.PDF;
        }
        if (hasExtension(fileName, ".txt")) {
            return ContentFormat.TEXT;
        }
        if (hasExtension(fileName, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")) {
            return ContentFormat.IMAGE;
        }
        if (hasExtension(fileName, ".mp4", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".mpeg", ".mpg", ".3gp", ".3gpp")) {
            return ContentFormat.VIDEO;
        }
        if (hasExtension(fileName, ".mp3", ".m4a", ".wav", ".ogg", ".flac", ".aac", ".weba", ".opus")) {
            return ContentFormat.AUDIO;
        }
        if (hasExtension(fileName, ".zip", ".rar", ".7z", ".tar", ".tar.gz", ".tgz", ".tar.bz2",
                ".tbz2", ".tar.xz", ".txz", ".gz", ".bz2", ".xz")) {
            return ContentFormat.ARCHIVE;
        }
        throw new IllegalArgumentException("Unsupported file-backed content format");
    }

    private static boolean hasExtension(String fileName, String... extensions) {
        String lowerCaseFileName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lowerCaseFileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String attachmentFileName(MessageSummary message, Path storedFile) {
        String candidate = message.title() == null || message.title().isBlank()
                ? storedFile.getFileName().toString()
                : message.title().trim();
        String extension = extension(storedFile);
        if (!extension.isBlank() && !candidate.toLowerCase(Locale.ROOT).endsWith(extension)) {
            candidate += extension;
        }
        return candidate;
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

    private static String headerSafeFilename(String filename) {
        String normalized = filename == null ? "attachment" : filename.trim();
        normalized = normalized.replaceAll("[\\r\\n\"\\\\]+", "_");
        normalized = normalized.replaceAll("[/]+", "_");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.isBlank() ? "attachment" : normalized;
    }

    private static String jsonString(String value) {
        String normalized = value == null ? "" : value;
        return normalized.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeAjaxError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"error":"%s"}
                """.formatted(jsonString(message)));
    }

    private static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static boolean isInlinePreviewType(String contentType, String attachment) {
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.startsWith("image/") || normalizedContentType.startsWith("video/")) {
            return true;
        }
        return hasExtension(attachment, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp",
                ".mp4", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".mpeg", ".mpg", ".3gp", ".3gpp");
    }

    private static Long safeLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long optionalLongParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }
}
