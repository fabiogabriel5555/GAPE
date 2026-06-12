package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
                validCreateCommand("Curso com Foto", "CF"),
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
                validCreateCommand("Curso Foto Invalida", "CFI"),
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
                                "Curso Invalido",
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
                                "Curso Misto",
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
                        validCreateCommand("Curso Estudante", "CE"),
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
                                "Curso com Duracao Invalida",
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

    private static CourseCreateCommand validCreateCommand(String name, String acronym) {
        return new CourseCreateCommand(
                10L,
                20L,
                name,
                acronym,
                null,
                "Curso de teste",
                BigDecimal.valueOf(60),
                "1",
                CourseType.SHORT_COURSE,
                CourseState.ACTIVE
        );
    }
}
