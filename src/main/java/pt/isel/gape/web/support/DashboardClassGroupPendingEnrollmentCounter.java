package pt.isel.gape.web.support;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;

/**
 * Counts event work that is actionable by the current dashboard user.
 *
 * <p>Unlike the Events badge, this is not a read/unread concept: it exposes
 * pending enrollment requests and submitted attempts awaiting correction for
 * class groups the user can actually manage.</p>
 */
public final class DashboardClassGroupPendingEnrollmentCounter {

    private final ConnectionProvider connectionProvider;
    private final ApplicationReadService readService;

    public DashboardClassGroupPendingEnrollmentCounter() {
        this(ConnectionProvider.defaultProvider());
    }

    public DashboardClassGroupPendingEnrollmentCounter(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.readService = new ApplicationReadService(this.connectionProvider);
    }

    public int countPendingWork(HttpServletRequest request, SessionUser sessionUser, Long sessionId) {
        try {
            AccessProfileType profile = sessionUser.primaryProfileType()
                    .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
            List<ClassGroup> classGroups = readService.classGroups().findAll();
            ClassGroupService classGroupService = new ClassGroupService(connectionProvider, ApplicationClock.system());
            List<ClassGroup> manageableClassGroups = classGroups.stream()
                    .filter(classGroup -> canManagePendingApprovals(
                            request,
                            sessionUser,
                            profile,
                            sessionId,
                            classGroupService,
                            classGroup.id()
                    ))
                    .toList();
            if (manageableClassGroups.isEmpty()) {
                return 0;
            }
            List<Long> manageableIds = manageableClassGroups.stream().map(ClassGroup::id).toList();
            Map<Long, Integer> pendingCounts = readService.classGroupEnrollments()
                    .countPendingByClassGroupIds(manageableIds);
            int submittedAttemptCount = readService.assessments()
                    .countDistinctSubmittedAttemptsByClassGroupIds(manageableIds);

            int pendingEnrollmentCount = manageableClassGroups.stream()
                    .mapToInt(classGroup -> pendingCounts.getOrDefault(classGroup.id(), 0))
                    .sum();
            return pendingEnrollmentCount + submittedAttemptCount;
        } catch (SQLException | RuntimeException exception) {
            return 0;
        }
    }

    /**
     * Retained for callers compiled against the original dashboard counter.
     * The badge now represents all actionable class-group events.
     */
    public int countPendingApprovals(HttpServletRequest request, SessionUser sessionUser, Long sessionId) {
        return countPendingWork(request, sessionUser, sessionId);
    }

    private static boolean canManagePendingApprovals(
            HttpServletRequest request,
            SessionUser sessionUser,
            AccessProfileType profile,
            Long sessionId,
            ClassGroupService classGroupService,
            long classGroupId
    ) {
        try {
            return classGroupService.canManageClassGroupEnrollments(
                    sessionUser.userId(),
                    sessionId,
                    profile,
                    classGroupId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
