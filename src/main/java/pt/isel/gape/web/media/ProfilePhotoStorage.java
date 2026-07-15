package pt.isel.gape.web.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.common.storage.UploadRootResolver;

public final class ProfilePhotoStorage {

    private static final int PROFILE_SIZE = 200;
    private static final int ENTITY_IMAGE_SIZE = 800;
    private static final long DEFAULT_MAX_IMAGE_BYTES = 50L * 1024L * 1024L;
    private static final String UPLOAD_DIR_PROPERTY = "gape.upload.dir";
    private static final String MAX_IMAGE_BYTES_PROPERTY = "gape.media.image.max-bytes";
    private static final String WEBP_NATIVE_DIR_PROPERTY = "gape.webp.native.dir";
    private static final String STORED_PROFILE_FILE_NAME = "profile.webp";
    private static final List<String> PROFILE_IMAGE_FILE_NAMES = List.of(
            "profile.webp",
            "profile.png",
            "profile.jpg",
            "profile.jpeg",
            "profile.gif"
    );
    private static final Object IMAGE_IO_CONFIGURATION_LOCK = new Object();
    private static boolean imageIoPluginsScanned;

    private final String configuredUploadDir;
    private final long maxImageBytes;

    public ProfilePhotoStorage() {
        this(UploadRootResolver.configuredUploadDirectory(UPLOAD_DIR_PROPERTY, "uploads"));
    }

    ProfilePhotoStorage(String configuredUploadDir) {
        this(configuredUploadDir, configuredMaxImageBytes());
    }

    ProfilePhotoStorage(String configuredUploadDir, long maxImageBytes) {
        if (maxImageBytes <= 0L) {
            throw new IllegalArgumentException("max image size must be positive");
        }
        this.configuredUploadDir = configuredUploadDir;
        this.maxImageBytes = maxImageBytes;
    }

    public String saveProfilePhoto(long userId, Part imagePart, ServletContext servletContext) throws IOException {
        return savePhoto("users", userId, imagePart, servletContext, PROFILE_SIZE);
    }

    public String saveOrganizationPhoto(long organizationId, Part imagePart, ServletContext servletContext) throws IOException {
        return savePhoto("organizations", organizationId, imagePart, servletContext, ENTITY_IMAGE_SIZE);
    }

    public String saveCoursePhoto(long courseId, Part imagePart, ServletContext servletContext) throws IOException {
        return savePhoto("courses", courseId, imagePart, servletContext, ENTITY_IMAGE_SIZE);
    }

    public String saveSubjectPhoto(long subjectId, Part imagePart, ServletContext servletContext) throws IOException {
        return savePhoto("subjects", subjectId, imagePart, servletContext, ENTITY_IMAGE_SIZE);
    }

    private String savePhoto(
            String entityDirectoryName,
            long entityId,
            Part imagePart,
            ServletContext servletContext,
            int targetSize
    )
            throws IOException {
        if (imagePart == null || imagePart.getSize() <= 0) {
            return null;
        }
        if (imagePart.getSize() > maxImageBytes) {
            throw new IllegalArgumentException("The uploaded image exceeds the maximum allowed size.");
        }

        Path uploadRoot = resolveUploadRoot(servletContext);
        configureImageIoForUploadRoot(uploadRoot);
        BufferedImage uploadedImage = readUploadedImage(imagePart);
        if (uploadedImage == null) {
            throw new IllegalArgumentException("The uploaded file is not a supported image.");
        }

        BufferedImage resizedImage = resizeToSquare(uploadedImage, targetSize);
        Path entityDirectory = uploadRoot.resolve(entityDirectoryName).resolve(Long.toString(entityId)).normalize();
        if (!entityDirectory.startsWith(uploadRoot)) {
            throw new IOException("Invalid upload target");
        }

        Files.createDirectories(entityDirectory);
        deleteExistingProfileImages(entityDirectory);
        Path target = entityDirectory.resolve(STORED_PROFILE_FILE_NAME);
        boolean written = ImageIO.write(resizedImage, "webp", target.toFile());
        if (!written) {
            throw new IllegalStateException("WebP image writer is not available.");
        }
        return entityDirectoryName + "/" + entityId + "/" + STORED_PROFILE_FILE_NAME;
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

    private static BufferedImage readUploadedImage(Part imagePart) throws IOException {
        try (InputStream inputStream = imagePart.getInputStream()) {
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

    private static void deleteExistingProfileImages(Path entityDirectory) throws IOException {
        for (String fileName : PROFILE_IMAGE_FILE_NAMES) {
            Files.deleteIfExists(entityDirectory.resolve(fileName));
        }
    }

    private static BufferedImage resizeToSquare(BufferedImage source, int targetSize) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int squareSize = Math.min(sourceWidth, sourceHeight);
        int cropX = (sourceWidth - squareSize) / 2;
        int cropY = (sourceHeight - squareSize) / 2;

        BufferedImage target = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(
                source,
                0,
                0,
                targetSize,
                targetSize,
                    cropX,
                    cropY,
                    cropX + squareSize,
                    cropY + squareSize,
                    null
            );
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static long configuredMaxImageBytes() {
        String configuredValue = DatabaseConfig.getProperty(MAX_IMAGE_BYTES_PROPERTY, Long.toString(DEFAULT_MAX_IMAGE_BYTES));
        try {
            return Long.parseLong(configuredValue);
        } catch (NumberFormatException exception) {
            return DEFAULT_MAX_IMAGE_BYTES;
        }
    }

    private Path resolveUploadRoot(ServletContext servletContext) {
        String realPath = servletContext == null ? null : servletContext.getRealPath("/");
        return UploadRootResolver.resolve(configuredUploadDir, realPath);
    }
}
