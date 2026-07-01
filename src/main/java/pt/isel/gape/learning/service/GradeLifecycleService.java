package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeRecordResult;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.Subject;

final class GradeLifecycleService {

    private static final BigDecimal DEFAULT_MAX_GRADE = new BigDecimal("20.00");
    private static final BigDecimal DEFAULT_PASSING_GRADE = new BigDecimal("9.50");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final AssessmentDAO assessmentDAO;
    private final CertificateDAO certificateDAO;
    private final GradeRecordDAO gradeRecordDAO;
    private final GradeSheetDAO gradeSheetDAO;
    private final SubjectDAO subjectDAO;
    private final CertificateService certificateService;
    private final Clock clock;

    GradeLifecycleService(ConnectionProvider connectionProvider, Clock clock) {
        this.assessmentDAO = new AssessmentDAO(connectionProvider);
        this.certificateDAO = new CertificateDAO(connectionProvider);
        this.gradeRecordDAO = new GradeRecordDAO(connectionProvider);
        this.gradeSheetDAO = new GradeSheetDAO(connectionProvider);
        this.subjectDAO = new SubjectDAO(connectionProvider);
        this.certificateService = new CertificateService(connectionProvider, clock);
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    void ensureCourseGradeSheets(Connection connection, long courseId) throws SQLException {
        for (CertificateDAO.CourseSubjectScale courseSubject : certificateDAO.findActiveCourseSubjects(connection, courseId)) {
            Subject subject = subjectDAO.findById(connection, courseSubject.subjectId()).orElse(null);
            if (subject != null) {
                ensureSubjectGradeSheetDraft(connection, subject);
            }
        }
    }

    long ensureSubjectGradeSheetDraft(Connection connection, Subject subject) throws SQLException {
        return gradeSheetDAO.findBySubjectWithoutClassGroups(connection, subject.id())
                .map(GradeSheet::id)
                .orElseGet(() -> createDraftGradeSheet(
                        connection,
                        subject,
                        List.of(),
                        "Pauta - " + subject.name(),
                        GradeSheetType.FINAL
                ));
    }

    long ensureClassGroupGradeSheetDraft(
            Connection connection,
            ClassGroup classGroup,
            Subject subject
    ) throws SQLException {
        return gradeSheetDAO.findByClassGroupId(connection, classGroup.id())
                .map(GradeSheet::id)
                .orElseGet(() -> createDraftGradeSheet(
                        connection,
                        subject,
                        List.of(classGroup.id()),
                        "Pauta - " + subject.name() + " - " + classGroup.code(),
                        GradeSheetType.FINAL
                ));
    }

    void synchronizeAfterAssessmentCorrection(
            Connection connection,
            long assessmentId,
            long currentStudentUserId
    ) throws SQLException {
        for (Long gradeSheetId : gradeSheetDAO.findGradeSheetIdsForAssessmentContext(connection, assessmentId)) {
            synchronizeGradeSheetAndCertificates(connection, gradeSheetId, currentStudentUserId);
        }
    }

    GradeSheet synchronizeGradeSheetAndCertificates(Connection connection, long gradeSheetId) throws SQLException {
        return synchronizeGradeSheetAndCertificates(connection, gradeSheetId, null);
    }

    private GradeSheet synchronizeGradeSheetAndCertificates(
            Connection connection,
            long gradeSheetId,
            Long currentStudentUserId
    ) throws SQLException {
        GradeSheet recalculatedSheet = recalculateAutomaticGradeRecords(connection, gradeSheetId);
        GradeSheet synchronizedSheet = synchronizeGradeSheetState(connection, recalculatedSheet.id());
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, synchronizedSheet);
        if (currentStudentUserId != null && !studentUserIds.contains(currentStudentUserId)) {
            studentUserIds = new ArrayList<>(studentUserIds);
            studentUserIds.add(currentStudentUserId);
        }
        for (Long studentUserId : studentUserIds) {
            for (Long courseId : gradeSheetDAO.findCourseIdsForSheetContext(connection, synchronizedSheet, studentUserId)) {
                certificateService.synchronizeCertificateForStudentCourse(connection, courseId, studentUserId);
            }
        }
        return synchronizedSheet;
    }

