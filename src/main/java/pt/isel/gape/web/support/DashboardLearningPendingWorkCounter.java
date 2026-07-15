package pt.isel.gape.web.support;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;

/**
 * Counts assessment enrollment requests and submitted attempts that the
 * current dashboard user can act on.
 */
public final class DashboardLearningPendingWorkCounter {

    private final ApplicationReadService readService;
    private final AssessmentService assessmentService;

    public DashboardLearningPendingWorkCounter() {
        this(ConnectionProvider.defaultProvider());
    }

    public DashboardLearningPendingWorkCounter(ConnectionProvider connectionProvider) {
        this(
                new ApplicationReadService(Objects.requireNonNull(connectionProvider, "connectionProvider is required")),
                new AssessmentService(connectionProvider, ApplicationClock.system())
        );
    }

    DashboardLearningPendingWorkCounter(
            ApplicationReadService readService,
            AssessmentService assessmentService
    ) {
        this.readService = Objects.requireNonNull(readService, "readService is required");
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService is required");
    }

    public int countPendingWork(HttpServletRequest request, SessionUser sessionUser, Long sessionId) {
        try {
            AccessProfileType profile = sessionUser.primaryProfileType()
                    .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
            assessmentService.synchronizeTemporalStates();
            List<Long> managedAssessmentIds = readService.assessments().findAll().stream()
                    .filter(assessment -> canManage(request, sessionUser, sessionId, profile, assessment))
                    .map(Assessment::id)
                    .toList();
            if (managedAssessmentIds.isEmpty()) {
                return 0;
            }
            Map<Long, Integer> pendingEnrollments = readService.assessmentEnrollments()
                    .countByAssessmentIdsAndState(managedAssessmentIds, EnrollmentState.PENDING);
            Map<Long, Integer> pendingCorrections = readService.attempts()
                    .countByAssessmentIdsAndState(managedAssessmentIds, AttemptState.SUBMITTED);
            return pendingEnrollments.values().stream().mapToInt(Integer::intValue).sum()
                    + pendingCorrections.values().stream().mapToInt(Integer::intValue).sum();
        } catch (SQLException | RuntimeException exception) {
            return 0;
        }
    }

    private boolean canManage(
            HttpServletRequest request,
            SessionUser sessionUser,
            Long sessionId,
            AccessProfileType profile,
            Assessment assessment
    ) {
        return assessmentService.canManageAssessment(
                sessionUser.userId(),
                sessionId,
                profile,
                assessment.id(),
                request.getRemoteAddr()
        );
    }
}
