package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateIssueCommand;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.CertificateType;
import pt.isel.gape.learning.model.CertificateValidationResult;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class CertificateService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final int VALIDATION_CODE_MAX_LENGTH = 80;

    private final ConnectionProvider connectionProvider;
    private final CertificateDAO certificateDAO;
    private final GradeSheetDAO gradeSheetDAO;
    private final GradeRecordDAO gradeRecordDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final GradeAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public CertificateService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CertificateDAO(connectionProvider),
                new GradeSheetDAO(connectionProvider),
                new GradeRecordDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public CertificateService(
            ConnectionProvider connectionProvider,
            CertificateDAO certificateDAO,
            GradeSheetDAO gradeSheetDAO,
            GradeRecordDAO gradeRecordDAO,
            EnrollmentDAO enrollmentDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.certificateDAO = Objects.requireNonNull(certificateDAO, "certificateDAO is required");
        this.gradeSheetDAO = Objects.requireNonNull(gradeSheetDAO, "gradeSheetDAO is required");
        this.gradeRecordDAO = Objects.requireNonNull(gradeRecordDAO, "gradeRecordDAO is required");
        this.enrollmentDAO = Objects.requireNonNull(enrollmentDAO, "enrollmentDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new GradeAccessPolicy(
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public Certificate issueCertificate(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CertificateIssueCommand command,
            String sourceIp
    ) {
        try {
            validateIssueCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    if (!enrollmentDAO.activeStudentExists(connection, command.studentUserId())) {
                        throw new IllegalArgumentException("Student not found or inactive: " + command.studentUserId());
                    }
                    requireCertificateCourseEnrollment(connection, command.studentUserId(), command.courseId());
                    accessPolicy.requireCourseManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command.courseId(),
                            sourceIp
                    );
                    Certificate certificate = synchronizeCertificateForStudentCourse(
                            connection,
                            command.courseId(),
                            command.studentUserId()
                    );
                    if (certificate == null || !isCompleted(certificate)) {
                        throw new IllegalStateException(
                                "Certificates are published automatically only after all final grades exist"
                        );
                    }
                    requireCertificateManager(connection, actorUserId, sessionId, actorProfileType, certificate, sourceIp);
                    auditService.record(connection, actorUserId, sessionId, "CERTIFICATE_SYNC",
                            "certificate", Long.toString(certificate.id()), "success", sourceIp);
                    connection.commit();
                    return certificate;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CERTIFICATE_SYNC", "automatic", sourceIp);
            throw wrap(exception, "Failed to synchronize certificate");
        }
    }

    public CertificateValidationResult validateCertificate(String validationCode, String sourceIp) {
        if (validationCode == null || validationCode.isBlank()) {
            auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                    "certificate", "blank", "failure", sourceIp);
            return CertificateValidationResult.invalid();
        }
        String normalizedCode = validationCode.trim();
        try (Connection connection = connectionProvider.getConnection()) {
            Certificate certificate = certificateDAO.findIssuedByValidationCode(connection, normalizedCode)
                    .orElse(null);
            if (certificate == null) {
                auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                        "certificate", normalizedCode, "failure", sourceIp);
                return CertificateValidationResult.invalid();
            }
            auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                    "certificate", Long.toString(certificate.id()), "success", sourceIp);
            return new CertificateValidationResult(
                    true,
                    certificate.id(),
                    certificate.title(),
                    certificate.type(),
                    certificate.courseId(),
                    certificate.issuedAt()
            );
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to validate certificate");
        }
    }

    Certificate synchronizeCertificateForStudentCourse(
            Connection connection,
            long courseId,
            long studentUserId
    ) throws SQLException {
        if (!enrollmentDAO.activeStudentExists(connection, studentUserId)) {
            return null;
        }
        requireCertificateCourseEnrollment(connection, studentUserId, courseId);
        String defaultTitle = "Certificate - " + certificateDAO.findCourseName(connection, courseId);
        long certificateId = certificateDAO.createDraftIfAbsent(connection, courseId, studentUserId, defaultTitle);
        Certificate current = requireCertificate(connection, certificateId);
        try {
            CertificateCalculation calculation = calculateCertificate(connection, courseId, studentUserId);
            String validationCode = resolveValidationCode(
                    connection,
                    current.state() == CertificateState.ISSUED ? current.validationCode() : null,
                    certificateId
            );
            certificateDAO.updateIssued(
                    connection,
                    certificateId,
                    current.title(),
                    current.notes(),
                    current.type(),
                    current.template(),
                    validationCode,
                    current.issuedAt() == null ? LocalDateTime.now(clock) : current.issuedAt(),
                    calculation.finalGrade()
            );
            certificateDAO.replaceGradeSheets(connection, certificateId, calculation.gradeSheetIds());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            if (current.state() == CertificateState.ISSUED
                    || current.validationCode() != null
                    || current.issuedAt() != null
                    || current.finalGrade() != null
                    || !current.gradeSheetIds().isEmpty()) {
                certificateDAO.updateDraft(
                        connection,
                        certificateId,
                        current.title(),
                        current.notes(),
                        current.type(),
                        current.template()
                );
                certificateDAO.replaceGradeSheets(connection, certificateId, List.of());
            }
        }
        return requireCertificate(connection, certificateId);
    }

    public List<Certificate> listOwnCertificates(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            List<Certificate> certificates = new ArrayList<>();
            for (Certificate certificate : certificateDAO.findByStudent(connection, actorUserId)) {
                if (isCompleted(certificate)) {
                    certificates.add(certificate);
                }
            }
            return List.copyOf(certificates);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list student certificates");
        }
    }

    public Certificate getCertificate(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long certificateId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            Certificate certificate = requireCertificate(connection, certificateId);
            if (actorProfileType == AccessProfileType.STUDENT) {
                accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                if (certificate.studentUserId() != actorUserId) {
                    throw new SecurityException("Students can only access their own certificates");
                }
                if (!isCompleted(certificate)) {
                    throw new SecurityException("Students cannot access incomplete certificates");
                }
                return certificate;
            }
            requireCertificateManager(connection, actorUserId, sessionId, actorProfileType, certificate, sourceIp);
            return certificate;
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read certificate");
        }
    }

    public Certificate revokeCertificate(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long certificateId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Certificate certificate = certificateDAO.lockById(connection, certificateId)
                        .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certificateId));
                requireCertificateManager(connection, actorUserId, sessionId, actorProfileType, certificate, sourceIp);
                if (certificate.state() != CertificateState.ISSUED || !isCompleted(certificate)) {
                    throw new IllegalStateException("Only issued certificates can be revoked");
                }
                certificateDAO.revoke(connection, certificateId, LocalDateTime.now(clock));
                certificateDAO.replaceGradeSheets(connection, certificateId, certificate.gradeSheetIds());
                auditService.record(connection, actorUserId, sessionId, "CERTIFICATE_REVOKE",
                        "certificate", Long.toString(certificateId), "success", sourceIp);
                Certificate revoked = requireCertificate(connection, certificateId);
                connection.commit();
                return revoked;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                auditFailure(actorUserId, sessionId, "CERTIFICATE_REVOKE", Long.toString(certificateId), sourceIp);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to revoke certificate");
        }
    }

    private Certificate requireCertificate(Connection connection, long certificateId) throws SQLException {
        return certificateDAO.findById(connection, certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certificateId));
    }

    private CertificateCalculation calculateCertificate(
            Connection connection,
            long courseId,
            long studentUserId
    ) throws SQLException {
        CertificateDAO.CourseScale course = certificateDAO.findCourseScale(connection, courseId);
        List<CertificateDAO.CourseSubjectScale> subjects = certificateDAO.findActiveCourseSubjects(connection, courseId);
        if (subjects.isEmpty()) {
            throw new IllegalStateException("Certificate requires at least one active course subject");
        }
        BigDecimal subjectEctsTotal = BigDecimal.ZERO;
        for (CertificateDAO.CourseSubjectScale subject : subjects) {
            subjectEctsTotal = subjectEctsTotal.add(subject.ects());
        }
        if (subjectEctsTotal.compareTo(course.ects()) != 0) {
            throw new IllegalStateException("Course subject ECTS total must match course ECTS before certificates can be completed");
        }

        BigDecimal weightedTotal = BigDecimal.ZERO;
        List<Long> gradeSheetIds = new ArrayList<>();
        for (CertificateDAO.CourseSubjectScale subject : subjects) {
            CertificateDAO.SubjectApprovedGrade approvedGrade = findApprovedSubjectGrade(
                    connection,
                    courseId,
                    subject.subjectId(),
                    studentUserId
            ).orElseThrow(() -> new IllegalStateException(
                    "Student has no approved published grade for every course subject"));
            BigDecimal subjectFinalGrade = approvedGrade.value()
                    .divide(approvedGrade.gradeSheetMaxGrade(), 8, RoundingMode.HALF_UP)
                    .multiply(subject.finalGradeMax());
            BigDecimal courseScaleGrade = subjectFinalGrade
                    .divide(subject.finalGradeMax(), 8, RoundingMode.HALF_UP)
                    .multiply(course.certificateMaxGrade());
            weightedTotal = weightedTotal.add(courseScaleGrade.multiply(subject.ects()));
            gradeSheetIds.add(approvedGrade.gradeSheetId());
        }
        BigDecimal finalGrade = weightedTotal.divide(course.ects(), 2, RoundingMode.HALF_UP);
        validateFinalGrade(finalGrade, course.certificateMaxGrade());
        return new CertificateCalculation(finalGrade, List.copyOf(gradeSheetIds));
    }

    private Optional<CertificateDAO.SubjectApprovedGrade> findApprovedSubjectGrade(
            Connection connection,
            long courseId,
            long subjectId,
            long studentUserId
    ) throws SQLException {
        for (CertificateDAO.SubjectApprovedGrade grade : certificateDAO.findApprovedSubjectGradeCandidates(
                connection,
                courseId,
                subjectId,
                studentUserId
        )) {
            if (gradeSheetCompleteForCertificateStudent(connection, grade.gradeSheetId(), studentUserId)) {
                return Optional.of(grade);
            }
        }
        return Optional.empty();
    }

    private boolean gradeSheetCompleteForCertificateStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId).orElse(null);
        if (gradeSheet == null) {
            return false;
        }
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (!weightsAreConfigured(assessmentWeights)) {
            return false;
        }
        List<Long> assessmentIds = assessmentWeights.stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
        for (Long assessmentId : assessmentIds) {
            if (!gradeSheetDAO.hasCorrectedAssessmentScore(connection, studentUserId, assessmentId)) {
                return false;
            }
        }
        return true;
    }

    private List<GradeAssessmentWeight> gradeSheetAssessmentWeights(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        Map<Long, BigDecimal> configuredWeights = new LinkedHashMap<>();
        for (GradeAssessmentWeight weight : gradeSheet.assessmentWeights()) {
            configuredWeights.putIfAbsent(weight.assessmentId(), weight.weight());
        }
        List<Long> contextAssessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                gradeSheet.subjectId(),
                gradeSheet.classGroupIds()
        );
        if (contextAssessmentIds.isEmpty()) {
            return gradeSheet.assessmentWeights();
        }
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (Long assessmentId : contextAssessmentIds) {
            weights.add(new GradeAssessmentWeight(assessmentId, configuredWeights.get(assessmentId)));
        }
        return List.copyOf(weights);
    }

    private static boolean weightsAreConfigured(List<GradeAssessmentWeight> assessmentWeights) {
        if (assessmentWeights == null || assessmentWeights.isEmpty()) {
            return false;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (GradeAssessmentWeight assessmentWeight : assessmentWeights) {
            if (assessmentWeight == null
                    || assessmentWeight.weight() == null
                    || assessmentWeight.weight().compareTo(BigDecimal.ZERO) < 0
                    || assessmentWeight.weight().compareTo(ONE_HUNDRED) > 0) {
                return false;
            }
            total = total.add(assessmentWeight.weight());
        }
        return total.compareTo(ONE_HUNDRED) == 0;
    }

    private void requireCertificateCourseEnrollment(
            Connection connection,
            long studentUserId,
            long courseId
    ) throws SQLException {
        CourseEnrollment enrollment = enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
                .orElseThrow(() -> new IllegalStateException("Student is not enrolled in this course"));
        if (enrollment.state() == EnrollmentState.WITHDRAWN || enrollment.state() == EnrollmentState.REJECTED) {
            throw new IllegalStateException("Student is not eligible for a certificate in this course");
        }
    }

    private static boolean isCompleted(Certificate certificate) {
        return certificate.state() == CertificateState.ISSUED
                && certificate.validationCode() != null
                && !certificate.validationCode().isBlank()
                && certificate.issuedAt() != null
                && certificate.finalGrade() != null;
    }

    private void requireCertificateManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Certificate certificate,
            String sourceIp
    ) throws SQLException {
        if (certificate.gradeSheetIds().isEmpty()) {
            accessPolicy.requireCourseManager(
                    connection,
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    certificate.courseId(),
                    sourceIp
            );
            return;
        }
        requireCertificateGradeSheetManager(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                certificate.gradeSheetIds(),
                sourceIp
        );
    }

    private void requireCertificateGradeSheetManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            List<Long> gradeSheetIds,
            String sourceIp
    ) throws SQLException {
        for (Long gradeSheetId : gradeSheetIds) {
            GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId)
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
            accessPolicy.requireGradeSheetManager(
                    connection,
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    gradeSheet,
                    sourceIp
            );
        }
    }

    private record CertificateCalculation(BigDecimal finalGrade, List<Long> gradeSheetIds) {
    }

    private String resolveValidationCode(
            Connection connection,
            String providedCode,
            long certificateId
    ) throws SQLException {
        if (providedCode != null && !providedCode.isBlank()) {
            String code = providedCode.trim();
            if (code.length() > VALIDATION_CODE_MAX_LENGTH) {
                throw new IllegalArgumentException("Certificate validation code is too long");
            }
            if (certificateDAO.validationCodeExistsForAnother(connection, code, certificateId)) {
                throw new IllegalArgumentException("Certificate validation code already exists");
            }
            return code;
        }
        for (int attempts = 0; attempts < 5; attempts++) {
            String code = "CERT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            if (!certificateDAO.validationCodeExists(connection, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique certificate validation code");
    }

    private static void validateIssueCommand(CertificateIssueCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Course id must be positive");
        }
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Student id must be positive");
        }
    }

    private static void validateFinalGrade(BigDecimal finalGrade, BigDecimal maxGrade) {
        if (finalGrade == null) {
            throw new IllegalStateException("Certificate final grade is required");
        }
        if (finalGrade.compareTo(BigDecimal.ZERO) < 0 || finalGrade.compareTo(maxGrade) > 0) {
            throw new IllegalArgumentException("Certificate final grade must be within the course scale");
        }
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "certificate", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
