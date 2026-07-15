package pt.isel.gape.learning.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import jakarta.servlet.http.Part;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.common.storage.UploadRootResolver;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentStorageContext;
import pt.isel.gape.learning.model.UploadedContentFile;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

public final class PdfUploadService {

    public static final long DEFAULT_MAX_PDF_BYTES = 150L * 1024L * 1024L;
    public static final long DEFAULT_MAX_TEXT_BYTES = 20L * 1024L * 1024L;
    public static final long DEFAULT_MAX_IMAGE_BYTES = 50L * 1024L * 1024L;
    public static final long DEFAULT_MAX_VIDEO_BYTES = 2L * 1024L * 1024L * 1024L;
    public static final long DEFAULT_MAX_AUDIO_BYTES = 750L * 1024L * 1024L;
    public static final long DEFAULT_MAX_ARCHIVE_BYTES = DEFAULT_MAX_VIDEO_BYTES;

    private static final int MAX_IMAGE_SIDE = 2560;
    private static final float WEBP_QUALITY = 0.82f;
    private static final Duration TOOL_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration MEDIA_TOOL_TIMEOUT = Duration.ofHours(2);
    private static final String UPLOAD_DIR_PROPERTY = "gape.upload.dir";
    private static final String WEBP_NATIVE_DIR_PROPERTY = "gape.webp.native.dir";
    private static final String STRICT_PROCESSING_PROPERTY = "gape.content.processing.strict";
    private static final String STRICT_MEDIA_PROCESSING_PROPERTY = "gape.content.media.processing.strict";
    private static final String MEDIA_PROCESSOR_EXECUTABLE_PROPERTY = "gape.media.processor.executable";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String TEXT_EXTENSION = ".txt";
    private static final byte[] PDF_MAGIC = new byte[]{'%', 'P', 'D', 'F', '-'};
    private static final Object IMAGE_IO_CONFIGURATION_LOCK = new Object();
    private static boolean imageIoPluginsScanned;

    private final Path uploadRoot;
    private final EnumMap<ContentFormat, Long> maxBytes;
    private final boolean strictProcessing;
    private final boolean strictMediaProcessing;
    private final String mediaProcessorExecutable;

    public PdfUploadService() {
        this(defaultConfiguration());
    }

    public PdfUploadService(Path uploadRoot, long maxPdfBytes) {
        this(uploadRoot, legacyLimits(maxPdfBytes, DEFAULT_MAX_MEDIA_BYTES()), true, true, resolveMediaProcessorExecutable(uploadRoot));
    }

    public PdfUploadService(Path uploadRoot, long maxPdfBytes, long maxMediaBytes) {
        this(uploadRoot, legacyLimits(maxPdfBytes, maxMediaBytes), true, true, resolveMediaProcessorExecutable(uploadRoot));
    }

    private PdfUploadService(ServiceConfiguration configuration) {
        this(
                configuration.uploadRoot(),
                configuration.maxBytes(),
                configuration.strictProcessing(),
                configuration.strictMediaProcessing(),
                configuration.mediaProcessorExecutable()
        );
    }

    private PdfUploadService(
            Path uploadRoot,
            Map<ContentFormat, Long> maxBytes,
            boolean strictProcessing,
            boolean strictMediaProcessing,
            String mediaProcessorExecutable
    ) {
        this.uploadRoot = Objects.requireNonNull(uploadRoot, "uploadRoot is required")
                .toAbsolutePath()
                .normalize();
        this.maxBytes = new EnumMap<>(ContentFormat.class);
        this.maxBytes.putAll(validateLimits(maxBytes));
        this.strictProcessing = strictProcessing;
        this.strictMediaProcessing = strictMediaProcessing;
        this.mediaProcessorExecutable = Objects.requireNonNull(mediaProcessorExecutable, "mediaProcessorExecutable is required");
    }

    public UploadedContentFile savePdf(Part part) throws IOException {
        Objects.requireNonNull(part, "part is required");
        try (InputStream inputStream = part.getInputStream()) {
            return savePdf(inputStream, part.getSubmittedFileName(), part.getContentType());
        }
    }

    public UploadedContentFile saveContentFile(ContentFormat format, Part part) throws IOException {
        Objects.requireNonNull(format, "format is required");
        Objects.requireNonNull(part, "part is required");
        if (!supportedUploadFormat(format)) {
            throw new IllegalArgumentException("Unsupported file-backed content format");
        }
        try (InputStream inputStream = part.getInputStream()) {
            return saveContentFile(format, inputStream, part.getSubmittedFileName(), part.getContentType());
        }
    }

