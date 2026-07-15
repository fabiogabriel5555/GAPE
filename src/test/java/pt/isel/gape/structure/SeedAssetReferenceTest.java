package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SeedAssetReferenceTest {

    private static final Path SEED_DIRECTORY = Path.of("src", "main", "resources", "sql", "seed");
    private static final Path UPLOAD_DIRECTORY = Path.of("uploads");
    private static final Pattern STORED_ASSET = Pattern.compile(
            "'/?((?:users|organizations|courses|subjects|contents|messages|justifications|attempts|submissions|uploads)/[^']+\\.(?:webp|png|jpe?g|pdf|txt|m4a|mp3|mp4|zip))'",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void everySeedStoragePathHasAValidVersionedFixture() throws IOException {
        List<String> failures = new ArrayList<>();

        try (var seeds = Files.list(SEED_DIRECTORY)) {
            for (Path seed : seeds.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
                Matcher matcher = STORED_ASSET.matcher(Files.readString(seed, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String relativePath = matcher.group(1).replace('/', java.io.File.separatorChar);
                    Path fixture = UPLOAD_DIRECTORY.resolve(relativePath).normalize();
                    if (!fixture.startsWith(UPLOAD_DIRECTORY)) {
                        failures.add(relativePath + " escapes the upload directory");
                    } else if (!Files.isRegularFile(fixture)) {
                        failures.add(relativePath + " is missing");
                    } else if (!hasExpectedSignature(fixture)) {
                        failures.add(relativePath + " does not match its file extension");
                    }
                }
            }
        }

        if (!failures.isEmpty()) {
            fail("Invalid seed upload fixtures:\n - " + String.join("\n - ", failures));
        }
    }

    private boolean hasExpectedSignature(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        assertTrue(bytes.length > 0, () -> "Seed fixture is empty: " + path);
        String extension = extension(path);
        return switch (extension) {
            case "pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "png" -> startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case "jpg", "jpeg" -> startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
            case "webp" -> bytes.length >= 12
                    && ascii(bytes, 0, 4).equals("RIFF")
                    && ascii(bytes, 8, 4).equals("WEBP");
            case "zip" -> bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4b;
            case "m4a", "mp4" -> bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp");
            case "mp3" -> startsWith(bytes, "ID3".getBytes(StandardCharsets.US_ASCII))
                    || (bytes.length >= 2 && bytes[0] == (byte) 0xff && (bytes[1] & 0xe0) == 0xe0);
            case "txt" -> true;
            default -> false;
        };
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] value, int offset, int length) {
        return new String(value, offset, length, StandardCharsets.US_ASCII);
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);
    }
}
