package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectInitialCourseAssignment;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class SubjectServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private SubjectService subjectService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        subjectService = new SubjectService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanCreateSubjectAndAssignCoordinator() throws Exception {
        Subject subject = subjectService.createSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new SubjectCreateCommand(
                        10L,
                        "Arquitetura de Software",
                        "ASW",
                        null,
                        "Disciplina de arquitetura",
                        BigDecimal.valueOf(6),
                        70,
                        SubjectState.ACTIVE,
                        30L,
                        3,
                        CurricularTerm.ANNUAL,
                        true,
                        Set.of(2L)
                ),
                "127.0.0.1"
        );

        assertTrue(subject.id() > 0);
        assertEquals(10L, subject.organizationId());
        assertTrue(hasCourseSubject(30L, subject.id()));
        assertTrue(hasActiveCoordinator(2L, subject.id()));
    }

    @Test
    void administratorCanCreateSubjectAssociatedWithMultipleCourses() throws Exception {
        Subject subject = subjectService.createSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new SubjectCreateCommand(
                        10L,
                        "Sistemas Distribuidos",
                        "SD",
                        null,
                        "Disciplina associada a varios cursos",
                        BigDecimal.valueOf(6),
                        70,
                        SubjectState.ACTIVE,
                        30L,
                        2,
                        CurricularTerm.SEMESTER_1,
                        true,
                        Set.of(),
                        List.of(
                                new SubjectInitialCourseAssignment(30L, 2, CurricularTerm.SEMESTER_1, true),
                                new SubjectInitialCourseAssignment(31L, 1, CurricularTerm.SEMESTER_1, true)
                        )
                ),
                "127.0.0.1"
        );

        assertTrue(hasCourseSubject(30L, subject.id()));
        assertTrue(hasCourseSubject(31L, subject.id()));
    }

    @Test
    void administratorCanAttachSubjectPhoto() {
        Subject subject = subjectService.createSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new SubjectCreateCommand(
                        10L,
                        "Disciplina com Foto",
                        "DCF",
                        null,
                        "Disciplina com imagem propria",
                        BigDecimal.valueOf(6),
                        70,
                        SubjectState.ACTIVE,
                        30L,
                        null,
                        null,
                        true,
                        Set.of()
                ),
                "127.0.0.1"
        );

        String photo = "subjects/" + subject.id() + "/profile.webp";
        Subject updated = subjectService.attachCreatedSubjectPhoto(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                subject.id(),
                photo,
                "127.0.0.1"
        );

        assertEquals(photo, updated.photo());
        Subject reloaded = subjectService.getSubject(1L, null, AccessProfileType.ADMINISTRATOR, subject.id(), "127.0.0.1");
        assertEquals(photo, reloaded.photo());
    }

    @Test
    void unsafeSubjectPhotoPathIsRejected() {
        Subject subject = subjectService.createSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new SubjectCreateCommand(
                        10L,
                        "Disciplina Foto Invalida",
                        "DFI",
                        null,
                        "Disciplina para teste de imagem",
                        BigDecimal.valueOf(6),
                        70,
                        SubjectState.ACTIVE,
                        30L,
                        null,
                        null,
                        true,
                        Set.of()
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> subjectService.attachCreatedSubjectPhoto(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        subject.id(),
                        "\" onerror=\"alert(1)",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanCreateInactiveSubjectWithInitialCourse() throws Exception {
        Subject subject = subjectService.createSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new SubjectCreateCommand(
                        10L,
                        "Disciplina Inativa",
                        "DIN",
                        null,
                        "Criada inativa mas associada a curso",
                        BigDecimal.valueOf(6),
                        70,
                        SubjectState.INACTIVE,
                        30L,
                        null,
                        null,
                        true,
                        Set.of()
                ),
                "127.0.0.1"
        );

        assertEquals(SubjectState.INACTIVE, subject.state());
        assertTrue(hasCourseSubject(30L, subject.id()));
    }

    @Test
    void subjectRequiresOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> subjectService.createSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectCreateCommand(
                                0L,
                                "Disciplina Invalida",
                                "INV",
                                null,
                                null,
                                BigDecimal.valueOf(6),
                                30,
                                SubjectState.ACTIVE,
                                30L,
                                null,
                                null,
                                true,
                                Set.of()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void subjectRequiresInitialCourse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> subjectService.createSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectCreateCommand(
                                10L,
                                "Disciplina Sem Curso",
                                "DSC",
                                null,
                                null,
                                BigDecimal.valueOf(6),
                                30,
                                SubjectState.ACTIVE,
                                0L,
                                null,
                                null,
                                true,
                                Set.of()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void coordinatorCanUpdateAssignedSubject() {
        Subject updated = subjectService.updateSubject(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                40L,
                new SubjectUpdateCommand(
                        "Projeto Aplicado",
                        "PRJ",
                        null,
                        "Atualizada pelo coordenador",
                        BigDecimal.valueOf(12),
                        140,
                        SubjectState.ACTIVE
                ),
                "127.0.0.1"
        );

        assertEquals("Projeto Aplicado", updated.name());
    }

    @Test
    void nonAdministratorCannotCreateSubject() {
        assertThrows(
                SecurityException.class,
                () -> subjectService.createSubject(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        new SubjectCreateCommand(
                                10L,
                                "Disciplina Estudante",
                                "DEST",
                                null,
                                null,
                                BigDecimal.valueOf(6),
                                30,
                                SubjectState.ACTIVE,
                                30L,
                                null,
                                null,
                                true,
                                Set.of()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void subjectWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> subjectService.deleteSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        40L,
                        "127.0.0.1"
                )
        );
    }

    private static boolean hasActiveCoordinator(long coordinatorUserId, long subjectId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM coordinate_subject
                     WHERE id_coordinator_user = ?
                       AND id_subject = ?
                       AND state = 'active'
                     """)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean hasCourseSubject(long courseId, long subjectId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM integrate_subject
                     WHERE id_course = ?
                       AND id_subject = ?
                       AND state = 'active'
                     """)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }
}
