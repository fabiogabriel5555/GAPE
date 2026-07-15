package pt.isel.gape.web.filter;

import java.io.IOException;
import java.util.Objects;
import java.util.OptionalLong;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.CommunicationReadService;
import pt.isel.gape.web.support.DashboardEventUnreadCounter;
import pt.isel.gape.web.support.DashboardClassGroupPendingEnrollmentCounter;
import pt.isel.gape.web.support.DashboardLearningPendingWorkCounter;

public final class DashboardEventUnreadFilter implements Filter {

    private final SessionManager sessionManager;
    private final DashboardEventUnreadCounter counter;
    private final DashboardClassGroupPendingEnrollmentCounter classGroupPendingEnrollmentCounter;
    private final DashboardLearningPendingWorkCounter learningPendingWorkCounter;
    private final CommunicationReadService communicationReadService;

    public DashboardEventUnreadFilter() {
        this(
                new SessionManager(),
                new DashboardEventUnreadCounter(),
                new DashboardClassGroupPendingEnrollmentCounter(),
                new DashboardLearningPendingWorkCounter(),
                new CommunicationReadService(ConnectionProvider.defaultProvider())
        );
    }

    DashboardEventUnreadFilter(
            SessionManager sessionManager,
            DashboardEventUnreadCounter counter,
            CommunicationReadService communicationReadService
    ) {
        this(
                sessionManager,
                counter,
                new DashboardClassGroupPendingEnrollmentCounter(),
                new DashboardLearningPendingWorkCounter(),
                communicationReadService
        );
    }

    DashboardEventUnreadFilter(
            SessionManager sessionManager,
            DashboardEventUnreadCounter counter,
            DashboardClassGroupPendingEnrollmentCounter classGroupPendingEnrollmentCounter,
            CommunicationReadService communicationReadService
    ) {
        this(
                sessionManager,
                counter,
                classGroupPendingEnrollmentCounter,
                new DashboardLearningPendingWorkCounter(),
                communicationReadService
        );
    }

    DashboardEventUnreadFilter(
            SessionManager sessionManager,
            DashboardEventUnreadCounter counter,
            DashboardClassGroupPendingEnrollmentCounter classGroupPendingEnrollmentCounter,
            DashboardLearningPendingWorkCounter learningPendingWorkCounter,
            CommunicationReadService communicationReadService
    ) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager is required");
        this.counter = Objects.requireNonNull(counter, "counter is required");
        this.classGroupPendingEnrollmentCounter = Objects.requireNonNull(
                classGroupPendingEnrollmentCounter,
                "classGroupPendingEnrollmentCounter is required"
        );
        this.learningPendingWorkCounter = Objects.requireNonNull(
                learningPendingWorkCounter,
                "learningPendingWorkCounter is required"
        );
        this.communicationReadService = Objects.requireNonNull(communicationReadService, "communicationReadService is required");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && AuthorizationPolicy.isProtected(normalizePath(httpRequest.getServletPath()))) {
            sessionManager.getSessionUser(httpRequest).ifPresent(sessionUser -> {
                if (httpRequest.getAttribute("eventUnreadCount") == null) {
                    httpRequest.setAttribute("eventUnreadCount", counter.countUnread(
                            httpRequest,
                            sessionUser,
                            currentSessionId(httpRequest)
                    ));
                }
                if (httpRequest.getAttribute("classGroupEventCount") == null) {
                    httpRequest.setAttribute(
                            "classGroupEventCount",
                            classGroupPendingEnrollmentCounter.countPendingWork(
                                    httpRequest,
                                    sessionUser,
                                    currentSessionId(httpRequest)
                            )
                    );
                }
                if (httpRequest.getAttribute("learningPendingWorkCount") == null) {
                    httpRequest.setAttribute(
                            "learningPendingWorkCount",
                            learningPendingWorkCounter.countPendingWork(
                                    httpRequest,
                                    sessionUser,
                                    currentSessionId(httpRequest)
                            )
                    );
                }
                if (httpRequest.getAttribute("messageUnreadCount") == null) {
                    httpRequest.setAttribute(
                            "messageUnreadCount",
                            countUnreadMessages(sessionUser)
                    );
                }
            });
        }
        chain.doFilter(request, response);
    }

    private int countUnreadMessages(SessionUser sessionUser) {
        try {
            return communicationReadService.countUnreadDirectMessages(sessionUser.userId());
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private Long currentSessionId(HttpServletRequest request) {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        return sessionId.isPresent() ? sessionId.getAsLong() : null;
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }
}
