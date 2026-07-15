package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.GradeRecordResult;
import pt.isel.gape.learning.model.GradeRecordState;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.security.authorization.PermissionChecker;

public final class GradeRecordService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final ConnectionProvider connectionProvider;
    private final GradeRecordDAO gradeRecordDAO;
    private final GradeSheetDAO gradeSheetDAO;
    private final GradeAccessPolicy accessPolicy;

    public GradeRecordService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new GradeRecordDAO(connectionProvider),
                new GradeSheetDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public GradeRecordService(
            ConnectionProvider connectionProvider,
            GradeRecordDAO gradeRecordDAO,
            GradeSheetDAO gradeSheetDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.gradeRecordDAO = Objects.requireNonNull(gradeRecordDAO, "gradeRecordDAO is required");
        this.gradeSheetDAO = Objects.requireNonNull(gradeSheetDAO, "gradeSheetDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new GradeAccessPolicy(
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
    }

    public GradeRecord getGradeRecord(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long gradeRecordId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            GradeRecord record = requireRecord(connection, gradeRecordId);
            GradeSheet gradeSheet = gradeSheetDAO.findById(connection, record.gradeSheetId())
                    .orElseThrow(() -> new IllegalArgumentException("Grade sheet not found: " + record.gradeSheetId()));
            if (actorProfileType == AccessProfileType.STUDENT) {
                accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                if (record.studentUserId() != actorUserId) {
                    throw new SecurityException("Students can only access their own grade records");
                }
                if (record.state() != GradeRecordState.PUBLISHED
                        || !gradeSheetCompleteForVisibility(connection, gradeSheet)) {
                    throw new SecurityException("Students can only access published grade records");
                }
                return record;
            }
            accessPolicy.requireGradeSheetManager(connection, actorUserId, sessionId, actorProfileType, gradeSheet, sourceIp);
            return record;
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read grade record");
        }
    }

    public List<GradeRecord> listOwnGradeRecords(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            List<GradeRecord> visible = new ArrayList<>();
            for (GradeRecord record : gradeRecordDAO.findByStudent(connection, actorUserId)) {
                GradeSheet gradeSheet = gradeSheetDAO.findById(connection, record.gradeSheetId())
                        .orElse(null);
                if (record.state() == GradeRecordState.PUBLISHED
                        && gradeSheet != null
                        && gradeSheetCompleteForVisibility(connection, gradeSheet)) {
                    visible.add(record);
                }
            }
            return List.copyOf(visible);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list student grade records");
        }
    }

    public static GradeRecordResult calculateResult(BigDecimal value, BigDecimal passingGrade) {
        Objects.requireNonNull(value, "grade value is required");
        Objects.requireNonNull(passingGrade, "passing grade is required");
        return value.compareTo(passingGrade) >= 0 ? GradeRecordResult.APPROVED : GradeRecordResult.FAILED;
    }

    private GradeRecord requireRecord(Connection connection, long recordId) throws SQLException {
        return gradeRecordDAO.findById(connection, recordId)
                .orElseThrow(() -> new IllegalArgumentException("Grade record not found: " + recordId));
    }

    private boolean gradeSheetCompleteForVisibility(Connection connection, GradeSheet gradeSheet) throws SQLException {
        if (!gradeSheet.state().blocksDirectChanges()) {
            return false;
        }
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

    private List<GradeAssessmentWeight> gradeSheetAssessmentWeights(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        Map<Long, BigDecimal> configuredWeights = new LinkedHashMap<>();
        for (GradeAssessmentWeight weight : gradeSheet.assessmentWeights()) {
            configuredWeights.putIfAbsent(weight.assessmentId(), weight.weight());
        }
        List<Long> assessmentIds = gradeSheetDAO.findAssessmentIdsForSheetContext(
                connection,
                gradeSheet.subjectId(),
                gradeSheet.courseOccurrenceId(),
                gradeSheet.classGroupIds()
        );
        if (assessmentIds.isEmpty()) {
            return gradeSheet.assessmentWeights();
        }
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (Long assessmentId : assessmentIds) {
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

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
