package pt.isel.gape.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatabaseMigrationManifestTest {

    @Test
    void manifestStartsWithImmutableBaselineAndHasOrderedChecksummedResources() throws Exception {
        List<DatabaseMigrationService.MigrationDefinition> migrations =
                DatabaseMigrationService.loadMigrations();

        assertFalse(migrations.isEmpty());
        assertEquals(1L, migrations.get(0).version());

        long previousVersion = 0;
        for (DatabaseMigrationService.MigrationDefinition migration : migrations) {
            assertTrue(migration.version() > previousVersion);
            assertTrue(migration.resource().matches("sql/migration/V\\d{3,}__[A-Za-z0-9_]+\\.sql"));
            assertTrue(migration.checksum().matches("[0-9a-f]{64}"));
            previousVersion = migration.version();
        }
    }
}