    GradeSheet synchronizeGradeSheetState(Connection connection, long gradeSheetId) throws SQLException {
        GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId)
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        if (gradeSheet.state() == GradeSheetState.CLOSED || gradeSheet.state() == GradeSheetState.ARCHIVED) {
            return gradeSheet;
        }
        gradeSheet = normalizeWeightsAfterClassGroupEnd(connection, gradeSheet);
        boolean complete = isComplete(connection, gradeSheet);
        if (complete && gradeSheet.state() == GradeSheetState.DRAFT) {
            gradeSheetDAO.updateState(connection, gradeSheet.id(), GradeSheetState.PUBLISHED, LocalDateTime.now(clock));
            return gradeSheetDAO.findById(connection, gradeSheetId)
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        }
        if (!complete && gradeSheet.state() == GradeSheetState.PUBLISHED) {
            gradeSheetDAO.updateState(connection, gradeSheet.id(), GradeSheetState.DRAFT, null);
            return gradeSheetDAO.findById(connection, gradeSheetId)
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        }
        return gradeSheet;
    }

    boolean isComplete(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        if (studentUserIds.isEmpty()) {
            return false;
        }
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (!weightsAreConfigured(assessmentWeights)) {
            return false;
        }
        List<Long> assessmentIds = assessmentWeights.stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
        for (Long studentUserId : studentUserIds) {
            if (!gradeSheetDAO.hasActiveGradeRecord(connection, gradeSheet.id(), studentUserId)) {
                return false;
            }
            for (Long assessmentId : assessmentIds) {
                if (!gradeSheetDAO.hasCorrectedAssessmentScore(connection, studentUserId, assessmentId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private GradeSheet recalculateAutomaticGradeRecords(Connection connection, long gradeSheetId) throws SQLException {
        GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId)
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        if (gradeSheet.state() == GradeSheetState.CLOSED || gradeSheet.state() == GradeSheetState.ARCHIVED) {
            return gradeSheet;
        }
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (studentUserIds.isEmpty() || !weightsAreConfigured(assessmentWeights)) {
            for (Long studentUserId : studentUserIds) {
                gradeRecordDAO.archiveActiveBySheetAndStudent(connection, gradeSheet.id(), studentUserId);
            }
            return gradeSheet;
        }
        for (Long studentUserId : studentUserIds) {
            Optional<BigDecimal> finalGrade = calculateFinalGrade(
                    connection,
                    gradeSheet,
                    assessmentWeights,
                    studentUserId
            );
            if (finalGrade.isEmpty()) {
                gradeRecordDAO.archiveActiveBySheetAndStudent(connection, gradeSheet.id(), studentUserId);
                continue;
            }
            GradeRecordResult result = GradeRecordService.calculateResult(finalGrade.get(), gradeSheet.passingGrade());
            gradeRecordDAO.upsertAutomatic(
                    connection,
                    gradeSheet.id(),
                    studentUserId,
                    finalGrade.get(),
                    result,
                    LocalDateTime.now(clock)
            );
        }
        return gradeSheetDAO.findById(connection, gradeSheetId)
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
    }

    private Optional<BigDecimal> calculateFinalGrade(
            Connection connection,
            GradeSheet gradeSheet,
            List<GradeAssessmentWeight> assessmentWeights,
            long studentUserId
    ) throws SQLException {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        for (GradeAssessmentWeight assessmentWeight : assessmentWeights) {
            Assessment assessment = assessmentDAO.findById(connection, assessmentWeight.assessmentId())
                    .orElse(null);
            if (assessment == null
                    || assessment.maxGrade() == null
                    || assessment.maxGrade().compareTo(BigDecimal.ZERO) <= 0) {
                return Optional.empty();
            }
            Optional<BigDecimal> score = gradeSheetDAO.findLatestCorrectedAssessmentScore(
                    connection,
                    studentUserId,
                    assessment.id()
            );
            if (score.isEmpty()) {
                return Optional.empty();
            }
            BigDecimal normalizedScore = score.get()
                    .divide(assessment.maxGrade(), 8, RoundingMode.HALF_UP)
                    .multiply(gradeSheet.maxGrade());
            BigDecimal contribution = normalizedScore
                    .multiply(assessmentWeight.weight())
                    .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
            weightedTotal = weightedTotal.add(contribution);
        }
        BigDecimal finalGrade = weightedTotal.setScale(2, RoundingMode.HALF_UP);
        if (finalGrade.compareTo(BigDecimal.ZERO) < 0 || finalGrade.compareTo(gradeSheet.maxGrade()) > 0) {
            throw new IllegalArgumentException("Calculated final grade must be within the grade sheet scale");
        }
        return Optional.of(finalGrade);
    }

    private long createDraftGradeSheet(
            Connection connection,
            Subject subject,
            List<Long> classGroupIds,
            String title,
            GradeSheetType type
    ) {
        try {
            List<GradeAssessmentWeight> weights = gradeSheetDAO.findDefaultAssessmentWeightsForSheetContext(
                    connection,
                    subject.id(),
                    classGroupIds
            );
            long gradeSheetId = gradeSheetDAO.create(connection, new GradeSheetCreateCommand(
                    subject.id(),
                    title,
                    type,
                    maxGrade(subject),
                    passingGrade(subject),
                    GradeSheetState.DRAFT,
                    classGroupIds,
                    weights
            ));
            gradeSheetDAO.replaceClassGroups(connection, gradeSheetId, classGroupIds);
            gradeSheetDAO.replaceAssessmentWeights(connection, gradeSheetId, weights);
            gradeSheetDAO.updateWeightAlert(connection, gradeSheetId, alertForTotal(weights));
            return gradeSheetId;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create automatic grade sheet", exception);
        }
    }

    private GradeSheet normalizeWeightsAfterClassGroupEnd(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (weightsAreConfigured(assessmentWeights)
                || !gradeSheetDAO.classGroupsEndedBefore(connection, gradeSheet.classGroupIds(), LocalDate.now(clock))) {
            return gradeSheet;
        }
        List<Long> assessmentIds = gradeSheetAssessmentIds(connection, gradeSheet);
        if (assessmentIds.isEmpty()) {
            return gradeSheet;
        }
        List<GradeAssessmentWeight> balancedWeights = equalWeights(assessmentIds);
        gradeSheetDAO.replaceAssessmentWeights(connection, gradeSheet.id(), balancedWeights);
        gradeSheetDAO.updateWeightAlert(
                connection,
                gradeSheet.id(),
                "Assessment weights were automatically redistributed equally at the end of the class group period so the sum is 100%."
        );
        return gradeSheetDAO.findById(connection, gradeSheet.id())
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheet.id()));
    }

    private List<Long> gradeSheetAssessmentIds(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<Long> contextAssessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                gradeSheet.subjectId(),
                gradeSheet.classGroupIds()
        );
        if (!contextAssessmentIds.isEmpty()) {
            return contextAssessmentIds;
        }
        return gradeSheet.assessmentWeights().stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
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

    private static String alertForTotal(List<GradeAssessmentWeight> weights) {
        if (weights == null || weights.isEmpty()) {
            return "No assessments are configured for this context yet. The grade sheet stays draft until grades exist.";
        }
        BigDecimal total = weights.stream()
                .map(GradeAssessmentWeight::weight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(ONE_HUNDRED) == 0) {
            return null;
        }
        return "Assessment weights total " + total.stripTrailingZeros().toPlainString()
                + "%. If this is not regularized before the class group period ends, the system will redistribute "
                + "the weights equally so the sum is 100%.";
    }

    private static BigDecimal maxGrade(Subject subject) {
        return subject.finalGradeMax() == null || subject.finalGradeMax().compareTo(BigDecimal.ZERO) <= 0
                ? DEFAULT_MAX_GRADE
                : subject.finalGradeMax();
    }

    private static BigDecimal passingGrade(Subject subject) {
        BigDecimal maxGrade = maxGrade(subject);
        if (maxGrade.compareTo(DEFAULT_MAX_GRADE) == 0) {
            return DEFAULT_PASSING_GRADE;
        }
        return maxGrade.divide(BigDecimal.valueOf(2L), 2, RoundingMode.HALF_UP);
    }

    private static List<GradeAssessmentWeight> equalWeights(List<Long> assessmentIds) {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueAssessmentIds = new ArrayList<>(new LinkedHashSet<>(assessmentIds));
        BigDecimal base = ONE_HUNDRED.divide(BigDecimal.valueOf(uniqueAssessmentIds.size()), 2, RoundingMode.DOWN);
        BigDecimal assigned = BigDecimal.ZERO;
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (int index = 0; index < uniqueAssessmentIds.size(); index++) {
            BigDecimal weight = index == uniqueAssessmentIds.size() - 1
                    ? ONE_HUNDRED.subtract(assigned)
                    : base;
            assigned = assigned.add(weight);
            weights.add(new GradeAssessmentWeight(uniqueAssessmentIds.get(index), weight));
        }
        return List.copyOf(weights);
    }
}
