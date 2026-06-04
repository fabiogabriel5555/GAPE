package pt.isel.gape.common.config;

import java.util.List;
import java.util.Locale;

public enum DatabaseBootstrapMode {
    NONE(List.of()),
    SCHEMA(List.of()),
    DEMO(List.of("sql/seed/base.sql")),
    FULL(List.of("sql/seed/base.sql", "sql/seed/full.sql"));

    private final List<String> seedResources;

    DatabaseBootstrapMode(List<String> seedResources) {
        this.seedResources = seedResources;
    }

    List<String> seedResources() {
        return seedResources;
    }

    boolean shouldBootstrap() {
        return this != NONE;
    }

    public static DatabaseBootstrapMode fromProperty(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NONE;
        }

        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "none" -> NONE;
            case "schema" -> SCHEMA;
            case "demo" -> DEMO;
            case "test", "full" -> FULL;
            default -> throw new IllegalStateException(
                    "Invalid db.bootstrap.mode value: " + rawValue + ". Expected one of: none, schema, demo, full"
            );
        };
    }
}
