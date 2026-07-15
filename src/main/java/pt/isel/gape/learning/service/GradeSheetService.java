package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.GradeSheetUpdateCommand;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class GradeSheetService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final ConnectionProvider connectionProvider;
    private final GradeSheetDAO gradeSheetDAO;
    private final GradeLifecycleService gradeLifecycleService;
    private final GradeAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public GradeSheetService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new GradeSheetDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public GradeSheetService(
            ConnectionProvider connectionProvider,
            GradeSheetDAO gradeSheetDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.gradeSheetDAO = Objects.requireNonNull(gradeSheetDAO, "gradeSheetDAO is required");
        this.gradeLifecycleService = new GradeLifecycleService(connectionProvider, clock);
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new GradeAccessPolicy(
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public GradeSheet createGradeSheet(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            GradeSheetCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    NormalizedWeights normalizedWeights = normalizeAssessmentWeights(
                            connection,
                            command.subjectId(),
                            command.classGroupIds(),
                            command.assessmentWeights()
                    );
                    validateRepositoryConsistency(connection, command.subjectId(), command.classGroupIds(),
                            normalizedWeights.assessmentWeights());
                    validateFinalSourceTopology(
                            connection,
                            command.type(),
                            command.classGroupIds(),
                            null
                    );
                    accessPolicy.requireGradeSheetManager(connection, actorUserId, sessionId, actorProfileType,
                            transientSheet(command, normalizedWeights.assessmentWeights(), normalizedWeights.alert()), sourceIp);
                    long gradeSheetId = gradeSheetDAO.create(connection, command);
                    gradeSheetDAO.replaceClassGroups(connection, gradeSheetId, command.classGroupIds());
                    gradeSheetDAO.replaceAssessmentWeights(connection, gradeSheetId, normalizedWeights.assessmentWeights());
                    gradeSheetDAO.updateWeightAlert(connection, gradeSheetId, normalizedWeights.alert());
                    gradeLifecycleService.synchronizeGradeSheetAndCertificates(connection, gradeSheetId);
                    auditService.record(connection, actorUserId, sessionId, "GRADE_SHEET_CREATE",
                            "grade_sheet", Long.toString(gradeSheetId), "success", sourceIp);
                    GradeSheet gradeSheet = requireGradeSheet(connection, gradeSheetId);
                    connection.commit();
                    return gradeSheet;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "GRADE_SHEET_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create grade sheet");
        }
    }

    public GradeSheet updateGradeSheet(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long gradeSheetId,
            GradeSheetUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    GradeSheet existing = gradeSheetDAO.lockById(connection, gradeSheetId)
                            .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
                    if (existing.classGroupIds().isEmpty()) {
                        throw new IllegalStateException(
                                "Subject-occurrence grade sheets are consolidated automatically and cannot be edited directly"
                        );
                    }
                    accessPolicy.requireGradeSheetManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            existing,
                            sourceIp
                    );
                    NormalizedWeights normalizedWeights = validateSubmittedAssessmentWeights(
                            connection,
                            command.subjectId(),
                            command.classGroupIds(),
                            command.assessmentWeights()
                    );
                    validateRepositoryConsistency(connection, command.subjectId(), command.classGroupIds(),
                            normalizedWeights.assessmentWeights());
                    validateFinalSourceTopology(
                            connection,
                            command.type(),
                            command.classGroupIds(),
                            gradeSheetId
                    );
                    accessPolicy.requireGradeSheetManager(connection, actorUserId, sessionId, actorProfileType,
                            transientSheet(command, normalizedWeights.assessmentWeights(), normalizedWeights.alert()), sourceIp);
                    gradeSheetDAO.update(connection, gradeSheetId, command);
                    gradeSheetDAO.replaceClassGroups(connection, gradeSheetId, command.classGroupIds());
                    gradeSheetDAO.replaceAssessmentWeights(connection, gradeSheetId, normalizedWeights.assessmentWeights());
                    gradeSheetDAO.updateWeightAlert(connection, gradeSheetId, normalizedWeights.alert());
                    GradeSheet updated = gradeLifecycleService.synchronizeGradeSheetAndCertificates(
                            connection,
                            gradeSheetId
                    );
                    auditService.record(connection, actorUserId, sessionId, "GRADE_SHEET_UPDATE",
                            "grade_sheet", Long.toString(gradeSheetId), "success", sourceIp);
                    connection.commit();
                    return updated;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "GRADE_SHEET_UPDATE", Long.toString(gradeSheetId), sourceIp);
            throw wrap(exception, "Failed to update grade sheet");
        }
    }

    public GradeSheet publishGradeSheet(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long gradeSheetId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                GradeSheet gradeSheet = gradeSheetDAO.lockById(connection, gradeSheetId)
                        .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
                accessPolicy.requireGradeSheetManager(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        gradeSheet,
                        sourceIp
                );
                GradeSheet synchronizedSheet = gradeLifecycleService.synchronizeGradeSheetAndCertificates(
                        connection,
                        gradeSheetId
                );
                if (synchronizedSheet.state().blocksDirectChanges()) {
                    auditService.record(connection, actorUserId, sessionId, "GRADE_SHEET_SYNC",
                            "grade_sheet", Long.toString(gradeSheetId), "success", sourceIp);
                    connection.commit();
                    return synchronizedSheet;
                }
                if (!gradeSheetCompleteForPublication(connection, synchronizedSheet)) {
                    throw new IllegalStateException("Grade sheets with missing grades cannot be published");
                }
                GradeSheet published = gradeLifecycleService.synchronizeGradeSheetAndCertificates(
                        connection,
                        gradeSheetId
                );
                auditService.record(connection, actorUserId, sessionId, "GRADE_SHEET_SYNC",
                        "grade_sheet", Long.toString(gradeSheetId), "success", sourceIp);
                connection.commit();
                return published;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                auditFailure(actorUserId, sessionId, "GRADE_SHEET_SYNC", Long.toString(gradeSheetId), sourceIp);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to publish grade sheet");
        }
    }

    /**
     * Applies the completed-period publication rule.  This method deliberately
     * has no actor because it is a temporal data-conformance operation invoked
     * at startup and by the lifecycle scheduler.
     */
    public void synchronizeCompletedPeriodPublications() {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                synchronizeCompletedPeriodPublications(connection);
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to publish grade sheets for completed periods");
        }
    }

    /**
     * Connection-scoped variant used by bootstrap and migration conformance
     * passes, so seeded and migrated data cannot remain outside the rule.
     */
    public void synchronizeCompletedPeriodPublications(Connection connection) throws SQLException {
        gradeLifecycleService.synchronizeCompletedPeriodPublications(connection);
    }

    /**
     * Full data-conformance pass used after a seed reset or schema migration.
     * Complete sheets are normally published first; incomplete sheets in an
     * ended period are then published with their mandatory explanation.
     */
    public void synchronizeGradeSheetConformance(Connection connection) throws SQLException {
        gradeLifecycleService.synchronizeGradeSheetTopology(connection);
        gradeLifecycleService.synchronizeAllGradeSheetStates(connection);
        gradeLifecycleService.synchronizeCompletedPeriodPublications(connection);
    }

    public GradeSheet getGradeSheet(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long gradeSheetId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            GradeSheet gradeSheet = requireGradeSheet(connection, gradeSheetId);
            if (actorProfileType == AccessProfileType.STUDENT) {
                accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                throw new SecurityException("Students cannot access grade sheet definitions directly");
            }
            accessPolicy.requireGradeSheetManager(
                    connection,
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    gradeSheet,
                    sourceIp
            );
            // Reading a page must never recalculate grades, issue certificates
            // or write aggregate sheets.  Those lifecycle writes occur on the
            // corresponding mutation paths and in the centralized temporal
            // synchronizer, which prevents parallel page requests from
            // deadlocking while rendering Subject Details.
            return gradeSheet;
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read grade sheet");
        }
    }

    GradeSheet getGradeSheet(long gradeSheetId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return requireGradeSheet(connection, gradeSheetId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read grade sheet");
        }
    }

    private GradeSheet requireGradeSheet(Connection connection, long gradeSheetId) throws SQLException {
        return gradeSheetDAO.findById(connection, gradeSheetId)
                .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + gradeSheetId));
    }

    private boolean gradeSheetCompleteForPublication(Connection connection, GradeSheet gradeSheet) throws SQLException {
        List<Long> studentUserIds = gradeSheetDAO.findStudentUserIdsForSheetContext(connection, gradeSheet);
        if (studentUserIds.isEmpty()) {
            return false;
        }
        List<Long> assessmentIds = gradeSheetAssessmentIds(connection, gradeSheet);
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
        return idsFrom(gradeSheet.assessmentWeights());
    }

    private void validateRepositoryConsistency(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds,
            List<GradeAssessmentWeight> assessmentWeights
    ) throws SQLException {
        if (!gradeSheetDAO.subjectExists(connection, subjectId)) {
            throw new IllegalArgumentException("Subject not found or inactive: " + subjectId);
        }
        if (!gradeSheetDAO.classGroupsMatchSubject(connection, subjectId, classGroupIds)) {
            throw new IllegalArgumentException("Grade sheet class groups must belong to the subject");
        }
        if (!gradeSheetDAO.assessmentsMatchSubject(connection, subjectId, assessmentWeights)) {
            throw new IllegalArgumentException("Grade sheet assessments must belong to the subject");
        }
    }

    private void validateFinalSourceTopology(
            Connection connection,
            GradeSheetType type,
            List<Long> classGroupIds,
            Long excludedGradeSheetId
    ) throws SQLException {
        if (type != GradeSheetType.FINAL) {
            return;
        }
        List<Long> uniqueClassGroupIds = classGroupIds == null
                ? List.of()
                : new LinkedHashSet<>(classGroupIds).stream().toList();
        if (uniqueClassGroupIds.size() != 1) {
            throw new IllegalArgumentException("A final class-group grade sheet must belong to exactly one class group");
        }
        if (gradeSheetDAO.hasFinalClassGroupGradeSheet(connection, uniqueClassGroupIds, excludedGradeSheetId)) {
            throw new IllegalStateException("The class group already has its final grade sheet");
        }
    }

    private static void validateCreateCommand(GradeSheetCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.state() != GradeSheetState.DRAFT) {
            throw new IllegalArgumentException("New grade sheets must start as draft");
        }
        requireClassGroupScope(command.classGroupIds());
        validateCommon(command.subjectId(), command.title(), command.type(), command.maxGrade(),
                command.passingGrade(), command.assessmentWeights());
    }

    private static void validateUpdateCommand(GradeSheetUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        requireClassGroupScope(command.classGroupIds());
        validateCommon(command.subjectId(), command.title(), command.type(), command.maxGrade(),
                command.passingGrade(), command.assessmentWeights());
    }

    private static void validateCommon(
            long subjectId,
            String title,
            Object type,
            BigDecimal maxGrade,
            BigDecimal passingGrade,
            List<GradeAssessmentWeight> assessmentWeights
    ) {
        if (subjectId <= 0) {
            throw new IllegalArgumentException("Subject id must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Grade sheet title is required");
        }
        if (title.trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("Grade sheet title is too long");
        }
        Objects.requireNonNull(type, "grade sheet type is required");
        validateScale(maxGrade, passingGrade);
        validateAssessmentIds(assessmentWeights);
    }

    private static void requireClassGroupScope(List<Long> classGroupIds) {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Subject-occurrence grade sheets are created automatically with the first class group"
            );
        }
    }

    private static void validateScale(BigDecimal maxGrade, BigDecimal passingGrade) {
        Objects.requireNonNull(maxGrade, "grade sheet max grade is required");
        Objects.requireNonNull(passingGrade, "grade sheet passing grade is required");
        if (maxGrade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Grade sheet max grade must be greater than zero");
        }
        if (passingGrade.compareTo(BigDecimal.ZERO) < 0 || passingGrade.compareTo(maxGrade) > 0) {
            throw new IllegalArgumentException("Grade sheet passing grade must be within the scale");
        }
    }

    private static void validateAssessmentIds(List<GradeAssessmentWeight> assessmentWeights) {
        if (assessmentWeights == null || assessmentWeights.isEmpty()) {
            return;
        }
        for (GradeAssessmentWeight assessmentWeight : assessmentWeights) {
            if (assessmentWeight == null || assessmentWeight.assessmentId() <= 0) {
                throw new IllegalArgumentException("Assessment ids must be positive");
            }
        }
    }

    private NormalizedWeights normalizeAssessmentWeights(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds,
            List<GradeAssessmentWeight> submittedWeights
    ) throws SQLException {
        List<Long> contextAssessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                subjectId,
                classGroupIds
        );
        List<GradeAssessmentWeight> uniqueSubmitted = uniqueSubmittedWeights(submittedWeights);
        List<GradeAssessmentWeight> defaultWeights = gradeSheetDAO.findDefaultAssessmentWeightsForSheetContext(
                connection,
                subjectId,
                classGroupIds
        );
        if (contextAssessmentIds.isEmpty()) {
            if (uniqueSubmitted.isEmpty()) {
                return new NormalizedWeights(
                        List.of(),
                        "No assessments are configured for this class group yet. The grade sheet is incomplete."
                );
            }
            validateWeightValues(uniqueSubmitted);
            return new NormalizedWeights(uniqueSubmitted, alertForTotal(uniqueSubmitted));
        }
        List<GradeAssessmentWeight> effectiveWeights = new LinkedHashSet<>(idsFrom(uniqueSubmitted))
                .equals(new LinkedHashSet<>(contextAssessmentIds))
                ? orderByContext(uniqueSubmitted, contextAssessmentIds)
                : defaultWeights;
        validateWeightValues(effectiveWeights);
        return new NormalizedWeights(effectiveWeights, alertForTotal(effectiveWeights));
    }

    private NormalizedWeights validateSubmittedAssessmentWeights(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds,
            List<GradeAssessmentWeight> submittedWeights
    ) throws SQLException {
        List<Long> contextAssessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                subjectId,
                classGroupIds
        );
        List<GradeAssessmentWeight> uniqueSubmitted = uniqueSubmittedWeights(submittedWeights);
        List<Long> requiredAssessmentIds = contextAssessmentIds.isEmpty() ? idsFrom(uniqueSubmitted) : contextAssessmentIds;
        if (requiredAssessmentIds.isEmpty()) {
            if (uniqueSubmitted.isEmpty()) {
                return new NormalizedWeights(List.of(), null);
            }
            throw new IllegalArgumentException("Assessment weights cannot be configured without assessments");
        }
        if (!new LinkedHashSet<>(idsFrom(uniqueSubmitted)).equals(new LinkedHashSet<>(requiredAssessmentIds))) {
            throw new IllegalArgumentException("Assessment weights must include every class assessment");
        }
        List<GradeAssessmentWeight> orderedWeights = orderByContext(uniqueSubmitted, requiredAssessmentIds);
        validateWeightValues(orderedWeights);
        return new NormalizedWeights(orderedWeights, alertForTotal(orderedWeights));
    }

    private static void validateWeightValues(List<GradeAssessmentWeight> weights) {
        if (weights == null) {
            return;
        }
        for (GradeAssessmentWeight assessmentWeight : weights) {
            if (assessmentWeight.weight() == null
                    || assessmentWeight.weight().compareTo(BigDecimal.ZERO) < 0
                    || assessmentWeight.weight().compareTo(ONE_HUNDRED) > 0) {
                throw new IllegalArgumentException("Assessment weights must be numeric values between 0 and 100");
            }
        }
    }

    private static String alertForTotal(List<GradeAssessmentWeight> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
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

    private static List<GradeAssessmentWeight> equalWeights(List<Long> assessmentIds) {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return List.of();
        }
        BigDecimal base = ONE_HUNDRED.divide(BigDecimal.valueOf(assessmentIds.size()), 2, RoundingMode.DOWN);
        BigDecimal assigned = BigDecimal.ZERO;
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (int index = 0; index < assessmentIds.size(); index++) {
            BigDecimal weight = index == assessmentIds.size() - 1
                    ? ONE_HUNDRED.subtract(assigned)
                    : base;
            assigned = assigned.add(weight);
            weights.add(new GradeAssessmentWeight(assessmentIds.get(index), weight));
        }
        return List.copyOf(weights);
    }

    private static List<GradeAssessmentWeight> uniqueSubmittedWeights(List<GradeAssessmentWeight> submittedWeights) {
        if (submittedWeights == null || submittedWeights.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<GradeAssessmentWeight> unique = new ArrayList<>();
        for (GradeAssessmentWeight assessmentWeight : submittedWeights) {
            if (assessmentWeight == null || assessmentWeight.assessmentId() <= 0) {
                throw new IllegalArgumentException("Assessment ids must be positive");
            }
            if (seen.add(assessmentWeight.assessmentId())) {
                unique.add(assessmentWeight);
            }
        }
        return List.copyOf(unique);
    }

    private static List<Long> idsFrom(List<GradeAssessmentWeight> weights) {
        return weights.stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
    }

    private static List<GradeAssessmentWeight> orderByContext(
            List<GradeAssessmentWeight> weights,
            List<Long> assessmentIds
    ) {
        List<GradeAssessmentWeight> ordered = new ArrayList<>();
        for (Long assessmentId : assessmentIds) {
            for (GradeAssessmentWeight weight : weights) {
                if (weight.assessmentId() == assessmentId) {
                    ordered.add(weight);
                    break;
                }
            }
        }
        return List.copyOf(ordered);
    }

    private static GradeSheet transientSheet(
            GradeSheetCreateCommand command,
            List<GradeAssessmentWeight> assessmentWeights,
            String weightAlert
    ) {
        return new GradeSheet(
                0L,
                command.subjectId(),
                command.title(),
                command.type(),
                command.maxGrade(),
                command.passingGrade(),
                weightAlert,
                null,
                command.state(),
                command.classGroupIds() == null ? List.of() : command.classGroupIds(),
                assessmentWeights == null ? List.of() : assessmentWeights
        );
    }

    private static GradeSheet transientSheet(
            GradeSheetUpdateCommand command,
            List<GradeAssessmentWeight> assessmentWeights,
            String weightAlert
    ) {
        return new GradeSheet(
                0L,
                command.subjectId(),
                command.title(),
                command.type(),
                command.maxGrade(),
                command.passingGrade(),
                weightAlert,
                null,
                GradeSheetState.DRAFT,
                command.classGroupIds() == null ? List.of() : command.classGroupIds(),
                assessmentWeights == null ? List.of() : assessmentWeights
        );
    }

    private record NormalizedWeights(List<GradeAssessmentWeight> assessmentWeights, String alert) {
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "grade_sheet", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
