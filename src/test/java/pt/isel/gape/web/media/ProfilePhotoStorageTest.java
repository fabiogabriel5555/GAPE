package pt.isel.gape.web.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

class ProfilePhotoStorageTest {

    @Test
    void storesOrganizationPhotoInOrganizationDirectory() throws Exception {
        Path testRoot = Files.createDirectories(Path.of("target", "profile-photo-storage-test"));
        Path uploadRoot = Files.createTempDirectory(testRoot, "uploads-");
        try {
            ProfilePhotoStorage storage = new ProfilePhotoStorage(uploadRoot.toAbsolutePath().toString());

            String storedPath = storage.saveOrganizationPhoto(42L, new ImagePart(pngImageBytes()), null);

            assertEquals("organizations/42/profile.webp", storedPath);
            Path storedFile = uploadRoot.resolve("organizations/42/profile.webp");
            assertTrue(Files.exists(storedFile));
            assertTrue(Files.size(storedFile) > 0);
            assertWebpFile(storedFile);
        } finally {
            deleteDirectory(uploadRoot);
        }
    }

    @Test
    void convertsJpegProfilePhotoToWebp() throws Exception {
        Path testRoot = Files.createDirectories(Path.of("target", "profile-photo-storage-test"));
        Path uploadRoot = Files.createTempDirectory(testRoot, "uploads-");
        try {
            ProfilePhotoStorage storage = new ProfilePhotoStorage(uploadRoot.toAbsolutePath().toString());

            String storedPath = storage.saveProfilePhoto(7L, new ImagePart(jpegImageBytes(), "image/jpeg", "profile.jpg"), null);

            assertEquals("users/7/profile.webp", storedPath);
            Path storedFile = uploadRoot.resolve("users/7/profile.webp");
            assertTrue(Files.exists(storedFile));
            assertTrue(Files.size(storedFile) > 0);
            assertWebpFile(storedFile);
        } finally {
            deleteDirectory(uploadRoot);
        }
    }

    @Test
    void storesRelativeUploadDirectoryUnderWebappRootWhenDirectoryDoesNotExist() throws Exception {
        Path webappRoot = Files.createTempDirectory("profile-photo-storage-webapp-");
        try {
            ProfilePhotoStorage storage = new ProfilePhotoStorage("test-uploads-does-not-exist");

            String storedPath = storage.saveOrganizationPhoto(43L, new ImagePart(pngImageBytes()), servletContext(webappRoot));

            assertEquals("organizations/43/profile.webp", storedPath);
            Path storedFile = webappRoot.resolve("test-uploads-does-not-exist/organizations/43/profile.webp");
            assertTrue(Files.exists(storedFile));
            assertTrue(Files.size(storedFile) > 0);
            assertWebpFile(storedFile);
        } finally {
            deleteDirectory(webappRoot);
        }
    }

    private static byte[] pngImageBytes() throws IOException {
        return imageBytes("png");
    }

    private static byte[] jpegImageBytes() throws IOException {
        return imageBytes("jpg");
    }

    private static byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(12, 96, 160));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static void assertWebpFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        assertTrue(bytes.length > 12);
        assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WEBP", new String(bytes, 8, 4, StandardCharsets.US_ASCII));
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (directory != null && Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to delete " + path, exception);
                            }
                        });
            }
        }
    }

    private static ServletContext servletContext(Path webappRoot) {
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRealPath" -> webappRoot.toString();
                    default -> null;
                }
        );
    }

    private record ImagePart(byte[] bytes, String contentType, String submittedFileName) implements Part {

        private ImagePart(byte[] bytes) {
            this(bytes, "image/png", "organization.png");
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "organizationImage";
        }

        @Override
        public String getSubmittedFileName() {
            return submittedFileName;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public void write(String fileName) {
        }

        @Override
        public void delete() {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.of();
        }
    }
}
