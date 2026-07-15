package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.CourseOccurrenceDAO;
import pt.isel.gape.learning.dao.LessonDAO;

/**
 * Keeps persisted academic lifecycle data aligned with its concrete time
 * context.  It is used after bootstrap and migrations and by the application
 * scheduler, so a state cannot remain stale merely because no user edited the
 * record on the day its period changed.
 */
public final class AcademicLifecycleSynchronizationService {

    private final ConnectionProvider connectionProvider;
    private final Clock clock;
    private final CourseOccurrenceDAO courseOccurrenceDAO;
    private final ClassGroupDAO classGroupDAO;
    private final LessonDAO lessonDAO;
    private final AssessmentDAO assessmentDAO;
    private final GradeSheetService gradeSheetService;

    public AcademicLifecycleSynchronizationService(ConnectionProvider connectionProvider, Clock clock) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.courseOccurrenceDAO = new CourseOccurrenceDAO(connectionProvider);
        this.classGroupDAO = new ClassGroupDAO(connectionProvider);
        this.lessonDAO = new LessonDAO(connectionProvider);
        this.assessmentDAO = new AssessmentDAO(connectionProvider);
        this.gradeSheetService = new GradeSheetService(connectionProvider, clock);
    }

    public void synchronize() {
        synchronized (AcademicLifecycleSynchronizationLock.monitor()) {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronize(connection);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            } catch (RuntimeException | SQLException exception) {
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Failed to synchronize academic lifecycle data", exception);
            }
        }
    }

    /**
     * Connection-scoped variant for bootstrap and migration transactions.
     */
    public void synchronize(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection is required");
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        courseOccurrenceDAO.synchronizeTemporalStates(connection, today);
        classGroupDAO.synchronizeTemporalStates(connection, today);
        lessonDAO.synchronizeTemporalStates(connection, now);
        assessmentDAO.synchronizeTemporalStates(connection, now);
        synchronizeDependentEnrollments(connection);
        gradeSheetService.synchronizeGradeSheetConformance(connection);
    }

    private static void synchronizeDependentEnrollments(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE enroll_course enrollment
                    JOIN course_occurrence occurrence
                      ON occurrence.id_course_occurrence = enrollment.id_course_occurrence
                    SET enrollment.state = 'completed',
                        enrollment.end_date = CASE
                            WHEN enrollment.end_date IS NULL OR enrollment.end_date > occurrence.ends_at
                                THEN occurrence.ends_at
                            ELSE enrollment.end_date
                        END
                    WHERE enrollment.state = 'active'
                      AND occurrence.state = 'completed'
                    """);
            statement.executeUpdate("""
                    UPDATE teach_class_group teaching
                    JOIN class_group class_group_row
                      ON class_group_row.id_class_group = teaching.id_class_group
                    SET teaching.state = 'inactive',
                        teaching.end_date = CASE
                            WHEN teaching.end_date IS NULL OR teaching.end_date > class_group_row.ends_at
                                THEN class_group_row.ends_at
                            ELSE teaching.end_date
                        END
                    WHERE teaching.state = 'active'
                      AND class_group_row.state = 'completed'
                    """);
            statement.executeUpdate("""
                    UPDATE enroll_assessment enrollment
                    JOIN assessment assessment_row
                      ON assessment_row.id_assessment = enrollment.id_assessment
                    SET enrollment.state = 'completed',
                        enrollment.end_date = CASE
                            WHEN enrollment.end_date IS NULL
                                 OR enrollment.end_date > DATE(assessment_row.available_until)
                                THEN DATE(assessment_row.available_until)
                            ELSE enrollment.end_date
                        END
                    WHERE enrollment.state = 'active'
                      AND assessment_row.state = 'completed'
                      AND assessment_row.available_until IS NOT NULL
                    """);
        }
        synchronizeClassGroupEnrollmentLifecycle(connection);
    }

    /**
     * Keeps class-group enrollment states actionable as their owning occurrence
     * concludes. A request cannot remain pending after the class group is
     * completed because no approval action can legitimately be taken then.
     */
    static int synchronizeClassGroupEnrollmentLifecycle(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            int completedEnrollments = statement.executeUpdate("""
                    UPDATE enroll_class_group enrollment
                    JOIN class_group class_group_row
                      ON class_group_row.id_class_group = enrollment.id_class_group
                    SET enrollment.state = 'completed',
                        enrollment.end_date = CASE
                            WHEN enrollment.end_date IS NULL OR enrollment.end_date > class_group_row.ends_at
                                THEN class_group_row.ends_at
                            ELSE enrollment.end_date
                        END
                    WHERE enrollment.state = 'active'
                      AND class_group_row.state = 'completed'
                    """);
            int rejectedRequests = statement.executeUpdate("""
                    UPDATE enroll_class_group enrollment
                    JOIN class_group class_group_row
                      ON class_group_row.id_class_group = enrollment.id_class_group
                    SET enrollment.state = 'rejected',
                        enrollment.end_date = class_group_row.ends_at
                    WHERE enrollment.state = 'pending'
                      AND class_group_row.state = 'completed'
                    """);
            return completedEnrollments + rejectedRequests;
        }
    }
}
