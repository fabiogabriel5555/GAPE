package pt.isel.gape.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DatabaseTemporalConventionTest {

    @Test
    void schemaDoesNotOverrideTheLisbonSessionWithFixedUtc() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/sql/schema.sql"));

        assertTrue(schema.contains("Europe/Lisbon"));
        assertFalse(schema.contains("SET time_zone = '+00:00'"));
    }

    @Test
    void seedDatesRemainIsoTechnicalValues() throws IOException {
        String baseSeed = Files.readString(Path.of("src/main/resources/sql/seed/base.sql"));

        assertTrue(baseSeed.contains("'2026-01-01'"));
        assertFalse(baseSeed.contains("'01-01-2026'"));
    }
}