    public UploadedContentFile saveContentFile(
            ContentFormat format,
            Part part,
            ContentStorageContext storageContext
    ) throws IOException {
        Objects.requireNonNull(format, "format is required");
        Objects.requireNonNull(part, "part is required");
        Objects.requireNonNull(storageContext, "storageContext is required");
        if (!supportedUploadFormat(format)) {
            throw new IllegalArgumentException("Unsupported file-backed content format");
        }
        try (InputStream inputStream = part.getInputStream()) {
            return saveContentFile(format, inputStream, part.getSubmittedFileName(), part.getContentType(), storageContext);
        }
    }

    public UploadedContentFile saveText(Part part) throws IOException {
        Objects.requireNonNull(part, "part is required");
        try (InputStream inputStream = part.getInputStream()) {
            return saveText(inputStream, part.getSubmittedFileName(), part.getContentType());
        }
    }

    public UploadedContentFile saveText(
            InputStream inputStream,
            String submittedFileName,
            String contentType
    ) throws IOException {
        return saveContentFile(ContentFormat.TEXT, inputStream, submittedFileName, contentType);
    }

    public UploadedContentFile savePdf(
            InputStream inputStream,
            String submittedFileName,
            String contentType
    ) throws IOException {
        return saveContentFile(ContentFormat.PDF, inputStream, submittedFileName, contentType);
    }

    public UploadedContentFile saveMedia(
            ContentFormat format,
            InputStream inputStream,
            String submittedFileName,
            String contentType
    ) throws IOException {
        if (format != ContentFormat.VIDEO && format != ContentFormat.AUDIO) {
            throw new IllegalArgumentException("Only video and audio media uploads are supported");
        }
        return saveContentFile(format, inputStream, submittedFileName, contentType);
    }

    public UploadedContentFile saveContentFile(
            ContentFormat format,
            InputStream inputStream,
            String submittedFileName,
            String contentType
    ) throws IOException {
        return saveContentFile(format, inputStream, submittedFileName, contentType, null);
    }

    public UploadedContentFile saveContentFile(
            ContentFormat format,
            InputStream inputStream,
            String submittedFileName,
            String contentType,
            ContentStorageContext storageContext
    ) throws IOException {
        Objects.requireNonNull(format, "format is required");
        Objects.requireNonNull(inputStream, "inputStream is required");
        if (!supportedUploadFormat(format)) {
            throw new IllegalArgumentException("Unsupported file-backed content format");
        }

        UploadSpec spec = UploadSpec.from(format, submittedFileName, contentType);
        CopyResult copied = copyToTemporaryFile(inputStream, maxBytes.get(format));
        Path contentsDirectory = null;
        String fileStem = null;
        try {
            validatePayload(format, copied.path());
            StorageTarget storageTarget = newContentTarget(format, storageContext);
            contentsDirectory = storageTarget.directory();
            fileStem = storageTarget.fileStem();
            Files.createDirectories(contentsDirectory);
            deleteExistingProcessedArtifacts(contentsDirectory, fileStem);

            Path finalFile = storageTarget.finalFile(spec.finalExtension());
            requireInsideRoot(finalFile);
            String thumbnailRelativePath = processFinalFile(format, copied.path(), finalFile, storageTarget.thumbnailFile());
            if (!Files.isRegularFile(finalFile) || Files.size(finalFile) == 0L) {
                throw new IllegalStateException("Processed content file was not created");
            }

            long finalSize = Files.size(finalFile);
            return new UploadedContentFile(
                    relativePath(finalFile),
                    spec.originalFileName(),
                    spec.finalContentType(),
                    finalSize,
                    null,
                    copied.size(),
                    finalSize,
                    copied.sha256(),
                    thumbnailRelativePath,
                    spec.originalContentType()
            );
        } catch (RuntimeException | IOException exception) {
            cleanupFailedProcessedArtifacts(contentsDirectory, fileStem, exception);
            throw exception;
        } finally {
            Files.deleteIfExists(copied.path());
        }
    }

    public Path resolveStoredContentFile(String relativePath) {
        String safeRelativePath = MediaPathValidator.safeRelativePath(relativePath)
                .orElseThrow(() -> new IllegalArgumentException("Invalid stored content path"));
        if (!safeRelativePath.startsWith("contents/")) {
            throw new IllegalArgumentException("Stored content path must be under contents");
        }
        Path resolved = uploadRoot.resolve(safeRelativePath).normalize();
        requireInsideRoot(resolved);
        return resolved;
    }

    public Path uploadRoot() {
        return uploadRoot;
    }

