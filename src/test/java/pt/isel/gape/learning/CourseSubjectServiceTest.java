package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CourseSubjectServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private CourseSubjectService courseSubjectService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            insertSubject(connection, 42L, 10L, "Sistemas Distribuidos");
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        courseSubjectService = new CourseSubjectService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanAssociateSubjectWithCourse() {
        CourseSubjectAssociation association = courseSubjectService.associateSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseSubjectAssociationCommand(
                        30L,
                        42L,
                        2,
                        CurricularTerm.SEMESTER_2,
                        true,
                        CourseSubjectState.ACTIVE
                ),
                "127.0.0.1"
        );

        assertEquals(30L, association.courseId());
        assertEquals(42L, association.subjectId());
        assertEquals(CurricularTerm.SEMESTER_2, association.term());
    }

    @Test
    void duplicateCourseSubjectAssociationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> courseSubjectService.associateSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseSubjectAssociationCommand(
                                30L,
                                40L,
                                3,
                                CurricularTerm.ANNUAL,
                                true,
                                CourseSubjectState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void curricularYearAndTermMustBeProvidedTogether() {
        assertThrows(
                IllegalArgumentException.class,
                () -> courseSubjectService.associateSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseSubjectAssociationCommand(
                                30L,
                                42L,
                                1,
                                null,
                                true,
                                CourseSubjectState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void subjectMustBelongToCourseOrganization() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            insertSubject(connection, 43L, 11L, "Subject Externa");
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> courseSubjectService.associateSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseSubjectAssociationCommand(
                                30L,
                                43L,
                                1,
                                CurricularTerm.SEMESTER_1,
                                true,
                                CourseSubjectState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void nonAdministratorCannotAssociateSubjectWithCourse() {
        assertThrows(
                SecurityException.class,
                () -> courseSubjectService.associateSubject(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        new CourseSubjectAssociationCommand(
                                30L,
                                42L,
                                2,
                                CurricularTerm.SEMESTER_2,
                                true,
                                CourseSubjectState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void lastCourseSubjectAssociationCannotBeDeleted() {
        courseSubjectService.associateSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseSubjectAssociationCommand(
                        30L,
                        42L,
                        2,
                        CurricularTerm.SEMESTER_2,
                        true,
                        CourseSubjectState.ACTIVE
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> courseSubjectService.deleteAssociation(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        30L,
                        42L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void lastCourseSubjectAssociationCannotBeArchived() {
        courseSubjectService.associateSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseSubjectAssociationCommand(
                        30L,
                        42L,
                        2,
                        CurricularTerm.SEMESTER_2,
                        true,
                        CourseSubjectState.ACTIVE
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> courseSubjectService.archiveAssociation(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        30L,
                        42L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void coordinatorLearningCanManageOnlyAssignedSubjectAssociations() {
        assertTrue(courseSubjectService.canManageAssociation(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                30L,
                40L,
                "127.0.0.1"
        ));
        assertFalse(courseSubjectService.canManageAssociation(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                30L,
                41L,
                "127.0.0.1"
        ));
    }

    @Test
    void legacySubjectScopedLearningAdministratorCannotManageSubjectAssociations() throws Exception {
        addAdministrator(101L, "ADM-SUBJECT-ASSOC", "MANAGE_LEARNING", "SUBJECT", 40L);

        assertFalse(courseSubjectService.canManageAssociation(
                101L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                40L,
                "127.0.0.1"
        ));
        assertFalse(courseSubjectService.canManageAssociation(
                101L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                41L,
                "127.0.0.1"
        ));
        assertThrows(
                SecurityException.class,
                () -> courseSubjectService.updateAssociation(
                        101L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseSubjectAssociationCommand(
                                30L,
                                41L,
                                1,
                                CurricularTerm.SEMESTER_1,
                                true,
                                CourseSubjectState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    private static void insertSubject(Connection connection, long subjectId, long organizationId, String name)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO subject (
                    id_subject, id_organization, name, acronym, description, ects, workload_hours, state
                ) VALUES (?, ?, ?, ?, NULL, 6.00, 70, 'active')
                """)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, organizationId);
            statement.setString(3, name);
            statement.setString(4, "S" + subjectId);
            statement.executeUpdate();
        }
    }

    private void addAdministrator(
            long userId,
            String administratorCode,
            String permissionCode,
            String contextType,
            long contextId
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Scoped Association Admin");
                user.setString(3, "scoped.association.admin@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, administratorCode);
                profile.executeUpdate();
            }
            try (PreparedStatement grant = connection.prepareStatement("""
                    INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                    VALUES (?, ?, ?, ?)
                    """)) {
                grant.setLong(1, userId);
                grant.setString(2, permissionCode);
                grant.setString(3, contextType);
                grant.setLong(4, contextId);
                grant.executeUpdate();
            }
        }
    }
}
