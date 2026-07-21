package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateIssueCommand;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.CertificateValidationResult;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.service.CertificateService;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CertificateServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private GradeSheetService gradeSheetService;
    private CertificateService certificateService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        gradeSheetService = new GradeSheetService(connectionProvider, FIXED_CLOCK);
        certificateService = new CertificateService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorIssuesValidCertificateForEligibleStudent() throws Exception {
        List<GradeSheet> gradeSheets = prepareCourse30ForCompletion();

        Certificate certificate = certificateService.issueCertificate(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                issueCommand(),
                IP
        );

        assertEquals(bd("13.33"), certificate.finalGrade());
        assertEquals(gradeSheets.stream().map(GradeSheet::id).toList(), certificate.gradeSheetIds());
        assertEquals(300L, certificate.courseOccurrenceId());
        assertEquals(CertificateState.ISSUED, certificate.state());
        assertNotNull(certificate.issuedAt());
        assertNotNull(certificate.validationCode());
        assertTrue(certificate.validationCode().matches("CERT-[A-F0-9]{32}"));

        CertificateValidationResult validation = certificateService.validateCertificate(
                certificate.validationCode(),
                IP
        );
        assertTrue(validation.valid());
        assertEquals(certificate.id(), validation.certificateId());
        assertEquals(30L, validation.courseId());
        assertEquals(300L, validation.courseOccurrenceId());
    }

    @Test
    void optionalCourseSubjectsDoNotBlockCertificateOrChangeItsMandatoryEctsAverage() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE integrate_subject
                     SET mandatory = 0
                     WHERE id_course = 30
                       AND id_subject = 41
                     """)) {
            statement.executeUpdate();
        }
        prepareCourse30ForCompletion();
        deleteCourse30Certificate();

        Certificate certificate = certificateService.issueCertificate(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                issueCommand(),
                IP
        );
        assertEquals(CertificateState.ISSUED, certificate.state());
        assertEquals(bd("12.00"), certificate.finalGrade());
        assertEquals(2, certificate.gradeSheetIds().size(),
                "An optional subject with a grade is shown without affecting the final grade");
    }

    @Test
    void issuedCertificateSnapshotAndValidationCodeRemainStable() throws Exception {
        prepareCourse30ForCompletion();
        Certificate issued = certificateService.issueCertificate(
                1L, null, AccessProfileType.ADMINISTRATOR, issueCommand(), IP);
        makePublishedGradeUnavailable(issued.gradeSheetIds().get(0));

        Certificate synchronizedAgain = certificateService.issueCertificate(
                1L, null, AccessProfileType.ADMINISTRATOR, issueCommand(), IP);

        assertEquals(issued.id(), synchronizedAgain.id());
        assertEquals(issued.validationCode(), synchronizedAgain.validationCode());
        assertEquals(issued.issuedAt(), synchronizedAgain.issuedAt());
        assertEquals(issued.finalGrade(), synchronizedAgain.finalGrade());
        assertTrue(certificateService.validateCertificate(issued.validationCode(), IP).valid());
    }

    @Test
    void publicValidationRejectsBlankMalformedAndUnknownCodes() {
        assertFalse(certificateService.validateCertificate(null, IP).valid());
        assertFalse(certificateService.validateCertificate("  ", IP).valid());
        assertFalse(certificateService.validateCertificate("X".repeat(81), IP).valid());
        assertFalse(certificateService.validateCertificate("CERT-DOES-NOT-EXIST", IP).valid());
    }

    @Test
    void teacherCannotIssueCertificateForCourseWithUnmanagedSubject() throws Exception {
        List<GradeSheet> gradeSheets = prepareCourse30ForCompletion();

        assertThrows(SecurityException.class, () -> certificateService.issueCertificate(
                3L,
                null,
                AccessProfileType.TEACHER,
                issueCommand(),
                IP
        ));
    }

    @Test
    void certificateWithoutCourseEligibilityIsRejected() throws Exception {
        GradeSheet gradeSheet = createPublishedApprovedGradeSheet(
                3L,
                AccessProfileType.TEACHER,
                40L,
                50L,
                90L,
                "Certificate No Eligibility",
                "GR-CERT-NO",
                bd("14.00")
        );
        addActiveStudent(6L);

        assertThrows(IllegalStateException.class, () -> certificateService.issueCertificate(
                3L,
                null,
                AccessProfileType.TEACHER,
                new CertificateIssueCommand(30L, 6L),
                IP
        ));
    }

    @Test
    void certificateWithoutApprovedGradeRecordIsRejected() throws Exception {
        prepareCourse30CertificateBase();
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Certificate No Approved Record",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(new GradeAssessmentWeight(90L, bd("100.00")))
                ),
                IP
        );
        completeAssessmentScores(gradeSheet, bd("5.00"));
        GradeSheet published = gradeSheetService.publishGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                IP
        );

        assertThrows(IllegalStateException.class, () -> certificateService.issueCertificate(
                3L,
                null,
                AccessProfileType.TEACHER,
                issueCommand(),
                IP
        ));
    }

    @Test
    void certificateWithApprovedRecordButMissingAssessmentScoreIsRejected() throws Exception {
        prepareCourse30CertificateBase();
        GradeSheet projectSheet = createForcePublishedApprovedGradeSheetWithMissingAssessmentScore();
        GradeSheet mathematicsSheet = createPublishedApprovedGradeSheet(
                1L,
                AccessProfileType.ADMINISTRATOR,
                41L,
                52L,
                91L,
                "Certificate Mathematics Complete",
                "GR-CERT-MAT-COMPLETE",
                bd("14.00")
        );

        assertThrows(IllegalStateException.class, () -> certificateService.issueCertificate(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                issueCommand(),
                IP
        ));
        assertNotNull(mathematicsSheet);
    }

    @Test
    void studentCannotIssueCertificate() {
        GradeSheet gradeSheet = createPublishedApprovedGradeSheet(
                3L,
                AccessProfileType.TEACHER,
                40L,
                50L,
                90L,
                "Certificate Student Block",
                "GR-CERT-STU",
                bd("14.00")
        );

        assertThrows(SecurityException.class, () -> certificateService.issueCertificate(
                4L,
                null,
                AccessProfileType.STUDENT,
                issueCommand(),
                IP
        ));
    }

    @Test
    void studentCanConsultOnlyOwnCertificates() throws Exception {
        List<GradeSheet> gradeSheets = prepareCourse30ForCompletion();
        Certificate certificate = certificateService.issueCertificate(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                issueCommand(),
                IP
        );

        Certificate ownCertificate = certificateService.getCertificate(
                4L,
                null,
                AccessProfileType.STUDENT,
                certificate.id(),
                IP
        );

        assertEquals(certificate.id(), ownCertificate.id());
        assertTrue(certificateService.listOwnCertificates(4L, null, AccessProfileType.STUDENT, IP)
                .stream()
                .anyMatch(visible -> visible.id() == certificate.id()));
    }

    @Test
    void studentCannotReadAnotherStudentsCertificate() throws Exception {
        addActiveStudent(6L);
        List<GradeSheet> gradeSheets = prepareCourse30ForCompletion();
        Certificate certificate = certificateService.issueCertificate(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                issueCommand(),
                IP
        );

        assertThrows(SecurityException.class, () -> certificateService.getCertificate(
                6L,
                null,
                AccessProfileType.STUDENT,
                certificate.id(),
                IP
        ));
    }

    @Test
    void studentCannotReadIncompleteCertificate() {
        assertThrows(SecurityException.class, () -> certificateService.getCertificate(
                4L,
                null,
                AccessProfileType.STUDENT,
                192L,
                IP
        ));
        assertFalse(certificateService.listOwnCertificates(4L, null, AccessProfileType.STUDENT, IP)
                .stream()
                .anyMatch(certificate -> certificate.id() == 192L));
    }

    private List<GradeSheet> prepareCourse30ForCompletion() throws SQLException {
        prepareCourse30CertificateBase();
        createPublishedApprovedGradeSheet(
                3L,
                AccessProfileType.TEACHER,
                40L,
                50L,
                90L,
                "Certificate Project Sheet",
                "GR-CERT-PRJ",
                bd("12.00")
        );
        createPublishedApprovedGradeSheet(
                1L,
                AccessProfileType.ADMINISTRATOR,
                41L,
                52L,
                91L,
                "Certificate Mathematics Sheet",
                "GR-CERT-MAT",
                bd("16.00")
        );
        return List.of(subjectGradeSheet(40L), subjectGradeSheet(41L));
    }

    private static void prepareCourse30CertificateBase() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM based_on_grade_sheet_certificate
                    WHERE id_certificate IN (
                        SELECT id_certificate
                        FROM certificate
                        WHERE id_course = 30
                          AND id_user_student = 4
                    )
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM certificate
                    WHERE id_course = 30
                      AND id_user_student = 4
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE course
                    SET ects = 18.00,
                        certificate_max_grade = 20.00
                    WHERE id_course = 30
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
                    VALUES (4, 52, 'active', '2026-01-01', '2026-06-30')
                    ON DUPLICATE KEY UPDATE state = 'active', start_date = '2026-01-01', end_date = '2026-06-30'
                    """)) {
                statement.executeUpdate();
            }
        }
    }

    private static void deleteCourse30Certificate() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM based_on_grade_sheet_certificate
                    WHERE id_certificate IN (
                        SELECT id_certificate FROM certificate
                        WHERE id_course = 30 AND id_user_student = 4
                    )
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM certificate
                     WHERE id_course = 30
                       AND id_user_student = 4
                     """)) {
                statement.executeUpdate();
            }
        }
    }

    private GradeSheet createPublishedApprovedGradeSheet(
            long actorUserId,
            AccessProfileType actorProfileType,
            long subjectId,
            long classGroupId,
            long assessmentId,
            String title,
            String gradeCode,
            BigDecimal value
    ) {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                actorUserId,
                null,
                actorProfileType,
                new GradeSheetCreateCommand(
                        subjectId,
                        title,
                        GradeSheetType.FINAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(classGroupId),
                        List.of(new GradeAssessmentWeight(assessmentId, bd("100.00")))
                ),
                IP
        );
        completeAssessmentScores(gradeSheet, value);
        return gradeSheetService.publishGradeSheet(actorUserId, null, actorProfileType, gradeSheet.id(), IP);
    }

    private GradeSheet createForcePublishedApprovedGradeSheetWithMissingAssessmentScore() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Certificate Project Missing Assessment Score",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(new GradeAssessmentWeight(90L, bd("100.00")))
                ),
                IP
        );
        insertApprovedGradeRecord(gradeSheet.id(), "AUTO-" + gradeSheet.id() + "-4", bd("14.00"));
        completeAssessmentScore(90L, bd("14.00"));
        forcePublishGradeSheet(gradeSheet.id());
        return gradeSheet;
    }

    private static void insertApprovedGradeRecord(long gradeSheetId, String code, BigDecimal value) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO grade_record (
                         id_grade_sheet, id_user_student, id_attempt, cod_grade_record,
                         value, result, recorded_at, notes
                     ) VALUES (?, 4, NULL, ?, ?, 'approved', '2026-02-11 10:15:30', 'Corrupted fixture')
                     """)) {
            statement.setLong(1, gradeSheetId);
            statement.setString(2, code);
            statement.setBigDecimal(3, value);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert grade record fixture", exception);
        }
    }

    private static void forcePublishGradeSheet(long gradeSheetId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE grade_sheet
                     SET state = 'published',
                         released_at = '2026-02-11 10:15:30'
                     WHERE id_grade_sheet = ?
                     """)) {
            statement.setLong(1, gradeSheetId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to force publish grade sheet fixture", exception);
        }
    }

    private static void makePublishedGradeUnavailable(long gradeSheetId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE grade_record
                     SET state = 'inactive'
                     WHERE id_grade_sheet = ?
                       AND id_user_student = 4
                     """)) {
            statement.setLong(1, gradeSheetId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to invalidate certificate calculation fixture", exception);
        }
    }

    private GradeSheet subjectGradeSheet(long subjectId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT gs.id_grade_sheet
                     FROM grade_sheet gs
                     WHERE gs.id_subject = ?
                       AND gs.id_course_occurrence = 300
                       AND NOT EXISTS (
                             SELECT 1
                             FROM associate_grade_sheet_class_group agscg
                             WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                       )
                     ORDER BY gs.id_grade_sheet
                     LIMIT 1
                     """)) {
            statement.setLong(1, subjectId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Consolidated subject grade sheet fixture was not found");
                }
                return gradeSheetService.getGradeSheet(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        resultSet.getLong(1),
                        IP
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load consolidated subject grade sheet fixture", exception);
        }
    }

    private static void completeAssessmentScores(GradeSheet gradeSheet, BigDecimal score) {
        for (GradeAssessmentWeight weight : gradeSheet.assessmentWeights()) {
            completeAssessmentScore(weight.assessmentId(), score);
        }
    }

    private static void completeAssessmentScore(long assessmentId, BigDecimal score) {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE assessment
                    SET state = 'active'
                    WHERE id_assessment = ?
                    """)) {
                statement.setLong(1, assessmentId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                    VALUES (4, ?, 'active')
                    ON DUPLICATE KEY UPDATE state = 'active'
                    """)) {
                statement.setLong(1, assessmentId);
                statement.executeUpdate();
            }
            if (assessmentId == 90L) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE attempt
                        SET score = ?,
                            state = 'corrected',
                            submitted_at = '2026-02-11 10:10:00'
                        WHERE id_attempt = 120
                        """)) {
                    statement.setBigDecimal(1, score);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO attempt (
                            id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
                        ) VALUES (4, ?, 1, ?, 'corrected', '2026-06-20 09:00:00', '2026-06-20 10:30:00')
                        """)) {
                    statement.setLong(1, assessmentId);
                    statement.setBigDecimal(2, score);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to complete assessment score fixture", exception);
        }
    }

    private static CertificateIssueCommand issueCommand() {
        return new CertificateIssueCommand(30L, 4L);
    }

    private static void addActiveStudent(long userId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Certificate Student " + userId);
                user.setString(3, "certificate.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-CERT-" + userId);
                profile.executeUpdate();
            }
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