    public void deleteStoredContentFiles(List<String> relativePaths) throws IOException {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return;
        }
        IOException failure = null;
        for (String relativePath : relativePaths) {
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            try {
                Path storedFile = resolveStoredContentFile(relativePath);
                Files.deleteIfExists(storedFile);
                deleteEmptyContentDirectories(storedFile.getParent());
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public long maxPdfBytes() {
        return maxBytes.get(ContentFormat.PDF);
    }

    public long maxBytes(ContentFormat format) {
        Long value = maxBytes.get(format);
        if (value == null) {
            throw new IllegalArgumentException("Unsupported file-backed content format");
        }
        return value;
    }

    private String processFinalFile(
            ContentFormat format,
            Path inputFile,
            Path finalFile,
            Path thumbnailFile
    ) throws IOException {
        return switch (format) {
            case PDF -> {
                optimizePdf(inputFile, finalFile);
                yield null;
            }
            case TEXT -> {
                normalizeText(inputFile, finalFile);
                yield null;
            }
            case IMAGE -> {
                writeWebpImage(inputFile, finalFile, MAX_IMAGE_SIDE);
                yield null;
            }
            case VIDEO -> processVideo(inputFile, finalFile, thumbnailFile);
            case AUDIO -> {
                processAudio(inputFile, finalFile);
                yield null;
            }
            case ARCHIVE -> {
                Files.copy(inputFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                yield null;
            }
            default -> throw new IllegalArgumentException("Unsupported file-backed content format");
        };
    }

    private void optimizePdf(Path inputFile, Path finalFile) throws IOException {
        Path outputDirectory = finalFile.getParent();
        Files.createDirectories(outputDirectory);
        Path qpdfCandidate = Files.createTempFile(outputDirectory, "content-qpdf-", PDF_EXTENSION);
        Path ghostscriptCandidate = Files.createTempFile(outputDirectory, "content-gs-", PDF_EXTENSION);
        try {
            runTool(List.of("qpdf", "--linearize", inputFile.toString(), qpdfCandidate.toString()), TOOL_TIMEOUT);

            boolean ghostscriptProcessed = false;
            for (String ghostscript : List.of("gs", "gswin64c", "gswin32c")) {
                if (runTool(List.of(
                        ghostscript,
                        "-sDEVICE=pdfwrite",
                        "-dCompatibilityLevel=1.6",
                        "-dPDFSETTINGS=/ebook",
                        "-dNOPAUSE",
                        "-dQUIET",
                        "-dBATCH",
                        "-sOutputFile=" + ghostscriptCandidate,
                        inputFile.toString()
                ), TOOL_TIMEOUT)) {
                    ghostscriptProcessed = true;
                    break;
                }
            }

            Path bestCandidate = bestProcessedPdf(qpdfCandidate, ghostscriptProcessed ? ghostscriptCandidate : null);
            if (bestCandidate != null) {
                moveAtomically(bestCandidate, finalFile);
                return;
            }
            if (optimizePdfWithPdfBox(inputFile, finalFile)) {
                return;
            }
        } finally {
            Files.deleteIfExists(qpdfCandidate);
            Files.deleteIfExists(ghostscriptCandidate);
        }
        copyOrFail(inputFile, finalFile, "PDF optimization tools are not available");
    }

    private static boolean optimizePdfWithPdfBox(Path inputFile, Path finalFile) throws IOException {
        Path candidate = Files.createTempFile(finalFile.getParent(), "content-pdfbox-", PDF_EXTENSION);
        try {
            try {
                try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
                    document.save(candidate.toFile(), CompressParameters.DEFAULT_COMPRESSION);
                }
            } catch (IOException exception) {
                return false;
            }
            if (!validPdfCandidate(candidate)) {
                return false;
            }
            moveAtomically(candidate, finalFile);
            return true;
        } finally {
            Files.deleteIfExists(candidate);
        }
    }

    private static Path bestProcessedPdf(Path firstCandidate, Path secondCandidate) throws IOException {
        Path bestCandidate = validPdfCandidate(firstCandidate) ? firstCandidate : null;
        if (validPdfCandidate(secondCandidate)
                && (bestCandidate == null || Files.size(secondCandidate) < Files.size(bestCandidate))) {
            bestCandidate = secondCandidate;
        }
        return bestCandidate;
    }

    private static boolean validPdfCandidate(Path candidate) throws IOException {
        return candidate != null && Files.isRegularFile(candidate) && Files.size(candidate) > 0L && hasPdfMagic(candidate);
    }

    private static boolean hasPdfMagic(Path file) throws IOException {
        byte[] header = new byte[PDF_MAGIC.length];
        try (InputStream inputStream = Files.newInputStream(file)) {
            int read = inputStream.read(header);
            if (read < PDF_MAGIC.length) {
                return false;
            }
        }
        for (int index = 0; index < PDF_MAGIC.length; index++) {
            if (header[index] != PDF_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    private void normalizeText(Path inputFile, Path finalFile) throws IOException {
        byte[] bytes = Files.readAllBytes(inputFile);
        String text = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u0000", "");
        Files.writeString(finalFile, text, StandardCharsets.UTF_8);
    }

    private void writeWebpImage(Path inputFile, Path finalFile, int maxSide) throws IOException {
        configureImageIoForUploadRoot(uploadRoot);
        BufferedImage source = readImage(inputFile);
        if (source == null) {
            throw new IllegalArgumentException("The uploaded file is not a supported image.");
        }
        BufferedImage target = resizeToFit(source, maxSide);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            throw new IOException("WebP image writer is required to process image uploads.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(finalFile.toFile())) {
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = parameters.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    parameters.setCompressionType(preferredWebpCompressionType(compressionTypes));
                }
                parameters.setCompressionQuality(WEBP_QUALITY);
            }
            writer.setOutput(output);
            writer.write(null, new IIOImage(target, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private static String preferredWebpCompressionType(String[] compressionTypes) {
        for (String compressionType : compressionTypes) {
            if ("lossy".equalsIgnoreCase(compressionType)) {
                return compressionType;
            }
        }
        return compressionTypes[0];
    }

    private String processVideo(Path inputFile, Path finalFile, Path thumbnailFile) throws IOException {
        boolean processed = processVideoFastPath(inputFile, finalFile);
        if (!processed) {
            processed = processVideoTranscode(inputFile, finalFile);
        }
        if (!processed) {
            copyOrFail(
                    inputFile,
                    finalFile,
                    "Media processor is required to process video uploads",
                    strictMediaProcessing
            );
            return null;
        }

        Path thumbnail = thumbnailFile.normalize();
        requireInsideRoot(thumbnail);
        if (extractVideoThumbnail(finalFile, thumbnail, "00:00:00.5")
                || extractVideoThumbnail(finalFile, thumbnail, null)
                || extractVideoThumbnail(inputFile, thumbnail, null)) {
            return relativePath(thumbnail);
        }
        Files.deleteIfExists(thumbnail);
        return null;
    }

    private boolean extractVideoThumbnail(Path sourceFile, Path thumbnailFile, String seekPosition) throws IOException {
        Files.deleteIfExists(thumbnailFile);
        List<String> command = seekPosition == null
                ? List.of(
                        mediaProcessorExecutable,
                        "-y",
                        "-i", sourceFile.toString(),
                        "-frames:v", "1",
                        "-vf", "scale=640:-2",
                        thumbnailFile.toString()
                )
                : List.of(
                        mediaProcessorExecutable,
                        "-y",
                        "-ss", seekPosition,
                        "-i", sourceFile.toString(),
                        "-frames:v", "1",
                        "-vf", "scale=640:-2",
                        thumbnailFile.toString()
                );
        return runTool(command, Duration.ofMinutes(3))
                && Files.isRegularFile(thumbnailFile)
                && Files.size(thumbnailFile) > 0L;
    }

    private boolean processVideoFastPath(Path inputFile, Path finalFile) {
        return runTool(List.of(
                mediaProcessorExecutable,
                "-y",
                "-i", inputFile.toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-sn",
                "-dn",
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "160k",
                "-movflags", "+faststart",
                finalFile.toString()
        ), MEDIA_TOOL_TIMEOUT);
    }

    private boolean processVideoTranscode(Path inputFile, Path finalFile) {
        boolean processed = runTool(List.of(
                mediaProcessorExecutable,
                "-y",
                "-i", inputFile.toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-sn",
                "-dn",
                "-vf", "scale=trunc(min(1920\\,iw)/2)*2:-2",
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "160k",
                "-movflags", "+faststart",
                finalFile.toString()
        ), MEDIA_TOOL_TIMEOUT);
        return processed;
    }

    private void processAudio(Path inputFile, Path finalFile) throws IOException {
        if (runTool(List.of(
                mediaProcessorExecutable,
                "-y",
                "-i", inputFile.toString(),
                "-vn",
                "-c:a", "aac",
                "-b:a", "160k",
                finalFile.toString()
        ), MEDIA_TOOL_TIMEOUT)) {
            return;
        }
        copyOrFail(
                inputFile,
                finalFile,
                "Media processor is required to process audio uploads",
                strictMediaProcessing
        );
    }

    private void copyOrFail(Path inputFile, Path finalFile, String message) throws IOException {
        copyOrFail(inputFile, finalFile, message, strictProcessing);
    }

    private void copyOrFail(Path inputFile, Path finalFile, String message, boolean strict) throws IOException {
        if (strict) {
            throw new IOException(message);
        }
        Files.copy(inputFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean runTool(List<String> command, Duration timeout) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private CopyResult copyToTemporaryFile(InputStream inputStream, long maxBytes) throws IOException {
        Path tmpDirectory = uploadRoot.resolve("tmp").normalize();
        requireInsideRoot(tmpDirectory);
        Files.createDirectories(tmpDirectory);
        Path tempFile = Files.createTempFile(tmpDirectory, "content-upload-", ".tmp");
        MessageDigest digest = sha256Digest();
        long size = 0L;
        try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                size += read;
                if (size > maxBytes) {
                    throw new IllegalArgumentException("Content upload exceeds the maximum allowed size");
                }
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
            }
        } catch (RuntimeException | IOException exception) {
            Files.deleteIfExists(tempFile);
            throw exception;
        }
        if (size == 0L) {
            Files.deleteIfExists(tempFile);
            throw new IllegalArgumentException("Content upload cannot be empty");
        }
        return new CopyResult(tempFile, size, HexFormat.of().formatHex(digest.digest()));
    }

    private void validatePayload(ContentFormat format, Path file) throws IOException {
        if (format == ContentFormat.PDF) {
            requirePdfMagic(file);
            return;
        }
        if (format == ContentFormat.IMAGE) {
            try {
                if (readImage(file) == null) {
                    throw new IllegalArgumentException("The uploaded file is not a supported image.");
                }
            } catch (IOException exception) {
                throw new IllegalArgumentException("The uploaded file is not a supported image.", exception);
            }
        }
    }

    private StorageTarget newContentTarget(ContentFormat format, ContentStorageContext storageContext) {
        Path directory = uploadRoot.resolve("contents");
        String fileStem;
        if (storageContext == null || storageContext.uploaderUserId() == null) {
            directory = directory.resolve(storageDirectory(format))
                    .resolve("unassigned");
            fileStem = UUID.randomUUID().toString();
        } else {
            directory = directory.resolve(storageDirectory(format))
                    .resolve(Long.toString(storageContext.uploaderUserId()));
            fileStem = Long.toString(storageContext.contentItemId());
        }
        directory = directory.normalize();
        requireInsideRoot(directory);
        return new StorageTarget(directory, fileStem);
    }

    private static String storageDirectory(ContentFormat format) {
        return switch (format) {
            case PDF -> "pdf";
            case TEXT -> "text";
            case IMAGE -> "image";
            case VIDEO -> "video";
            case AUDIO -> "audio";
            case ARCHIVE -> "archive";
            default -> throw new IllegalArgumentException("Unsupported file-backed content format");
        };
    }

    private void deleteExistingProcessedArtifacts(Path contentsDirectory, String fileStem) throws IOException {
        requireInsideRoot(contentsDirectory);
        if (fileStem == null || fileStem.isBlank() || !Files.isDirectory(contentsDirectory)) {
            return;
        }
        try (var stream = Files.newDirectoryStream(contentsDirectory, path -> {
            String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
            return Files.isRegularFile(path)
                    && (fileName.startsWith(fileStem + ".") || (fileStem + "-thumb.webp").equals(fileName));
        })) {
            for (Path file : stream) {
                Path normalized = file.toAbsolutePath().normalize();
                requireInsideRoot(normalized);
                Files.deleteIfExists(normalized);
            }
        }
    }

    private void cleanupFailedProcessedArtifacts(Path contentsDirectory, String fileStem, Exception originalException) {
        if (contentsDirectory == null) {
            return;
        }
        try {
            deleteExistingProcessedArtifacts(contentsDirectory, fileStem);
            deleteEmptyContentDirectories(contentsDirectory);
        } catch (IOException | RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private String relativePath(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        requireInsideRoot(normalized);
        return uploadRoot.relativize(normalized).toString().replace('\\', '/');
    }

    private void deleteEmptyContentDirectories(Path directory) throws IOException {
        if (directory == null) {
            return;
        }
        Path contentRoot = uploadRoot.resolve("contents").toAbsolutePath().normalize();
        Path current = directory.toAbsolutePath().normalize();
        while (current.startsWith(contentRoot) && !current.equals(contentRoot)) {
            requireInsideRoot(current);
            if (!Files.isDirectory(current) || !isDirectoryEmpty(current)) {
                return;
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    private static UploadSpec uploadSpec(ContentFormat format, String submittedFileName, String contentType) {
        return UploadSpec.from(format, submittedFileName, contentType);
    }

    private static boolean supportedUploadFormat(ContentFormat format) {
        return format == ContentFormat.PDF
                || format == ContentFormat.TEXT
                || format == ContentFormat.IMAGE
                || format == ContentFormat.VIDEO
                || format == ContentFormat.AUDIO
                || format == ContentFormat.ARCHIVE;
    }

    private static BufferedImage readImage(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(inputStream);
            try {
                return ImageIO.read(imageInputStream);
            } finally {
                closeImageInputStream(imageInputStream);
            }
        }
    }

    private static void closeImageInputStream(MemoryCacheImageInputStream imageInputStream) throws IOException {
        try {
            imageInputStream.close();
        } catch (IOException exception) {
            if (!"closed".equalsIgnoreCase(exception.getMessage())) {
                throw exception;
            }
        }
    }

    private static BufferedImage resizeToFit(BufferedImage source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largestSide = Math.max(width, height);
        if (largestSide <= maxSide) {
            return toRgb(source, width, height);
        }
        double ratio = (double) maxSide / largestSide;
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));
        return drawImage(source, targetWidth, targetHeight);
    }

    private static BufferedImage toRgb(BufferedImage source, int width, int height) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        return drawImage(source, width, height);
    }

    private static BufferedImage drawImage(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static void configureImageIoForUploadRoot(Path uploadRoot) throws IOException {
        Path nativeDirectory = resolveWebpNativeDirectory();
        Files.createDirectories(nativeDirectory);
        synchronized (IMAGE_IO_CONFIGURATION_LOCK) {
            System.setProperty(WEBP_NATIVE_DIR_PROPERTY, nativeDirectory.toAbsolutePath().toString());
            ImageIO.setUseCache(false);
            if (!imageIoPluginsScanned) {
                ImageIO.scanForPlugins();
                imageIoPluginsScanned = true;
            }
        }
    }

    private static Path resolveWebpNativeDirectory() {
        String configuredDirectory = System.getProperty(WEBP_NATIVE_DIR_PROPERTY);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            configuredDirectory = DatabaseConfig.getProperty(WEBP_NATIVE_DIR_PROPERTY, "");
        }
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory).toAbsolutePath().normalize();
        }

        return Path.of(System.getProperty("user.home", "."), ".gape", "webp-native")
                .toAbsolutePath()
                .normalize();
    }

    private static void requirePdfMagic(Path file) throws IOException {
        if (Files.size(file) < PDF_MAGIC.length) {
            throw new IllegalArgumentException("PDF upload is too small");
        }
        if (!hasPdfMagic(file)) {
            throw new IllegalArgumentException("Uploaded file is not a valid PDF");
        }
    }

    private void requireInsideRoot(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Content storage path escapes the upload root");
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private static Path resolveConfiguredUploadRoot() {
        String configuredPath = UploadRootResolver.configuredUploadDirectory(UPLOAD_DIR_PROPERTY, "uploads");
        return UploadRootResolver.resolve(configuredPath);
    }

    private static EnumMap<ContentFormat, Long> configuredLimits() {
        EnumMap<ContentFormat, Long> limits = new EnumMap<>(ContentFormat.class);
        limits.put(ContentFormat.PDF, configuredMaxBytes("gape.content.pdf.max-bytes", DEFAULT_MAX_PDF_BYTES));
        limits.put(ContentFormat.TEXT, configuredMaxBytes("gape.content.text.max-bytes", DEFAULT_MAX_TEXT_BYTES));
        limits.put(ContentFormat.IMAGE, configuredMaxBytes("gape.content.image.max-bytes", DEFAULT_MAX_IMAGE_BYTES));
        limits.put(ContentFormat.VIDEO, configuredMaxBytes("gape.content.video.max-bytes", DEFAULT_MAX_VIDEO_BYTES));
        limits.put(ContentFormat.AUDIO, configuredMaxBytes("gape.content.audio.max-bytes", DEFAULT_MAX_AUDIO_BYTES));
        limits.put(ContentFormat.ARCHIVE, configuredMaxBytes("gape.content.archive.max-bytes", DEFAULT_MAX_ARCHIVE_BYTES));
        return limits;
    }

    private static EnumMap<ContentFormat, Long> legacyLimits(long maxPdfBytes, long maxMediaBytes) {
        EnumMap<ContentFormat, Long> limits = new EnumMap<>(ContentFormat.class);
        limits.put(ContentFormat.PDF, maxPdfBytes);
        limits.put(ContentFormat.TEXT, maxPdfBytes);
        limits.put(ContentFormat.IMAGE, DEFAULT_MAX_IMAGE_BYTES);
        limits.put(ContentFormat.VIDEO, maxMediaBytes);
        limits.put(ContentFormat.AUDIO, maxMediaBytes);
        limits.put(ContentFormat.ARCHIVE, maxMediaBytes);
        return limits;
    }

    private static Map<ContentFormat, Long> validateLimits(Map<ContentFormat, Long> limits) {
        for (ContentFormat format : List.of(
                ContentFormat.PDF,
                ContentFormat.TEXT,
                ContentFormat.IMAGE,
                ContentFormat.VIDEO,
                ContentFormat.AUDIO,
                ContentFormat.ARCHIVE
        )) {
            Long value = limits.get(format);
            if (value == null || value <= 0L) {
                throw new IllegalArgumentException(format.toDatabaseValue() + " max upload size must be positive");
            }
        }
        return limits;
    }

    private static long configuredMaxBytes(String propertyName, long fallback) {
        String configuredValue = DatabaseConfig.getProperty(propertyName, Long.toString(fallback));
        try {
            return Long.parseLong(configuredValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean configuredStrictProcessing() {
        return DatabaseConfig.getBooleanProperty(STRICT_PROCESSING_PROPERTY, true);
    }

    private static boolean configuredStrictMediaProcessing() {
        return DatabaseConfig.getBooleanProperty(STRICT_MEDIA_PROCESSING_PROPERTY, true);
    }

    private static ServiceConfiguration defaultConfiguration() {
        Path uploadRoot = resolveConfiguredUploadRoot();
        return new ServiceConfiguration(
                uploadRoot,
                configuredLimits(),
                configuredStrictProcessing(),
                configuredStrictMediaProcessing(),
                resolveMediaProcessorExecutable(uploadRoot)
        );
    }

    private static String resolveMediaProcessorExecutable(Path uploadRoot) {
        String configuredExecutable = configuredProperty(MEDIA_PROCESSOR_EXECUTABLE_PROPERTY, "");
        if (configuredExecutable != null && !configuredExecutable.isBlank()) {
            return Path.of(configuredExecutable).toAbsolutePath().normalize().toString();
        }
        ensureWritableProcessTemp(uploadRoot);
        try {
            return new DefaultFFMPEGLocator().getExecutablePath();
        } catch (RuntimeException exception) {
            return "ffmpeg";
        }
    }

    private static void ensureWritableProcessTemp(Path uploadRoot) {
        if (isWritableDirectory(systemTempDirectory())) {
            return;
        }
        Path processTempDirectory = uploadRoot.toAbsolutePath().normalize()
                .resolve("tmp")
                .resolve("process")
                .normalize();
        try {
            Files.createDirectories(processTempDirectory);
            if (isWritableDirectory(processTempDirectory)) {
                System.setProperty("java.io.tmpdir", processTempDirectory.toString());
            }
        } catch (IOException | RuntimeException exception) {
            // The media locator will still try its fallback resolution below.
        }
    }

    private static Path systemTempDirectory() {
        String configuredTempDirectory = System.getProperty("java.io.tmpdir");
        if (configuredTempDirectory == null || configuredTempDirectory.isBlank()) {
            return null;
        }
        try {
            return Path.of(configuredTempDirectory).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static boolean isWritableDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return false;
        }
        try {
            Path probe = Files.createTempFile(directory, ".gape-media-temp-", ".tmp");
            Files.deleteIfExists(probe);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static String configuredProperty(String propertyName, String fallback) {
        String systemValue = System.getProperty(propertyName);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        return DatabaseConfig.getProperty(propertyName, fallback);
    }

    private static long DEFAULT_MAX_MEDIA_BYTES() {
        return DEFAULT_MAX_VIDEO_BYTES;
    }

    private static String validateGenericFileName(String submittedFileName, String missingMessage) {
        if (submittedFileName == null || submittedFileName.isBlank()) {
            throw new IllegalArgumentException(missingMessage);
        }
        String normalized = submittedFileName.trim();
        if (normalized.indexOf('\0') >= 0
                || normalized.contains("\r")
                || normalized.contains("\n")
                || normalized.contains("/")
                || normalized.contains("\\")
                || normalized.contains(":")
                || normalized.contains("..")) {
            throw new IllegalArgumentException("Media file name is unsafe");
        }
        try {
            Path fileName = Path.of(normalized).getFileName();
            if (fileName == null || !fileName.toString().equals(normalized)) {
                throw new IllegalArgumentException("Media file name is unsafe");
            }
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Media file name is invalid", exception);
        }
        return normalized;
    }

    private record CopyResult(Path path, long size, String sha256) {
    }

    private record StorageTarget(Path directory, String fileStem) {

        private Path finalFile(String extension) {
            return directory.resolve(fileStem + extension).normalize();
        }

        private Path thumbnailFile() {
            return directory.resolve(fileStem + "-thumb.webp").normalize();
        }
    }

    private record ServiceConfiguration(
            Path uploadRoot,
            Map<ContentFormat, Long> maxBytes,
            boolean strictProcessing,
            boolean strictMediaProcessing,
            String mediaProcessorExecutable
    ) {
    }

    private record UploadSpec(
            String originalFileName,
            String originalExtension,
            String finalExtension,
            String finalContentType,
            String originalContentType
    ) {
        private static UploadSpec from(ContentFormat format, String submittedFileName, String contentType) {
            String originalName = validateGenericFileName(submittedFileName, "Content file name is required");
            String normalizedContentType = normalizedContentType(contentType);
            String originalExtension = validateExtension(format, originalName);
            validateContentType(format, normalizedContentType);
            return switch (format) {
                case PDF -> new UploadSpec(originalName, originalExtension, PDF_EXTENSION, "application/pdf", normalizedContentType);
                case TEXT -> new UploadSpec(originalName, originalExtension, TEXT_EXTENSION, "text/plain", normalizedContentType);
                case IMAGE -> new UploadSpec(originalName, originalExtension, ".webp", "image/webp", normalizedContentType);
                case VIDEO -> new UploadSpec(originalName, originalExtension, ".mp4", "video/mp4", normalizedContentType);
                case AUDIO -> new UploadSpec(originalName, originalExtension, ".m4a", "audio/mp4", normalizedContentType);
                case ARCHIVE -> new UploadSpec(originalName, originalExtension, originalExtension, normalizedContentType, normalizedContentType);
                default -> throw new IllegalArgumentException("Unsupported file-backed content format");
            };
        }

        private static String normalizedContentType(String contentType) {
            if (contentType == null || contentType.isBlank()) {
                return "application/octet-stream";
            }
            return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        }

        private static String validateExtension(ContentFormat format, String submittedFileName) {
            String lower = submittedFileName.toLowerCase(Locale.ROOT);
            for (String extension : allowedExtensions(format)) {
                if (lower.endsWith(extension)) {
                    return extension;
                }
            }
            throw new IllegalArgumentException(extensionError(format));
        }

        private static void validateContentType(ContentFormat format, String contentType) {
            if ("application/octet-stream".equals(contentType)) {
                return;
            }
            boolean allowed = switch (format) {
                case PDF -> "application/pdf".equals(contentType) || "application/x-pdf".equals(contentType);
                case TEXT -> "text/plain".equals(contentType);
                case IMAGE -> contentType.startsWith("image/");
                case VIDEO -> contentType.startsWith("video/");
                case AUDIO -> contentType.startsWith("audio/")
                        || "application/ogg".equals(contentType)
                        || "application/x-ogg".equals(contentType);
                case ARCHIVE -> isArchiveContentType(contentType);
                default -> false;
            };
            if (!allowed) {
                throw new IllegalArgumentException(contentTypeError(format));
            }
        }

        private static String[] allowedExtensions(ContentFormat format) {
            return switch (format) {
                case PDF -> new String[]{".pdf"};
                case TEXT -> new String[]{".txt"};
                case IMAGE -> new String[]{".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"};
                case VIDEO -> new String[]{".mp4", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".mpeg", ".mpg", ".3gp", ".3gpp"};
                case AUDIO -> new String[]{".mp3", ".m4a", ".wav", ".ogg", ".flac", ".aac", ".weba", ".opus"};
                case ARCHIVE -> new String[]{".zip", ".rar", ".7z", ".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".tar.xz", ".txz", ".gz", ".bz2", ".xz"};
                default -> new String[0];
            };
        }

        private static String extensionError(ContentFormat format) {
            return switch (format) {
                case PDF -> "PDF upload must use a .pdf file";
                case TEXT -> "Text upload must use a .txt file";
                case IMAGE -> "Image upload must use .jpg, .jpeg, .png, .webp, .gif or .bmp";
                case VIDEO -> "Video upload must use .mp4, .webm, .mov, .m4v, .mkv, .avi, .mpeg, .mpg, .3gp or .3gpp";
                case AUDIO -> "Audio upload must use .mp3, .m4a, .wav, .ogg, .flac, .aac, .weba or .opus";
                case ARCHIVE -> "Archive upload must use .zip, .rar, .7z, .tar, .gz, .bz2 or .xz";
                default -> "Unsupported file-backed content format";
            };
        }

        private static String contentTypeError(ContentFormat format) {
            return switch (format) {
                case PDF -> "PDF upload must use application/pdf content type";
                case TEXT -> "Text upload must use text/plain content type";
                case IMAGE -> "Image upload must use a supported image content type";
                case VIDEO -> "Video upload must use a supported video content type";
                case AUDIO -> "Audio upload must use a supported audio content type";
                case ARCHIVE -> "Archive upload must use a supported archive content type";
                default -> "Unsupported file-backed content format";
            };
        }

        private static boolean isArchiveContentType(String contentType) {
            return "application/zip".equals(contentType)
                    || "application/x-zip-compressed".equals(contentType)
                    || "application/vnd.rar".equals(contentType)
                    || "application/x-rar-compressed".equals(contentType)
                    || "application/x-7z-compressed".equals(contentType)
                    || "application/x-tar".equals(contentType)
                    || "application/gzip".equals(contentType)
                    || "application/x-gzip".equals(contentType)
                    || "application/x-bzip2".equals(contentType)
                    || "application/x-xz".equals(contentType);
        }
    }
}
