package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class CertificateService {

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

    public CertificateValidationResult validateCertificate(String validationCode, String sourceIp) {
        if (validationCode == null || validationCode.isBlank()) {
            auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                    "certificate", "blank", "failure", sourceIp);
            return CertificateValidationResult.invalid();
        }
        String normalizedCode = validationCode.trim();
        if (normalizedCode.length() > VALIDATION_CODE_MAX_LENGTH) {
            auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                    "certificate", "malformed", "failure", sourceIp);
            return CertificateValidationResult.invalid();
        }
        try (Connection connection = connectionProvider.getConnection()) {
            Certificate certificate = certificateDAO.findIssuedByValidationCode(connection, normalizedCode)
                    .orElse(null);
            if (certificate == null) {
                auditService.record(null, null, "CERTIFICATE_PUBLIC_VALIDATE",
                        "certificate", "not-found", "failure", sourceIp);
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
                    certificate.courseOccurrenceId(),
                    certificate.issuedAt()
            );
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to validate certificate");
        }
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
                    CourseEnrollment enrollment = requireCertificateCourseEnrollment(
                            connection,
                            command.studentUserId(),
                            command.courseId(),
                            command.courseOccurrenceId()
                    );
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
                            enrollment.courseOccurrenceId(),
                            command.studentUserId()
                    );
                    if (certificate == null || !isCompleted(certificate)) {
                        connection.commit();
                        throw new IllegalStateException(
                                "Certificates are published automatically only after every mandatory subject has a positive final grade"
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

    Certificate synchronizeCertificateForStudentCourse(
            Connection connection,
            long courseId,
            long studentUserId
    ) throws SQLException {
        CourseEnrollment enrollment = requireCertificateCourseEnrollment(connection, studentUserId, courseId, null);
        return synchronizeCertificateForStudentCourse(connection, courseId, enrollment.courseOccurrenceId(), studentUserId);
    }

    void synchronizeCertificatesForCourse(Connection connection, long courseId) throws SQLException {
        for (Certificate certificate : certificateDAO.findByCourse(connection, courseId)) {
            try {
                synchronizeCertificateForStudentCourse(
                        connection,
                        courseId,
                        certificate.courseOccurrenceId(),
                        certificate.studentUserId()
                );
            } catch (IllegalStateException ignored) {
                // Withdrawn or otherwise ineligible enrollments stay as drafts;
                // eligible certificates continue to be reconciled.
            }
        }
    }

    Certificate synchronizeCertificateForStudentCourse(
            Connection connection,
            long courseId,
            long courseOccurrenceId,
            long studentUserId
    ) throws SQLException {
        if (!enrollmentDAO.activeStudentExists(connection, studentUserId)) {
            return null;
        }
        requireCertificateCourseEnrollment(connection, studentUserId, courseId, courseOccurrenceId);
        String defaultTitle = "Certificate - " + certificateDAO.findCourseName(connection, courseId);
        long certificateId = certificateDAO.createDraftIfAbsent(
                connection,
                courseId,
                courseOccurrenceId,
                studentUserId,
                defaultTitle
        );
        Certificate current = requireCertificate(connection, certificateId);
        if (isCompleted(current)) {
            return current;
        }
        CertificateCalculation calculation = calculateCertificate(
                connection,
                courseId,
                courseOccurrenceId,
                studentUserId
        );
        if (!calculation.publishable()) {
            certificateDAO.updateDraft(
                    connection,
                    certificateId,
                    current.title(),
                    current.notes(),
                    current.type(),
                    current.template()
            );
            certificateDAO.replaceGradeSheets(connection, certificateId, calculation.gradeSheetIds());
        } else {
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
                Certificate synchronizedCertificate = certificate;
                try {
                    synchronizedCertificate = synchronizeCertificateForStudentCourse(
                            connection,
                            certificate.courseId(),
                            certificate.courseOccurrenceId(),
                            actorUserId
                    );
                } catch (IllegalStateException ignored) {
                    // A withdrawn/ineligible enrollment remains hidden from a
                    // student's published list, without breaking other certificates.
                }
                if (synchronizedCertificate != null && isCompleted(synchronizedCertificate)) {
                    certificates.add(synchronizedCertificate);
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
            try {
                Certificate synchronizedCertificate = synchronizeCertificateForStudentCourse(
                        connection,
                        certificate.courseId(),
                        certificate.courseOccurrenceId(),
                        certificate.studentUserId()
                );
                if (synchronizedCertificate != null) {
                    certificate = synchronizedCertificate;
                }
            } catch (IllegalStateException ignored) {
                // Keep the stored draft for withdrawn/ineligible enrollments.
            }
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

    private Certificate requireCertificate(Connection connection, long certificateId) throws SQLException {
        return certificateDAO.findById(connection, certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certificateId));
    }

    private CertificateCalculation calculateCertificate(
            Connection connection,
            long courseId,
            long courseOccurrenceId,
            long studentUserId
    ) throws SQLException {
        CertificateDAO.CourseScale course = certificateDAO.findCourseScale(connection, courseId);
        List<CertificateDAO.CourseSubjectScale> subjects = certificateDAO.findActiveCourseSubjects(connection, courseId);
        if (subjects.isEmpty()) {
            return new CertificateCalculation(null, List.of(), false);
        }
        BigDecimal mandatorySubjectEctsTotal = BigDecimal.ZERO;
        for (CertificateDAO.CourseSubjectScale subject : subjects) {
            if (subject.mandatory()) {
                mandatorySubjectEctsTotal = mandatorySubjectEctsTotal.add(subject.ects());
            }
        }
        if (mandatorySubjectEctsTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new CertificateCalculation(null, List.of(), false);
        }

        BigDecimal weightedTotal = BigDecimal.ZERO;
        List<Long> gradeSheetIds = new ArrayList<>();
        boolean mandatoryGradesComplete = true;
        for (CertificateDAO.CourseSubjectScale subject : subjects) {
            Optional<CertificateDAO.SubjectApprovedGrade> approvedGrade = findApprovedSubjectGrade(
                    connection, courseId, subject.subjectId(), studentUserId);
            if (approvedGrade.isEmpty()) {
                if (subject.mandatory()) {
                    mandatoryGradesComplete = false;
                }
                continue;
            }
            CertificateDAO.SubjectApprovedGrade grade = approvedGrade.get();
            BigDecimal subjectFinalGrade = grade.value()
                    .divide(grade.gradeSheetMaxGrade(), 8, RoundingMode.HALF_UP)
                    .multiply(subject.finalGradeMax());
            BigDecimal courseScaleGrade = subjectFinalGrade
                    .divide(subject.finalGradeMax(), 8, RoundingMode.HALF_UP)
                    .multiply(course.certificateMaxGrade());
            if (subject.mandatory()) {
                weightedTotal = weightedTotal.add(courseScaleGrade.multiply(subject.ects()));
                if (normalizedGrade(grade).compareTo(new BigDecimal("0.50")) <= 0) {
                    mandatoryGradesComplete = false;
                }
            }
            // Optional subjects with an approved final grade are shown on the
            // certificate, but never contribute to its weighted final grade.
            gradeSheetIds.add(grade.gradeSheetId());
        }
        if (!mandatoryGradesComplete) {
            return new CertificateCalculation(null, List.copyOf(gradeSheetIds), false);
        }
        BigDecimal finalGrade = weightedTotal.divide(mandatorySubjectEctsTotal, 2, RoundingMode.HALF_UP);
        validateFinalGrade(finalGrade, course.certificateMaxGrade());
        return new CertificateCalculation(finalGrade, List.copyOf(gradeSheetIds), true);
    }

    private Optional<CertificateDAO.SubjectApprovedGrade> findApprovedSubjectGrade(
            Connection connection,
            long courseId,
            long subjectId,
            long studentUserId
    ) throws SQLException {
        CertificateDAO.SubjectApprovedGrade bestGrade = null;
        for (CertificateDAO.SubjectApprovedGrade grade : certificateDAO.findApprovedSubjectGradeCandidates(
                connection, courseId, subjectId, studentUserId
        )) {
            if (gradeSheetCompleteForCertificateStudent(connection, grade.gradeSheetId(), studentUserId)) {
                if (bestGrade == null || normalizedGrade(grade).compareTo(normalizedGrade(bestGrade)) > 0) {
                    bestGrade = grade;
                }
            }
        }
        return Optional.ofNullable(bestGrade);
    }

    private static BigDecimal normalizedGrade(CertificateDAO.SubjectApprovedGrade grade) {
        if (grade.value() == null || grade.gradeSheetMaxGrade() == null
                || grade.gradeSheetMaxGrade().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grade.value().divide(grade.gradeSheetMaxGrade(), 8, RoundingMode.HALF_UP);
    }

    private boolean gradeSheetCompleteForCertificateStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId).orElse(null);
        if (gradeSheet == null || !isPublishedForCertificate(gradeSheet)) {
            return false;
        }
        // Certificate eligibility is student-specific: other students may
        // still have '-' in the same published sheet without blocking a
        // student who already has a final record in every mandatory subject.
        return gradeSheetDAO.hasActiveGradeRecord(connection, gradeSheet.id(), studentUserId);
    }

    private static boolean isPublishedForCertificate(GradeSheet gradeSheet) {
        return gradeSheet.state() == GradeSheetState.PUBLISHED || gradeSheet.state() == GradeSheetState.CLOSED;
    }

    private CourseEnrollment requireCertificateCourseEnrollment(
            Connection connection,
            long studentUserId,
            long courseId,
            Long courseOccurrenceId
    ) throws SQLException {
        Optional<CourseEnrollment> enrollmentResult = courseOccurrenceId != null && courseOccurrenceId > 0
                ? enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, courseOccurrenceId)
                : enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId);
        CourseEnrollment enrollment = enrollmentResult.orElseThrow(() -> new IllegalStateException(
                courseOccurrenceId != null && courseOccurrenceId > 0
                        ? "Student is not enrolled in the requested course occurrence"
                        : "Student is not enrolled in this course"
        ));
        if (enrollment.state() == EnrollmentState.WITHDRAWN || enrollment.state() == EnrollmentState.REJECTED) {
            throw new IllegalStateException("Student is not eligible for a certificate in this course");
        }
        return enrollment;
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

    private record CertificateCalculation(
            BigDecimal finalGrade,
            List<Long> gradeSheetIds,
            boolean publishable
    ) {
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
            String code = "CERT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
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
        if (command.courseOccurrenceId() != null && command.courseOccurrenceId() <= 0) {
            throw new IllegalArgumentException("Course occurrence id must be positive");
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
