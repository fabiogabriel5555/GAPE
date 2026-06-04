package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

class ValidDataInsertTest {

    @Test
    void shouldInsertValidDatasetAcrossAllAreas() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") >= 4);
            assertTrue(DatabaseTestSupport.countRows(connection, "organization") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "class_group") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "content_item") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "assessment") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "schedule_event") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "attendance_record") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "absence_justification") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "grade_record") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "certificate") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "message") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "activity_log") >= 1);
        }
    }

    @Test
    void shouldAllowSameUserToAccumulateMultipleProfiles() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            DatabaseTestSupport.resetDatabase(connection);

            statement.execute("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, created_at, credential_hash, credential_salt
                    ) VALUES (
                        9500, 'Utilizador MultiPerfil', 'multi-profile@gape.local',
                        'active', 'pt-PT', '2026-04-01 10:00:00', 'h_multi', 's_multi'
                    )
                    """);
            statement.execute("""
                    INSERT INTO coordinator_profile (id_user, cod_coordinator)
                    VALUES (9500, 'COO-9500')
                    """);
            statement.execute("""
                    INSERT INTO teacher_profile (id_user, cod_teacher)
                    VALUES (9500, 'TCH-9500')
                    """);

            assertTrue(DatabaseTestSupport.countRows(connection, "coordinator_profile") == 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "teacher_profile") == 1);
        }
    }
}
