package pt.isel.gape.web.support;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;

public final class DashboardEventUnreadCounter {

    private final ConnectionProvider connectionProvider;
    private final ApplicationReadService readService;

    public DashboardEventUnreadCounter() {
        this(ConnectionProvider.defaultProvider());
    }

    public DashboardEventUnreadCounter(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.readService = new ApplicationReadService(this.connectionProvider);
    }

    public int countUnread(HttpServletRequest request, SessionUser sessionUser, Long sessionId) {
        try {
            AccessProfileType profile = sessionUser.primaryProfileType()
                    .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
            List<ClassGroup> visibleClassGroups = visibleClassGroupsForEvents(request, sessionUser, profile, sessionId);
            return readService.learningEvents().countUnreadVisible(
                    visibleClassGroups.stream().map(ClassGroup::id).distinct().toList(),
                    visibleClassGroups.stream().map(ClassGroup::courseId).distinct().toList(),
                    visibleClassGroups.stream().map(ClassGroup::subjectId).distinct().toList(),
                    profile == AccessProfileType.STUDENT ? sessionUser.userId() : null,
                    profile == AccessProfileType.STUDENT,
                    profile == AccessProfileType.ADMINISTRATOR,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now(ApplicationClock.system()),
                    sessionUser.userId()
            );
        } catch (SQLException | RuntimeException exception) {
            return 0;
        }
    }

    private List<ClassGroup> visibleClassGroupsForEvents(
            HttpServletRequest request,
            SessionUser sessionUser,
            AccessProfileType profile,
            Long sessionId
    ) throws SQLException {
        ClassGroupService classGroupService = new ClassGroupService(connectionProvider, ApplicationClock.system());
        return readService.classGroups().findAll().stream()
                .filter(classGroup -> canReadClassGroupForEvents(
                        request,
                        sessionUser,
                        profile,
                        sessionId,
                        classGroupService,
                        classGroup.id()
                ))
                .toList();
    }

    private static boolean canReadClassGroupForEvents(
            HttpServletRequest request,
            SessionUser sessionUser,
            AccessProfileType profile,
            Long sessionId,
            ClassGroupService classGroupService,
            long classGroupId
    ) {
        try {
            return classGroupService.canReadClassGroup(
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
