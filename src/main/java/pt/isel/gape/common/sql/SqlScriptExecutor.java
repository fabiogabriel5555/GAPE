package pt.isel.gape.common.sql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqlScriptExecutor {

    private SqlScriptExecutor() {
    }

    public static void executeResource(Connection connection, String resourcePath) throws SQLException, IOException {
        String script = readResource(resourcePath);
        executeStatements(connection, parseStatements(script));
    }

    public static List<String> parseStatements(String rawScript) {
        String normalized = rawScript.replace("\r\n", "\n").replace('\r', '\n');
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String delimiter = ";";

        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.regionMatches(true, 0, "DELIMITER ", 0, "DELIMITER ".length())) {
                delimiter = trimmed.substring("DELIMITER ".length()).trim();
                continue;
            }

            current.append(line).append('\n');

            if (!trimmed.endsWith(delimiter)) {
                continue;
            }

            String sql = current.toString().trim();
            sql = sql.substring(0, sql.length() - delimiter.length()).trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
            current.setLength(0);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            if (tail.endsWith(delimiter)) {
                tail = tail.substring(0, tail.length() - delimiter.length()).trim();
            }
            if (!tail.isEmpty()) {
                statements.add(tail);
            }
        }

        return statements;
    }

    private static void executeStatements(Connection connection, List<String> statements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static String readResource(String resourcePath) throws IOException {
        try (InputStream input = SqlScriptExecutor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing SQL resource: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
