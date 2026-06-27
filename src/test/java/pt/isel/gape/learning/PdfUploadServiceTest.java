package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.isel.gape.learning.model.UploadedContentFile;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentStorageContext;
import pt.isel.gape.learning.service.PdfUploadService;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

class PdfUploadServiceTest {

    @TempDir
    Path uploadRoot;

    @Test
    void savesValidPdfUnderContentsDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);
        byte[] pdfBytes = validPdfBytes();

        UploadedContentFile uploadedFile = pdfUploadService.savePdf(
                new ByteArrayInputStream(pdfBytes),
                "lesson.pdf",
                "application/pdf; charset=binary"
        );

        assertTrue(uploadedFile.relativePath().startsWith("contents/"));
        assertTrue(uploadedFile.relativePath().endsWith(".pdf"));
        assertEquals("lesson.pdf", uploadedFile.originalFileName());
        assertEquals("application/pdf", uploadedFile.contentType());
        assertEquals(pdfBytes.length, uploadedFile.originalSize());
        assertEquals(64, uploadedFile.sha256().length());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesValidPdfWithGenericBrowserContentType() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.savePdf(
                new ByteArrayInputStream(validPdfBytes()),
                "lesson.pdf",
                "application/octet-stream"
        );

        assertTrue(uploadedFile.relativePath().endsWith(".pdf"));
        assertEquals("application/pdf", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesFileBackedContentUnderPedagogicalStructureWithoutOriginalCopy() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(
                ContentFormat.PDF,
                new ByteArrayInputStream(validPdfBytes()),
                "lesson.pdf",
                "application/pdf",
                ContentStorageContext.forContentBlock(53L, 60L, 120L, 9L)
        );

        assertEquals(
                "contents/pdf/9/120.pdf",
                uploadedFile.relativePath()
        );
        assertNull(uploadedFile.originalRelativePath());
        assertTrue(Files.exists(uploadRoot.resolve(uploadedFile.relativePath())));
        assertTrue(Files.notExists(uploadRoot.resolve("contents/items/120/original")));
    }

    @Test
    void savingFileBackedContentDoesNotDeleteOtherFilesFromSameUserDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);
        Path siblingFile = uploadRoot.resolve("contents/pdf/9/121.pdf");
        Files.createDirectories(siblingFile.getParent());
        Files.write(siblingFile, validPdfBytes());

        UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(
                ContentFormat.PDF,
                new ByteArrayInputStream(validPdfBytes()),
                "lesson.pdf",
                "application/pdf",
                ContentStorageContext.forContentItem(120L, 9L)
        );

        assertEquals("contents/pdf/9/120.pdf", uploadedFile.relativePath());
        assertTrue(Files.exists(uploadRoot.resolve(uploadedFile.relativePath())));
        assertTrue(Files.exists(siblingFile));
    }

    @Test
    void savesArchiveContentPreservingOriginalExtension() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);
        byte[] archiveBytes = "zip placeholder".getBytes(StandardCharsets.US_ASCII);

        UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(
                ContentFormat.ARCHIVE,
                new ByteArrayInputStream(archiveBytes),
                "answers.zip",
                "application/zip",
                ContentStorageContext.forContentItem(120L, 9L)
        );

        assertEquals("contents/archive/9/120.zip", uploadedFile.relativePath());
        assertEquals("application/zip", uploadedFile.contentType());
        assertEquals(archiveBytes.length, uploadedFile.originalSize());
        assertTrue(Files.exists(uploadRoot.resolve(uploadedFile.relativePath())));
    }

    @Test
    void defaultServiceUsesConfiguredSystemUploadDirectory() throws Exception {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            UploadedContentFile uploadedFile = pdfUploadService.saveText(
                    new ByteArrayInputStream("textual content".getBytes(StandardCharsets.UTF_8)),
                    "lesson.txt",
                    "text/plain"
            );

            assertTrue(uploadedFile.relativePath().startsWith("contents/"));
            assertTrue(Files.exists(uploadRoot.resolve(uploadedFile.relativePath())));
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }

    @Test
    void defaultServiceRejectsUnprocessedVideoInsteadOfCopyingOriginal() {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            assertThrows(
                    IOException.class,
                    () -> pdfUploadService.saveMedia(
                            ContentFormat.VIDEO,
                            new ByteArrayInputStream("not-a-real-video".getBytes(StandardCharsets.US_ASCII)),
                            "lesson.mp4",
                            "video/mp4"
                    )
            );
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }

    @Test
    void defaultServiceRejectsUnprocessedPdfInsteadOfCopyingOriginal() {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            assertThrows(
                    IOException.class,
                    () -> pdfUploadService.savePdf(
                            new ByteArrayInputStream("%PDF-1.4\ninvalid-pdf-body".getBytes(StandardCharsets.US_ASCII)),
                            "lesson.pdf",
                            "application/pdf"
                    )
            );
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }

    @Test
    void defaultServiceRejectsUnprocessedAudioInsteadOfCopyingOriginal() {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            assertThrows(
                    IOException.class,
                    () -> pdfUploadService.saveMedia(
                            ContentFormat.AUDIO,
                            new ByteArrayInputStream("not-a-real-audio".getBytes(StandardCharsets.US_ASCII)),
                            "lesson.mp3",
                            "audio/mpeg"
                    )
            );
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }

    @Test
    void defaultServiceProcessesAudioWithBundledMediaProcessor() throws Exception {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                    ContentFormat.AUDIO,
                    new ByteArrayInputStream(wavSilenceBytes()),
                    "silence.wav",
                    "audio/wav"
            );

            assertTrue(uploadedFile.relativePath().endsWith(".m4a"));
            assertEquals("audio/mp4", uploadedFile.contentType());
            assertTrue(Files.size(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())) > 0L);
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }


    @Test
    void rejectsNonPdfPayload() {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfUploadService.savePdf(
                        new ByteArrayInputStream("not a pdf".getBytes(StandardCharsets.US_ASCII)),
                        "lesson.pdf",
                        "application/pdf"
                )
        );
    }

    @Test
    void rejectsEmptyPdfFile() {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L);

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfUploadService.savePdf(
                        new ByteArrayInputStream(new byte[0]),
                        "vazio.pdf",
                        "application/pdf"
                )
        );
    }

    @Test
    void rejectsPdfExceedingConfiguredMaximumSize() {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 4L);

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfUploadService.savePdf(
                        new ByteArrayInputStream(validPdfBytes()),
                        "grande.pdf",
                        "application/pdf"
                )
        );
    }

    @Test
    void savesSupportedVideoUnderContentsDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L * 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                ContentFormat.VIDEO,
                new ByteArrayInputStream(mp4VideoBytes()),
                "lesson.mp4",
                "video/mp4"
        );

        assertTrue(uploadedFile.relativePath().startsWith("contents/"));
        assertTrue(uploadedFile.relativePath().endsWith(".mp4"));
        assertEquals("lesson.mp4", uploadedFile.originalFileName());
        assertEquals("video/mp4", uploadedFile.contentType());
        assertNotNull(uploadedFile.thumbnailRelativePath());
        assertTrue(uploadedFile.thumbnailRelativePath().endsWith("-thumb.webp"));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.thumbnailRelativePath())));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedVideoWithGenericBrowserContentType() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L * 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                ContentFormat.VIDEO,
                new ByteArrayInputStream(mp4VideoBytes()),
                "lesson.mp4",
                "application/octet-stream"
        );

        assertTrue(uploadedFile.relativePath().endsWith(".mp4"));
        assertEquals("video/mp4", uploadedFile.contentType());
        assertNotNull(uploadedFile.thumbnailRelativePath());
        assertTrue(uploadedFile.thumbnailRelativePath().endsWith("-thumb.webp"));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.thumbnailRelativePath())));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void acceptsMkvVideoAndStoresItAsMp4() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L * 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                ContentFormat.VIDEO,
                new ByteArrayInputStream(mkvVideoBytes()),
                "lesson.mkv",
                "video/x-matroska"
        );

        assertEquals("lesson.mkv", uploadedFile.originalFileName());
        assertTrue(uploadedFile.relativePath().endsWith(".mp4"));
        assertEquals("video/mp4", uploadedFile.contentType());
        assertEquals("video/x-matroska", uploadedFile.originalContentType());
        assertNotNull(uploadedFile.thumbnailRelativePath());
        assertTrue(uploadedFile.thumbnailRelativePath().endsWith("-thumb.webp"));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.thumbnailRelativePath())));
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedAudioUnderContentsDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L * 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                ContentFormat.AUDIO,
                new ByteArrayInputStream(wavSilenceBytes()),
                "lesson.mp3",
                "audio/mpeg"
        );

        assertTrue(uploadedFile.relativePath().startsWith("contents/"));
        assertEquals("lesson.mp3", uploadedFile.originalFileName());
        assertTrue(uploadedFile.relativePath().endsWith(".m4a"));
        assertEquals("audio/mp4", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedAudioWithGenericBrowserContentType() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L * 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                ContentFormat.AUDIO,
                new ByteArrayInputStream(wavSilenceBytes()),
                "lesson.mp3",
                "application/octet-stream"
        );

        assertTrue(uploadedFile.relativePath().endsWith(".m4a"));
        assertEquals("audio/mp4", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedTextUnderContentsDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveText(
                new ByteArrayInputStream("textual content".getBytes(StandardCharsets.UTF_8)),
                "lesson.txt",
                "text/plain"
        );

        assertTrue(uploadedFile.relativePath().startsWith("contents/"));
        assertTrue(uploadedFile.relativePath().endsWith(".txt"));
        assertEquals("lesson.txt", uploadedFile.originalFileName());
        assertEquals("text/plain", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedImageAsWebpUnderContentsDirectory() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(
                ContentFormat.IMAGE,
                new ByteArrayInputStream(pngImageBytes()),
                "diagram.png",
                "image/png"
        );

        assertTrue(uploadedFile.relativePath().startsWith("contents/"));
        assertTrue(uploadedFile.relativePath().endsWith(".webp"));
        assertEquals("diagram.png", uploadedFile.originalFileName());
        assertEquals("image/webp", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void savesSupportedImageWithGenericBrowserContentType() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L);

        UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(
                ContentFormat.IMAGE,
                new ByteArrayInputStream(pngImageBytes()),
                "diagram.png",
                "application/octet-stream"
        );

        assertTrue(uploadedFile.relativePath().endsWith(".webp"));
        assertEquals("image/webp", uploadedFile.contentType());
        assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())));
    }

    @Test
    void deletesStoredContentFilesAndEmptyContentDirectories() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L);
        Path storedFile = uploadRoot.resolve("contents/text/77/777.txt");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "content", StandardCharsets.UTF_8);

        pdfUploadService.deleteStoredContentFiles(List.of("contents/text/77/777.txt"));

        assertTrue(Files.notExists(storedFile));
        assertTrue(Files.notExists(uploadRoot.resolve("contents/text/77")));
        assertTrue(Files.notExists(uploadRoot.resolve("contents/text")));
        assertTrue(Files.exists(uploadRoot.resolve("contents")));
    }

    @Test
    void deletesLegacyProcessedContentFilesAndEmptyDirectories() throws Exception {
        PdfUploadService pdfUploadService = new PdfUploadService(uploadRoot, 1024L, 1024L);
        Path storedFile = uploadRoot.resolve("contents/items/777/processed/content.txt");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "content", StandardCharsets.UTF_8);

        pdfUploadService.deleteStoredContentFiles(List.of("contents/items/777/processed/content.txt"));

        assertTrue(Files.notExists(storedFile));
        assertTrue(Files.notExists(uploadRoot.resolve("contents/items/777/processed")));
        assertTrue(Files.notExists(uploadRoot.resolve("contents/items/777")));
        assertTrue(Files.exists(uploadRoot.resolve("contents")));
    }

    @Test
    void defaultServiceProcessesVideoWithBundledMediaProcessor() throws Exception {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                    ContentFormat.VIDEO,
                    new ByteArrayInputStream(mkvVideoBytes()),
                    "sample.mkv",
                    "video/x-matroska"
            );

            assertTrue(uploadedFile.relativePath().endsWith(".mp4"));
            assertEquals("video/mp4", uploadedFile.contentType());
            assertNotNull(uploadedFile.thumbnailRelativePath());
            assertTrue(uploadedFile.thumbnailRelativePath().endsWith("-thumb.webp"));
            assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.thumbnailRelativePath())));
            assertTrue(Files.size(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())) > 0L);
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
        }
    }

    @Test
    void defaultServiceProcessesVideoWhenJvmTempDirectoryIsNotUsable() throws Exception {
        String previousUploadDir = System.getProperty("gape.upload.dir");
        String previousTempDirectory = System.getProperty("java.io.tmpdir");
        Path blockedTemp = Files.createTempFile(uploadRoot, "not-a-directory-", ".tmp");
        System.setProperty("gape.upload.dir", uploadRoot.toString());
        System.setProperty("java.io.tmpdir", blockedTemp.toString());
        try {
            PdfUploadService pdfUploadService = new PdfUploadService();

            UploadedContentFile uploadedFile = pdfUploadService.saveMedia(
                    ContentFormat.VIDEO,
                    new ByteArrayInputStream(mkvVideoBytes()),
                    "sample.mkv",
                    "video/x-matroska"
            );

            assertTrue(uploadedFile.relativePath().endsWith(".mp4"));
            assertEquals("video/mp4", uploadedFile.contentType());
            assertNotNull(uploadedFile.thumbnailRelativePath());
            assertTrue(uploadedFile.thumbnailRelativePath().endsWith("-thumb.webp"));
            assertTrue(Files.exists(pdfUploadService.resolveStoredContentFile(uploadedFile.thumbnailRelativePath())));
            assertTrue(Files.size(pdfUploadService.resolveStoredContentFile(uploadedFile.relativePath())) > 0L);
        } finally {
            if (previousUploadDir == null) {
                System.clearProperty("gape.upload.dir");
            } else {
                System.setProperty("gape.upload.dir", previousUploadDir);
            }
            if (previousTempDirectory == null) {
                System.clearProperty("java.io.tmpdir");
            } else {
                System.setProperty("java.io.tmpdir", previousTempDirectory);
            }
        }
    }

    private static byte[] pngImageBytes() throws Exception {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(40, 120, 200));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static byte[] validPdfBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            document.save(output);
        }
        return output.toByteArray();
    }

    private static byte[] mp4VideoBytes() throws Exception {
        return generatedVideoBytes(".mp4");
    }

    private static byte[] mkvVideoBytes() throws Exception {
        return generatedVideoBytes(".mkv");
    }

    private static byte[] generatedVideoBytes(String extension) throws Exception {
        Path output = Files.createTempFile("gape-upload-video-", extension);
        try {
            boolean generated = runMediaTool(List.of(
                    new DefaultFFMPEGLocator().getExecutablePath(),
                    "-y",
                    "-f", "lavfi",
                    "-i", "color=c=blue:s=32x24:d=0.2",
                    "-f", "lavfi",
                    "-i", "anullsrc=channel_layout=mono:sample_rate=8000",
                    "-shortest",
                    "-t", "0.2",
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    output.toString()
            ));
            if (!generated) {
                throw new IOException("Could not generate video test fixture");
            }
            return Files.readAllBytes(output);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static boolean runMediaTool(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return false;
        }
        return process.exitValue() == 0;
    }

    private static byte[] wavSilenceBytes() throws Exception {
        int sampleRate = 8000;
        int channels = 1;
        int bitsPerSample = 16;
        int durationSeconds = 1;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = sampleRate * durationSeconds * blockAlign;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "RIFF");
        writeLittleEndianInt(output, 36 + dataSize);
        writeAscii(output, "WAVE");
        writeAscii(output, "fmt ");
        writeLittleEndianInt(output, 16);
        writeLittleEndianShort(output, 1);
        writeLittleEndianShort(output, channels);
        writeLittleEndianInt(output, sampleRate);
        writeLittleEndianInt(output, byteRate);
        writeLittleEndianShort(output, blockAlign);
        writeLittleEndianShort(output, bitsPerSample);
        writeAscii(output, "data");
        writeLittleEndianInt(output, dataSize);
        output.write(new byte[dataSize]);
        return output.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
        output.write((value >>> 16) & 0xff);
        output.write((value >>> 24) & 0xff);
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
    }
}
