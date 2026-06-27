package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseCreateCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.CourseUpdateCommand;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CourseServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private CourseService courseService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        courseService = new CourseService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanCreateValidCourseInManagedOrganization() {
        Course course = courseService.createCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validCreateCommand("Sistemas de Informacao", "SI"),
                "127.0.0.1"
        );

        assertTrue(course.id() > 0);
        assertEquals(10L, course.organizationId());
        assertEquals(20L, course.organicUnitId());
        assertEquals(CourseState.ACTIVE, course.state());
    }

    @Test
    void administratorCanAttachCoursePhoto() {
        Course course = courseService.createCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validCreateCommand("Course With Photo", "CF"),
                "127.0.0.1"
        );

        String photo = "courses/" + course.id() + "/profile.webp";
        Course updated = courseService.attachCreatedCoursePhoto(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                course.id(),
                photo,
                "127.0.0.1"
        );

        assertEquals(photo, updated.photo());
        Course reloaded = courseService.getCourse(1L, null, AccessProfileType.ADMINISTRATOR, course.id(), "127.0.0.1");
        assertEquals(photo, reloaded.photo());
    }

    @Test
    void unsafeCoursePhotoPathIsRejected() {
        Course course = courseService.createCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validCreateCommand("Course Invalid Photo", "CFI"),
                "127.0.0.1"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> courseService.attachCreatedCoursePhoto(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        course.id(),
                        "javascript:alert(1)",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void courseWithoutOrganizationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseCreateCommand(
                                0L,
                                null,
                                "Course Invalid",
                                "INV",
                                null,
                                null,
                                BigDecimal.valueOf(30),
                                "1",
                                CourseType.OTHER,
                                CourseState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void courseOrganicUnitMustBelongToSameOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseCreateCommand(
                                10L,
                                21L,
                                "Course Mixed",
                                "MIX",
                                null,
                                null,
                                BigDecimal.valueOf(30),
                                "1",
                                CourseType.SHORT_COURSE,
                                CourseState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void nonAdministratorCannotCreateCourse() {
        assertThrows(
                SecurityException.class,
                () -> courseService.createCourse(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        validCreateCommand("Course Student", "CE"),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void courseDurationMustBeNumericYears() {
        assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseCreateCommand(
                                10L,
                                20L,
                                "Course With Invalid Duration",
                                "CDI",
                                null,
                                null,
                                BigDecimal.valueOf(30),
                                "2u",
                                CourseType.SHORT_COURSE,
                                CourseState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void archivedCourseCanBeUnarchivedByAdministrator() {
        courseService.archiveCourse(1L, null, AccessProfileType.ADMINISTRATOR, 31L, "127.0.0.1");
        courseService.unarchiveCourse(1L, null, AccessProfileType.ADMINISTRATOR, 31L, "127.0.0.1");

        Course course = courseService.getCourse(1L, null, AccessProfileType.ADMINISTRATOR, 31L, "127.0.0.1");

        assertEquals(CourseState.ACTIVE, course.state());
    }

    @Test
    void archivedCourseBlocksFurtherOperations() {
        courseService.archiveCourse(1L, null, AccessProfileType.ADMINISTRATOR, 31L, "127.0.0.1");

        assertThrows(
                IllegalStateException.class,
                () -> courseService.updateCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        31L,
                        new CourseUpdateCommand(
                                10L,
                                20L,
                                "Analise de Dados Arquivada",
                                "AD",
                                null,
                                "Nao editavel",
                                BigDecimal.valueOf(60),
                                "1",
                                CourseType.SHORT_COURSE,
                                CourseState.INACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void courseWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        30L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void organizationStructureAdministratorDoesNotListCourses() throws Exception {
        addAdministrator(100L, "ADM-COURSE-UNIT", "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L);

        Set<Long> courseIds = courseService.listCourses(
                        100L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        10L,
                        "127.0.0.1"
                )
                .stream()
                .map(Course::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(), courseIds);
    }

    @Test
    void coordinatorLearningListsOnlyCoursesContainingAssignedSubjects() {
        Set<Long> courseIds = courseService.listCourses(
                        2L,
                        null,
                        AccessProfileType.COORDINATOR,
                        10L,
                        "127.0.0.1"
                )
                .stream()
                .map(Course::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(30L), courseIds);
        assertEquals(30L, courseService.getCourse(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                30L,
                "127.0.0.1"
        ).id());
        assertThrows(SecurityException.class, () -> courseService.getCourse(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                31L,
                "127.0.0.1"
        ));
        assertFalse(courseService.canManageCourseChildren(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                30L,
                "127.0.0.1"
        ));
    }

    @Test
    void legacyLearningAdministratorDoesNotListAssociatedCourses() throws Exception {
        addAdministrator(101L, "ADM-COURSE-SUBJECT", "MANAGE_LEARNING", "SUBJECT", 40L);

        Set<Long> courseIds = courseService.listCourses(
                        101L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        10L,
                        "127.0.0.1"
                )
                .stream()
                .map(Course::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(), courseIds);
    }

    @Test
    void legacyCourseScopedLearningAdministratorCannotManageCourseChildren() throws Exception {
        addAdministrator(102L, "ADM-COURSE-EXACT", "MANAGE_LEARNING", "COURSE", 30L);

        assertFalse(courseService.canModifyCourse(
                102L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                "127.0.0.1"
        ));
        assertFalse(courseService.canManageCourseChildren(
                102L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                "127.0.0.1"
        ));
    }

    @Test
    void subjectScopedAdministratorCannotModifyAssociatedCourse() throws Exception {
        addAdministrator(103L, "ADM-COURSE-SUBJECT-READ", "MANAGE_LEARNING", "SUBJECT", 40L);

        assertFalse(courseService.canModifyCourse(
                103L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                "127.0.0.1"
        ));
    }

    private static CourseCreateCommand validCreateCommand(String name, String acronym) {
        return new CourseCreateCommand(
                10L,
                20L,
                name,
                acronym,
                null,
                "Course test",
                BigDecimal.valueOf(60),
                "1",
                CourseType.SHORT_COURSE,
                CourseState.ACTIVE
        );
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
                user.setString(2, "Scoped Course Admin");
                user.setString(3, "scoped.course.admin@gape.local");
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
