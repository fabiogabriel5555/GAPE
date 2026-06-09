package pt.isel.gape.web.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import pt.isel.gape.common.config.DatabaseConfig;

public final class ProfilePhotoStorage {

    private static final int PROFILE_SIZE = 200;
    private static final String UPLOAD_DIR_PROPERTY = "gape.upload.dir";

    private final String configuredUploadDir;

    public ProfilePhotoStorage() {
        this(DatabaseConfig.getProperty(UPLOAD_DIR_PROPERTY, "uploads"));
    }

    ProfilePhotoStorage(String configuredUploadDir) {
        this.configuredUploadDir = configuredUploadDir;
    }

    public String saveProfilePhoto(long userId, Part imagePart, ServletContext servletContext) throws IOException {
        if (imagePart == null || imagePart.getSize() <= 0) {
            return null;
        }

        BufferedImage uploadedImage = ImageIO.read(imagePart.getInputStream());
        if (uploadedImage == null) {
            throw new IllegalArgumentException("The uploaded file is not a supported image.");
        }

        BufferedImage resizedImage = resizeToSquare(uploadedImage);
        Path uploadRoot = resolveUploadRoot(servletContext);
        Path userDirectory = uploadRoot.resolve("users").resolve(Long.toString(userId)).normalize();
        if (!userDirectory.startsWith(uploadRoot)) {
            throw new IOException("Invalid upload target");
        }

        Files.createDirectories(userDirectory);
        Path target = userDirectory.resolve("profile.webp");
        ImageIO.scanForPlugins();
        boolean written = ImageIO.write(resizedImage, "webp", target.toFile());
        if (!written) {
            throw new IllegalStateException("WebP image writer is not available.");
        }
        return "users/" + userId + "/profile.webp";
    }

    private static BufferedImage resizeToSquare(BufferedImage source) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int squareSize = Math.min(sourceWidth, sourceHeight);
        int cropX = (sourceWidth - squareSize) / 2;
        int cropY = (sourceHeight - squareSize) / 2;

        BufferedImage target = new BufferedImage(PROFILE_SIZE, PROFILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(
                    source,
                    0,
                    0,
                    PROFILE_SIZE,
                    PROFILE_SIZE,
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

    private Path resolveUploadRoot(ServletContext servletContext) {
        Path configuredPath = Path.of(configuredUploadDir);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(System.getProperty("user.dir")).resolve(configuredPath));

        String realPath = servletContext.getRealPath("/");
        if (realPath != null && !realPath.isBlank()) {
            Path current = Path.of(realPath).toAbsolutePath().normalize();
            for (int depth = 0; depth < 8 && current != null; depth++) {
                candidates.add(current.resolve(configuredPath));
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
