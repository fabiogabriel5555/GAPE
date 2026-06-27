package pt.isel.gape.web.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.common.storage.UploadRootResolver;

public final class JustificationAttachmentStorage {

    private static final long DEFAULT_MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    private static final String UPLOAD_DIR_PROPERTY = "gape.upload.dir";
    private static final String MAX_ATTACHMENT_BYTES_PROPERTY = "gape.justification.attachment.max-bytes";
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf",
            "png",
            "jpg",
            "jpeg",
            "webp",
            "doc",
            "docx",
            "txt"
    );

    private final String configuredUploadDir;
    private final long maxAttachmentBytes;

    public JustificationAttachmentStorage() {
        this(
                UploadRootResolver.configuredUploadDirectory(UPLOAD_DIR_PROPERTY, "uploads"),
                configuredMaxAttachmentBytes()
        );
    }

    JustificationAttachmentStorage(String configuredUploadDir, long maxAttachmentBytes) {
        if (maxAttachmentBytes <= 0L) {
            throw new IllegalArgumentException("max attachment size must be positive");
        }
        this.configuredUploadDir = configuredUploadDir;
        this.maxAttachmentBytes = maxAttachmentBytes;
    }

    public String saveAttachment(
            long studentUserId,
            long attendanceRecordId,
            Part attachmentPart,
            ServletContext servletContext
    ) throws IOException {
        if (attachmentPart == null || attachmentPart.getSize() <= 0L) {
            return null;
        }
        if (attachmentPart.getSize() > maxAttachmentBytes) {
            throw new IllegalArgumentException("The uploaded attachment exceeds the maximum allowed size.");
        }

        String extension = extensionFrom(attachmentPart.getSubmittedFileName());
        Path uploadRoot = resolveUploadRoot(servletContext);
        Path targetDirectory = uploadRoot.resolve("justifications").resolve(Long.toString(studentUserId)).normalize();
        if (!targetDirectory.startsWith(uploadRoot)) {
            throw new IOException("Invalid upload target");
        }
        Files.createDirectories(targetDirectory);

        String storedFileName = "attendance-" + attendanceRecordId + "-" + UUID.randomUUID() + "." + extension;
        Path target = targetDirectory.resolve(storedFileName).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw new IOException("Invalid upload target");
        }
        try (InputStream inputStream = attachmentPart.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "justifications/" + studentUserId + "/" + storedFileName;
    }

    private static String extensionFrom(String submittedFileName) {
        if (submittedFileName == null || submittedFileName.isBlank()) {
            throw new IllegalArgumentException("Attachment file name is required.");
        }
        String normalizedFileName = submittedFileName.replace('\\', '/');
        int slash = normalizedFileName.lastIndexOf('/');
        String fileName = slash < 0 ? normalizedFileName : normalizedFileName.substring(slash + 1);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new IllegalArgumentException("Attachment file type is not supported.");
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Attachment file type is not supported.");
        }
        return extension;
    }

    private Path resolveUploadRoot(ServletContext servletContext) {
        String realPath = servletContext == null ? null : servletContext.getRealPath("/");
        return UploadRootResolver.resolve(configuredUploadDir, realPath);
    }

    private static long configuredMaxAttachmentBytes() {
        String configuredValue = DatabaseConfig.getProperty(
                MAX_ATTACHMENT_BYTES_PROPERTY,
                Long.toString(DEFAULT_MAX_ATTACHMENT_BYTES)
        );
        try {
            return Long.parseLong(configuredValue);
        } catch (NumberFormatException exception) {
            return DEFAULT_MAX_ATTACHMENT_BYTES;
        }
    }
}
