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
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeRecord;
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
    private final ClassGroupDAO classGroupDAO;
    private final GradeRecordDAO gradeRecordDAO;
    private final GradeSheetDAO gradeSheetDAO;
    private final SubjectDAO subjectDAO;
    private final CertificateService certificateService;
    private final Clock clock;

    GradeLifecycleService(ConnectionProvider connectionProvider, Clock clock) {
        this.assessmentDAO = new AssessmentDAO(connectionProvider);
        this.classGroupDAO = new ClassGroupDAO(connectionProvider);
        this.gradeRecordDAO = new GradeRecordDAO(connectionProvider);
        this.gradeSheetDAO = new GradeSheetDAO(connectionProvider);
        this.subjectDAO = new SubjectDAO(connectionProvider);
        this.certificateService = new CertificateService(connectionProvider, clock);
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /**
     * Restores the one-to-one grade-sheet topology without inventing sheets
     * from a course-subject association.  Every class group has one final
     * source sheet; each real subject-occurrence context then has one and only
     * one consolidated sheet.
     */
    void synchronizeGradeSheetTopology(Connection connection) throws SQLException {
        gradeSheetDAO.removeOrphanSubjectOccurrenceGradeSheets(connection);
        for (ClassGroup classGroup : classGroupDAO.findAll(connection)) {
            Subject subject = subjectDAO.findById(connection, classGroup.subjectId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Class group subject was not found: " + classGroup.subjectId()
                    ));
            ensureClassGroupGradeSheetDraft(connection, classGroup, subject);
        }
        gradeSheetDAO.removeOrphanSubjectOccurrenceGradeSheets(connection);
    }

    long ensureClassGroupGradeSheetDraft(
            Connection connection,
            ClassGroup classGroup,
            Subject subject
    ) throws SQLException {
        long gradeSheetId = gradeSheetDAO.findFinalByClassGroupId(connection, classGroup.id())
                .map(GradeSheet::id)
                .orElseGet(() -> createDraftGradeSheet(
                        connection,
                        subject,
                        null,
                        List.of(classGroup.id()),
                        "Grade sheet - " + subject.name() + " - " + classGroup.code(),
                        GradeSheetType.FINAL
                ));
        synchronizeGradeSheetAndCertificates(connection, gradeSheetId);
        return gradeSheetId;
    }

    void synchronizeAfterClassGroupEnrollmentChange(
            Connection connection,
            long classGroupId
    ) throws SQLException {
        gradeSheetDAO.findFinalByClassGroupId(connection, classGroupId)
                .ifPresent(sheet -> {
                    try {
                        synchronizeGradeSheetAndCertificates(connection, sheet.id());
                    } catch (SQLException exception) {
                        throw new GradeSheetSynchronizationException(exception);
                    }
                });
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

    /**
     * Reconciles every sheet that used an assessment which has just been
     * removed.  The assessment FK removes the association rows atomically;
     * this method then keeps the remaining weights valid and recalculates the
     * automatic grade records before the enclosing mutation is committed.
     */
    void synchronizeAfterAssessmentDeletion(
            Connection connection,
            List<Long> affectedGradeSheetIds
    ) throws SQLException {
        if (affectedGradeSheetIds == null || affectedGradeSheetIds.isEmpty()) {
            return;
        }
        for (Long gradeSheetId : new LinkedHashSet<>(affectedGradeSheetIds)) {
            GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId).orElse(null);
            if (gradeSheet == null) {
                continue;
            }
            List<GradeAssessmentWeight> remainingWeights = gradeSheetDAO.findAssessmentWeights(
                    connection,
                    gradeSheetId
            );
            // Deleting an assessment removes only that assessment.  The
            // remaining weights must stay untouched; balancing is reserved for
            // the explicit completed-class-group exception.
            gradeSheetDAO.updateWeightAlert(connection, gradeSheetId, alertForTotal(remainingWeights));
            synchronizeGradeSheetAndCertificates(connection, gradeSheetId);
        }
    }

    /**
     * Makes every grade sheet in a completed, non-cancelled academic period
     * available to consultation.  Availability and academic completeness are
     * intentionally different: incomplete sheets remain Published and expose
     * their missing values as {@code -}, while certificates continue to use
     * the stricter completeness checks.
     */
    void synchronizeCompletedPeriodPublications(Connection connection) throws SQLException {
        for (Long gradeSheetId : gradeSheetDAO.findGradeSheetIdsForCompletedPeriods(
                connection,
                LocalDate.now(clock)
        )) {
            GradeSheet synchronizedSheet = synchronizeGradeSheetAndCertificates(connection, gradeSheetId);
            boolean complete = isComplete(connection, synchronizedSheet);
            if (synchronizedSheet.state() == GradeSheetState.PUBLISHED && !complete) {
                gradeSheetDAO.updatePublicationExplanation(
                        connection,
                        synchronizedSheet.id(),
                        publicationExplanation(connection, synchronizedSheet)
                );
                continue;
            }
            if (synchronizedSheet.state() != GradeSheetState.DRAFT) {
                continue;
            }

            String explanation = publicationExplanation(connection, synchronizedSheet);
            gradeSheetDAO.updateState(
                    connection,
                    synchronizedSheet.id(),
                    GradeSheetState.PUBLISHED,
                    LocalDateTime.now(clock),
                    explanation
            );

            // The publication changes what can be consulted, so recalculate
            // any dependent subject sheet and certificate only after it is
            // persisted.  The certificate service still rejects incomplete
            // academic results.
            synchronizeGradeSheetAndCertificates(connection, synchronizedSheet.id());
        }
    }

    void synchronizeCompletedClassGroupPublications(Connection connection) throws SQLException {
        for (Long gradeSheetId : gradeSheetDAO.findGradeSheetIdsForCompletedClassGroups(connection)) {
            synchronizeGradeSheetAndCertificates(connection, gradeSheetId);
        }
    }

    /**
     * Reconciles the normal automatic lifecycle for all non-terminal sheets.
     * It is reserved for bootstrap/migration conformance; routine temporal
     * synchronization only needs to inspect newly completed periods.
     */
    void synchronizeAllGradeSheetStates(Connection connection) throws SQLException {
        for (Long gradeSheetId : gradeSheetDAO.findSynchronizableGradeSheetIds(connection)) {
            // Seed/migration conformance must not rewrite valid historic grade
            // record fixtures.  State reconciliation is enough here because
            // the records were already persisted; normal correction flows use
            // synchronizeGradeSheetAndCertificates when recalculation is due.
            synchronizeGradeSheetState(connection, gradeSheetId);
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
                certificateService.synchronizeCertificateForStudentCourse(
                        connection,
                        courseId,
                        synchronizedSheet.courseOccurrenceId(),
                        studentUserId
                );
            }
        }
        if (isClassGroupGradeSheet(synchronizedSheet)) {
            synchronizeSubjectGradeSheetForClassGroupContext(
                    connection,
                    synchronizedSheet.subjectId(),
                    synchronizedSheet.courseOccurrenceId()
            );
        }
        return synchronizedSheet;
    }

    void synchronizeSubjectGradeSheetForClassGroupContext(
            Connection connection,
            long subjectId,
            long courseOccurrenceId
    ) throws SQLException {
        if (courseOccurrenceId <= 0
                || !gradeSheetDAO.hasClassGroupsForSubjectOccurrence(connection, subjectId, courseOccurrenceId)) {
            return;
        }
        Subject subject = subjectDAO.findById(connection, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        long subjectGradeSheetId = gradeSheetDAO.findBySubjectWithoutClassGroups(
                        connection,
                        subjectId,
                        courseOccurrenceId
                )
                .map(GradeSheet::id)
                .orElseGet(() -> createSubjectGradeSheetDraft(connection, subject, courseOccurrenceId));
        synchronizeGradeSheetAndCertificates(connection, subjectGradeSheetId);
    }

    GradeSheet synchronizeGradeSheetState(Connection connection, long gradeSheetId) throws SQLException {
        GradeSheet gradeSheet = gradeSheetDAO.findById(connection, gradeSheetId)
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        if (gradeSheet.state() == GradeSheetState.CLOSED || gradeSheet.state() == GradeSheetState.INACTIVE) {
            return gradeSheet;
        }
        if (isSubjectGradeSheet(gradeSheet)) {
            return synchronizeSubjectGradeSheetState(connection, gradeSheet);
        }
        gradeSheet = synchronizeAssessmentWeightTopology(connection, gradeSheet);
        gradeSheet = normalizeWeightsAfterClassGroupEnd(connection, gradeSheet);
        boolean complete = isComplete(connection, gradeSheet);
        boolean forcePublication = classGroupCompletionReached(connection, gradeSheet);
        if (complete || forcePublication) {
            String explanation = forcePublication && !complete
                    ? publicationExplanation(connection, gradeSheet)
                    : null;
            if (gradeSheet.state() == GradeSheetState.DRAFT) {
                gradeSheetDAO.updateState(
                        connection,
                        gradeSheet.id(),
                        GradeSheetState.PUBLISHED,
                        LocalDateTime.now(clock),
                        explanation
                );
                return gradeSheetDAO.findById(connection, gradeSheetId)
                        .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
            }
            if (gradeSheet.state() == GradeSheetState.PUBLISHED) {
                String currentExplanation = gradeSheet.publicationExplanation();
                if ((explanation == null && currentExplanation != null && !currentExplanation.isBlank())
                        || (explanation != null && (currentExplanation == null || currentExplanation.isBlank()))) {
                    gradeSheetDAO.updatePublicationExplanation(connection, gradeSheet.id(), explanation);
                    return gradeSheetDAO.findById(connection, gradeSheetId)
                            .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
                }
            }
            return gradeSheet;
        }
        if (gradeSheet.state() == GradeSheetState.PUBLISHED) {
            // Publication is dynamic while the class group is open: removing
            // a student, changing a weight, or losing a positive-weight grade
            // immediately returns the sheet to Draft.
            gradeSheetDAO.updateState(
                    connection,
                    gradeSheet.id(),
                    GradeSheetState.DRAFT,
                    null,
                    null
            );
            return gradeSheetDAO.findById(connection, gradeSheetId)
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
        }
        return gradeSheet;
    }

    boolean isComplete(Connection connection, GradeSheet gradeSheet) throws SQLException {
        if (isSubjectGradeSheet(gradeSheet)) {
            return subjectGradeSheetIsComplete(connection, gradeSheet);
        }
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        if (studentUserIds.isEmpty()) {
            return false;
        }
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (!weightsAreConfigured(assessmentWeights)) {
            return false;
        }
        List<Long> assessmentIds = positiveWeightAssessments(assessmentWeights).stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
        for (Long studentUserId : studentUserIds) {
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
        if (gradeSheet.state() == GradeSheetState.CLOSED || gradeSheet.state() == GradeSheetState.INACTIVE) {
            return gradeSheet;
        }
        if (isSubjectGradeSheet(gradeSheet)) {
            return recalculateSubjectGradeRecords(connection, gradeSheet);
        }
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        List<GradeAssessmentWeight> configuredAssessmentWeights = gradeSheetAssessmentWeights(
                connection,
                gradeSheet
        );
        if (studentUserIds.isEmpty() || !weightsAreConfigured(configuredAssessmentWeights)) {
            for (Long studentUserId : studentUserIds) {
                gradeRecordDAO.deactivateActiveBySheetAndStudent(connection, gradeSheet.id(), studentUserId);
            }
            return gradeSheet;
        }
        List<GradeAssessmentWeight> assessmentWeights = positiveWeightAssessments(configuredAssessmentWeights);
        for (Long studentUserId : studentUserIds) {
            Optional<BigDecimal> finalGrade = calculateFinalGrade(
                    connection,
                    gradeSheet,
                    assessmentWeights,
                    studentUserId
            );
            if (finalGrade.isEmpty()) {
                gradeRecordDAO.deactivateActiveBySheetAndStudent(connection, gradeSheet.id(), studentUserId);
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
            Long courseOccurrenceId,
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
                    courseOccurrenceId,
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

    private long createSubjectGradeSheetDraft(
            Connection connection,
            Subject subject,
            long courseOccurrenceId
    ) {
        try {
            long gradeSheetId = gradeSheetDAO.create(connection, new GradeSheetCreateCommand(
                    subject.id(),
                    courseOccurrenceId,
                    "Grade sheet - " + subject.name(),
                    GradeSheetType.FINAL,
                    maxGrade(subject),
                    passingGrade(subject),
                    GradeSheetState.DRAFT,
                    List.of(),
                    List.of()
            ));
            gradeSheetDAO.updateWeightAlert(connection, gradeSheetId, subjectConsolidationAlert());
            return gradeSheetId;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create automatic subject grade sheet", exception);
        }
    }

    private GradeSheet recalculateSubjectGradeRecords(Connection connection, GradeSheet subjectGradeSheet)
            throws SQLException {
        if (!subjectGradeSheet.assessmentWeights().isEmpty()) {
            gradeSheetDAO.replaceAssessmentWeights(connection, subjectGradeSheet.id(), List.of());
        }
        gradeSheetDAO.updateWeightAlert(connection, subjectGradeSheet.id(), subjectConsolidationAlert());

        Map<Long, GradeRecord> sourceRecordsByStudent = publishedClassGroupRecordsByStudent(
                connection,
                subjectGradeSheet
        );
        Map<Long, GradeRecord> existingRecordsByStudent = new LinkedHashMap<>();
        for (GradeRecord record : gradeRecordDAO.findByGradeSheet(connection, subjectGradeSheet.id())) {
            existingRecordsByStudent.putIfAbsent(record.studentUserId(), record);
        }
        for (Map.Entry<Long, GradeRecord> source : sourceRecordsByStudent.entrySet()) {
            BigDecimal sourceMaximum = sourceGradeSheetMaximum(
                    connection,
                    subjectGradeSheet,
                    source.getValue().gradeSheetId()
            );
            BigDecimal consolidatedValue = source.getValue().value()
                    .divide(sourceMaximum, 8, RoundingMode.HALF_UP)
                    .multiply(subjectGradeSheet.maxGrade())
                    .setScale(2, RoundingMode.HALF_UP);
            GradeRecordResult result = GradeRecordService.calculateResult(
                    consolidatedValue,
                    subjectGradeSheet.passingGrade()
            );
            gradeRecordDAO.upsertAutomatic(
                    connection,
                    subjectGradeSheet.id(),
                    source.getKey(),
                    consolidatedValue,
                    result,
                    LocalDateTime.now(clock),
                    "Automatically consolidated from the published class group grade sheet."
            );
        }
        for (Long studentUserId : existingRecordsByStudent.keySet()) {
            if (!sourceRecordsByStudent.containsKey(studentUserId)) {
                gradeRecordDAO.deactivateActiveBySheetAndStudent(
                        connection,
                        subjectGradeSheet.id(),
                        studentUserId,
                        "Marked inactive because the class group grade sheet is not published."
                );
            }
        }
        return gradeSheetDAO.findById(connection, subjectGradeSheet.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Grade sheet not found: " + subjectGradeSheet.id()
                ));
    }

    private GradeSheet synchronizeSubjectGradeSheetState(Connection connection, GradeSheet subjectGradeSheet)
            throws SQLException {
        boolean complete = subjectGradeSheetIsComplete(connection, subjectGradeSheet);
        if (complete && subjectGradeSheet.state() == GradeSheetState.DRAFT) {
            gradeSheetDAO.updateState(
                    connection,
                    subjectGradeSheet.id(),
                    GradeSheetState.PUBLISHED,
                    LocalDateTime.now(clock)
            );
        } else if (complete
                && subjectGradeSheet.state() == GradeSheetState.PUBLISHED
                && subjectGradeSheet.publicationExplanation() != null
                && !subjectGradeSheet.publicationExplanation().isBlank()) {
            gradeSheetDAO.updatePublicationExplanation(connection, subjectGradeSheet.id(), null);
        } else if (!complete && subjectGradeSheet.state() == GradeSheetState.PUBLISHED) {
            gradeSheetDAO.updateState(
                    connection,
                    subjectGradeSheet.id(),
                    GradeSheetState.DRAFT,
                    null,
                    null
            );
        }
        return gradeSheetDAO.findById(connection, subjectGradeSheet.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Grade sheet not found: " + subjectGradeSheet.id()
                ));
    }

    private boolean subjectGradeSheetIsComplete(Connection connection, GradeSheet subjectGradeSheet)
            throws SQLException {
        List<GradeSheet> classGroupGradeSheets = gradeSheetDAO.findClassGroupGradeSheetsForSubjectOccurrence(
                connection,
                subjectGradeSheet.subjectId(),
                subjectGradeSheet.courseOccurrenceId()
        );
        if (classGroupGradeSheets.isEmpty()
                || hasUnavailableClassGroupGradeSheet(classGroupGradeSheets)) {
            return false;
        }
        // The occurrence sheet is an aggregator: its publication depends on
        // every source class-group sheet being published, not on whether each
        // source student already has a final value. Missing values remain '-'.
        return true;
    }

    private Map<Long, GradeRecord> publishedClassGroupRecordsByStudent(
            Connection connection,
            GradeSheet subjectGradeSheet
    ) throws SQLException {
        List<GradeSheet> classGroupGradeSheets = gradeSheetDAO.findClassGroupGradeSheetsForSubjectOccurrence(
                connection,
                subjectGradeSheet.subjectId(),
                subjectGradeSheet.courseOccurrenceId()
        );
        if (classGroupGradeSheets.isEmpty()
                || hasUnavailableClassGroupGradeSheet(classGroupGradeSheets)) {
            return Map.of();
        }
        Map<Long, GradeRecord> recordsByStudent = new LinkedHashMap<>();
        Map<Long, BigDecimal> sourceMaximums = new LinkedHashMap<>();
        for (GradeSheet classGroupGradeSheet : classGroupGradeSheets) {
            BigDecimal sourceMaximum = classGroupGradeSheet.maxGrade();
            if (sourceMaximum == null || sourceMaximum.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            sourceMaximums.put(classGroupGradeSheet.id(), sourceMaximum);
            for (GradeRecord record : gradeRecordDAO.findByGradeSheet(connection, classGroupGradeSheet.id())) {
                if (!record.state().toDatabaseValue().equals("published") || record.value() == null) {
                    continue;
                }
                GradeRecord previous = recordsByStudent.get(record.studentUserId());
                if (previous == null
                        || isHigherClassification(record, previous, sourceMaximums, connection)) {
                    recordsByStudent.put(record.studentUserId(), record);
                }
            }
        }
        return Map.copyOf(recordsByStudent);
    }

    private boolean isHigherClassification(
            GradeRecord candidate,
            GradeRecord current,
            Map<Long, BigDecimal> sourceMaximums,
            Connection connection
    ) throws SQLException {
        BigDecimal candidateMaximum = sourceMaximums.computeIfAbsent(
                candidate.gradeSheetId(),
                id -> {
                    try {
                        return gradeSheetDAO.findById(connection, id)
                                .map(GradeSheet::maxGrade)
                                .orElse(BigDecimal.ZERO);
                    } catch (SQLException exception) {
                        throw new GradeSheetSynchronizationException(exception);
                    }
                }
        );
        BigDecimal currentMaximum = sourceMaximums.computeIfAbsent(
                current.gradeSheetId(),
                id -> {
                    try {
                        return gradeSheetDAO.findById(connection, id)
                                .map(GradeSheet::maxGrade)
                                .orElse(BigDecimal.ZERO);
                    } catch (SQLException exception) {
                        throw new GradeSheetSynchronizationException(exception);
                    }
                }
        );
        BigDecimal candidateClassification = normalizedClassification(candidate.value(), candidateMaximum);
        BigDecimal currentClassification = normalizedClassification(current.value(), currentMaximum);
        int comparison = candidateClassification.compareTo(currentClassification);
        if (comparison != 0) {
            return comparison > 0;
        }
        return isAfter(current, candidate);
    }

    private static BigDecimal normalizedClassification(BigDecimal value, BigDecimal maximum) {
        if (value == null || maximum == null || maximum.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(maximum, 8, RoundingMode.HALF_UP);
    }

    private static boolean isAfter(GradeRecord current, GradeRecord candidate) {
        if (candidate.recordedAt() == null) {
            return false;
        }
        if (current.recordedAt() == null) {
            return true;
        }
        return candidate.recordedAt().isAfter(current.recordedAt())
                || (candidate.recordedAt().isEqual(current.recordedAt()) && candidate.id() > current.id());
    }

    private BigDecimal sourceGradeSheetMaximum(
            Connection connection,
            GradeSheet subjectGradeSheet,
            long sourceGradeSheetId
    ) throws SQLException {
        return gradeSheetDAO.findById(connection, sourceGradeSheetId)
                .filter(GradeLifecycleService::isClassGroupGradeSheet)
                .filter(sheet -> sheet.subjectId() == subjectGradeSheet.subjectId())
                .filter(sheet -> sheet.courseOccurrenceId() == subjectGradeSheet.courseOccurrenceId())
                .map(GradeSheet::maxGrade)
                .filter(maximum -> maximum != null && maximum.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalStateException("Class group grade sheet scale is invalid"));
    }

    private static boolean isSubjectGradeSheet(GradeSheet gradeSheet) {
        return gradeSheet.classGroupIds().isEmpty();
    }

    private static boolean isClassGroupGradeSheet(GradeSheet gradeSheet) {
        return !isSubjectGradeSheet(gradeSheet);
    }

    private static boolean hasUnavailableClassGroupGradeSheet(List<GradeSheet> classGroupGradeSheets) {
        for (GradeSheet classGroupGradeSheet : classGroupGradeSheets) {
            if (classGroupGradeSheet.state() != GradeSheetState.PUBLISHED) {
                return true;
            }
        }
        return false;
    }

    private String publicationExplanation(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<String> reasons = isSubjectGradeSheet(gradeSheet)
                ? subjectPublicationReasons(connection, gradeSheet)
                : classGroupPublicationReasons(connection, gradeSheet);
        if (reasons.isEmpty()) {
            reasons = List.of("one or more final grades are still pending");
        }
        return "Published automatically because the associated course occurrence period is completed. "
                + String.join(" ", reasons)
                + " Missing grade values are displayed as '-'.";
    }

    private List<String> classGroupPublicationReasons(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<String> reasons = new ArrayList<>();
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        if (studentUserIds.isEmpty()) {
            reasons.add("There are no enrolled students with a final grade record yet.");
        }

        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (assessmentWeights.isEmpty()) {
            reasons.add("No assessments are configured for this grade sheet.");
            return List.copyOf(reasons);
        }
        if (!weightsAreConfigured(assessmentWeights)) {
            reasons.add("Assessment weights are not configured to a total of 100%.");
        }

        List<Long> assessmentIds = positiveWeightAssessments(assessmentWeights).stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
        boolean hasMissingAssessmentScore = false;
        boolean hasMissingFinalGrade = false;
        for (Long studentUserId : studentUserIds) {
            if (!gradeSheetDAO.hasActiveGradeRecord(connection, gradeSheet.id(), studentUserId)) {
                hasMissingFinalGrade = true;
            }
            for (Long assessmentId : assessmentIds) {
                if (!gradeSheetDAO.hasCorrectedAssessmentScore(connection, studentUserId, assessmentId)) {
                    hasMissingAssessmentScore = true;
                    break;
                }
            }
        }
        if (hasMissingAssessmentScore) {
            reasons.add("One or more assessment grades are missing or awaiting correction.");
        }
        if (hasMissingFinalGrade && !hasMissingAssessmentScore) {
            reasons.add("One or more final grades cannot yet be calculated.");
        }
        return List.copyOf(reasons);
    }

    private List<String> subjectPublicationReasons(Connection connection, GradeSheet subjectGradeSheet)
            throws SQLException {
        List<GradeSheet> classGroupGradeSheets = gradeSheetDAO.findClassGroupGradeSheetsForSubjectOccurrence(
                connection,
                subjectGradeSheet.subjectId(),
                subjectGradeSheet.courseOccurrenceId()
        );
        if (classGroupGradeSheets.isEmpty()) {
            return List.of("There are no class group grade sheets available for consolidation.");
        }
        long incompleteSheets = classGroupGradeSheets.stream()
                .filter(sheet -> sheet.state() != GradeSheetState.PUBLISHED)
                .count();
        List<String> reasons = new ArrayList<>();
        if (incompleteSheets > 0) {
            reasons.add(incompleteSheets + " class group grade sheet"
                    + (incompleteSheets == 1 ? " still has" : "s still have")
                    + " pending grades or assessment weights.");
        }
        if (publishedClassGroupRecordsByStudent(connection, subjectGradeSheet).isEmpty()) {
            reasons.add("No completed class group grade records are available for consolidation.");
        }
        return List.copyOf(reasons);
    }

    private static String subjectConsolidationAlert() {
        return "This subject grade sheet is automatically consolidated from the published class group grade sheets.";
    }

    private static final class GradeSheetSynchronizationException extends RuntimeException {
        private GradeSheetSynchronizationException(SQLException cause) {
            super(cause);
        }
    }

    private GradeSheet normalizeWeightsAfterClassGroupEnd(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        List<GradeAssessmentWeight> assessmentWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (weightsAreConfigured(assessmentWeights)
                || !classGroupCompletionReached(connection, gradeSheet)) {
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
                "Assessment weights were automatically redistributed equally because the class group was completed so the sum is 100%."
        );
        return gradeSheetDAO.findById(connection, gradeSheet.id())
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheet.id()));
    }

    private boolean classGroupCompletionReached(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        return gradeSheetDAO.allClassGroupsCompleted(connection, gradeSheet.classGroupIds())
                || gradeSheetDAO.classGroupsEndedBefore(
                        connection,
                        gradeSheet.classGroupIds(),
                        LocalDate.now(clock)
                );
    }

    private List<Long> gradeSheetAssessmentIds(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<Long> contextAssessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                gradeSheet.subjectId(),
                gradeSheet.courseOccurrenceId(),
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
                gradeSheet.courseOccurrenceId(),
                gradeSheet.classGroupIds()
        );
        if (contextAssessmentIds.isEmpty()) {
            return isClassGroupGradeSheet(gradeSheet) ? List.of() : gradeSheet.assessmentWeights();
        }
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (Long assessmentId : contextAssessmentIds) {
            weights.add(new GradeAssessmentWeight(assessmentId, configuredWeights.get(assessmentId)));
        }
        return List.copyOf(weights);
    }

    private GradeSheet synchronizeAssessmentWeightTopology(
            Connection connection,
            GradeSheet gradeSheet
    ) throws SQLException {
        if (!isClassGroupGradeSheet(gradeSheet)) {
            return gradeSheet;
        }
        List<GradeAssessmentWeight> contextWeights = gradeSheetAssessmentWeights(connection, gradeSheet);
        if (contextWeights.stream().anyMatch(weight -> weight == null || weight.weight() == null)) {
            // A newly applicable assessment may not have a persisted weight
            // yet.  Keep the missing value in the lifecycle view as Draft, but
            // never write a NULL into based_on_assessment (the column is NOT
            // NULL); the assessment create/update flow will upsert its weight.
            return gradeSheet;
        }
        if (!contextWeights.equals(gradeSheet.assessmentWeights())) {
            gradeSheetDAO.replaceAssessmentWeights(connection, gradeSheet.id(), contextWeights);
            gradeSheetDAO.updateWeightAlert(connection, gradeSheet.id(), alertForTotal(contextWeights));
            return gradeSheetDAO.findById(connection, gradeSheet.id())
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheet.id()));
        }
        return gradeSheet;
    }

    private static List<GradeAssessmentWeight> positiveWeightAssessments(
            List<GradeAssessmentWeight> assessmentWeights
    ) {
        if (assessmentWeights == null || assessmentWeights.isEmpty()) {
            return List.of();
        }
        return assessmentWeights.stream()
                .filter(weight -> weight != null
                        && weight.weight() != null
                        && weight.weight().compareTo(BigDecimal.ZERO) > 0)
                .toList();
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
                + "%. The grade sheet remains Draft until the total reaches 100%; weights are redistributed equally "
                + "only when the class group is completed.";
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
