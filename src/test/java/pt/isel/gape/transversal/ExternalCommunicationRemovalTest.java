package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ExternalCommunicationRemovalTest {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Set<String> CLEANUP_ARTIFACTS = Set.of(
            "src/main/resources/sql/migration/V006__remove_external_delivery_artifacts.sql",
            "src/main/java/pt/isel/gape/common/config/DatabaseMigrationService.java",
            "src/test/java/pt/isel/gape/common/config/DatabaseMigrationServiceTest.java",
            "src/test/java/pt/isel/gape/transversal/ExternalCommunicationRemovalTest.java"
    );
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "jakarta.mail",
            "org.eclipse.angus",
            "smtp",
            "emailadapter",
            "emailmessage",
            "deliverymode",
            "delivery_mode",
            "gape.email",
            "notification_email_send"
    );

    @Test
    void externalDeliveryArtifactsOnlyExistInVersionedCleanupCode() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path file : projectTextFiles()) {
            String relative = relativePath(file);
            if (CLEANUP_ARTIFACTS.contains(relative)) {
                continue;
            }
            String content = Files.readString(file).toLowerCase(Locale.ROOT);
            for (String token : FORBIDDEN_TOKENS) {
                if (content.contains(token)) {
                    violations.add(relative + " contains " + token);
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("External delivery artifacts found:\n" + String.join("\n", violations));
        }
    }

    @Test
    void internalReceiptStatesDoNotExposeExternalFailureState() throws Exception {
        String model = Files.readString(ROOT.resolve(
                "src/main/java/pt/isel/gape/transversal/model/MessageReceiptState.java"
        ));
        assertFalse(model.contains("FAILED"));

        String schema = Files.readString(ROOT.resolve("src/main/resources/sql/schema.sql"));
        int receiptTableStart = schema.indexOf("CREATE TABLE IF NOT EXISTS receive_message");
        int receiptTableEnd = schema.indexOf("CREATE TABLE IF NOT EXISTS", receiptTableStart + 1);
        String receiptTable = schema.substring(receiptTableStart, receiptTableEnd);
        assertFalse(receiptTable.contains("'failed'"));
    }

    private static List<Path> projectTextFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : List.of(ROOT.resolve("src/main"), ROOT.resolve("src/test"), ROOT.resolve("docs"))) {
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(ExternalCommunicationRemovalTest::isTextFile)
                        .filter(path -> !relativePath(path).startsWith("docs/tests/temps/"))
                        .forEach(files::add);
            }
        }
        for (String rootFile : List.of("pom.xml", "README.md", "compose.yaml", "Dockerfile")) {
            Path path = ROOT.resolve(rootFile);
            if (Files.isRegularFile(path)) {
                files.add(path);
            }
        }
        return files;
    }

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".java")
                || name.endsWith(".sql")
                || name.endsWith(".xml")
                || name.endsWith(".properties")
                || name.endsWith(".jsp")
                || name.endsWith(".jspf")
                || name.endsWith(".md")
                || name.endsWith(".txt")
                || name.endsWith(".yml")
                || name.endsWith(".yaml");
    }

    private static String relativePath(Path path) {
        return ROOT.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
