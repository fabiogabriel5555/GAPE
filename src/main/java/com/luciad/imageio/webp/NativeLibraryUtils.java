package com.luciad.imageio.webp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class NativeLibraryUtils {

    private static final String WEBP_NATIVE_DIR_PROPERTY = "gape.webp.native.dir";

    private NativeLibraryUtils() {
    }

    public static void loadFromJar() {
        NativeLibrary nativeLibrary = nativeLibrary();
        String resourcePath = "/native/" + nativeLibrary.platformDirectory() + "/"
                + nativeLibrary.architectureDirectory() + "/" + nativeLibrary.fileName();

        try (InputStream inputStream = NativeLibraryUtils.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not find WebP native library for "
                        + nativeLibrary.platformDirectory() + " " + nativeLibrary.architectureDirectory()
                        + " in the jar");
            }

            Path nativeDirectory = resolveNativeDirectory();
            Path extractedLibrary = Files.createTempFile(
                    nativeDirectory,
                    "gape-webp-",
                    "-" + nativeLibrary.fileName()
            );
            try {
                Files.copy(inputStream, extractedLibrary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                extractedLibrary.toFile().deleteOnExit();
                System.load(extractedLibrary.toAbsolutePath().toString());
            } catch (IOException exception) {
                Files.deleteIfExists(extractedLibrary);
                throw exception;
            } catch (RuntimeException | Error exception) {
                deleteQuietly(extractedLibrary);
                throw exception;
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not load native WebP library", exception);
        }
    }

    private static Path resolveNativeDirectory() throws IOException {
        String configuredDirectory = System.getProperty(WEBP_NATIVE_DIR_PROPERTY);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            Path nativeDirectory = Path.of(configuredDirectory).toAbsolutePath().normalize();
            Files.createDirectories(nativeDirectory);
            return nativeDirectory;
        }

        List<Path> candidates = List.of(
                Path.of(System.getProperty("user.home", "."), ".gape", "webp-native"),
                Path.of(System.getProperty("java.io.tmpdir", "."), "gape-webp-native"),
                Path.of(System.getProperty("user.dir", "."), "gape-webp-native")
        );
        IOException lastException = null;
        for (Path candidate : candidates) {
            try {
                Path nativeDirectory = candidate.toAbsolutePath().normalize();
                Files.createDirectories(nativeDirectory);
                return nativeDirectory;
            } catch (IOException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new IOException("No WebP native library directory is available") : lastException;
    }

    private static NativeLibrary nativeLibrary() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").contains("64") ? "64" : "32";
        if (osName.contains("win")) {
            return new NativeLibrary("win", architecture, "webp-imageio.dll");
        }
        if (osName.contains("mac")) {
            return new NativeLibrary("mac", architecture, "libwebp-imageio.dylib");
        }
        return new NativeLibrary("linux", architecture, "libwebp-imageio.so");
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The failed load is the error that matters; stale temp files are removed on JVM exit where possible.
        }
    }

    private record NativeLibrary(String platformDirectory, String architectureDirectory, String fileName) {
    }
}
