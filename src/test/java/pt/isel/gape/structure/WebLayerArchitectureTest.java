package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class WebLayerArchitectureTest {

    private static final Path WEB_SOURCE_DIRECTORY = Path.of("src/main/java/pt/isel/gape/web");
    private static final Pattern DAO_IMPORT = Pattern.compile("(?m)^\\s*import\\s+pt\\.isel\\.gape\\..*\\.dao\\..*;");
    private static final Pattern DAO_CONSTRUCTION = Pattern.compile("\\bnew\\s+(?:[A-Za-z0-9_$.]*DAO)\\s*\\(");
    private static final Pattern JDBC_CONNECTION_ACCESS = Pattern.compile("\\.getConnection\\s*\\(");

    @Test
    void webLayerUsesServicesInsteadOfConcreteDaosOrJdbcConnections() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> sourceFiles = Files.walk(WEB_SOURCE_DIRECTORY)) {
            sourceFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertTrue(violations.isEmpty(), () -> "Web-layer architecture violations: " + violations);
    }

    private static void collectViolations(Path sourceFile, List<String> violations) {
        try {
            String source = Files.readString(sourceFile);
            if (DAO_IMPORT.matcher(source).find()) {
                violations.add(sourceFile + " imports a DAO");
            }
            if (DAO_CONSTRUCTION.matcher(source).find()) {
                violations.add(sourceFile + " constructs a DAO");
            }
            if (JDBC_CONNECTION_ACCESS.matcher(source).find()) {
                violations.add(sourceFile + " opens a JDBC connection");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect " + sourceFile, exception);
        }
    }
}
