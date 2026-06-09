package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.dao.SessionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.SessionState;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class ProfileServletTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private SessionService sessionService;
    private SessionManager sessionManager;
    private ProfileServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            insertUser(connection, 700L, "student-profile@gape.local");
            insertUser(connection, 701L, "admin-profile@gape.local");
        }

        sessionService = new SessionService(
                new SessionDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK),
                FIXED_CLOCK
        );
        sessionManager = new SessionManager();
        servlet = new ProfileServlet(sessionService, sessionManager);
    }

    @Test
    void missingSessionRedirectsToLoginRequired() throws Exception {
        TestHttpServletRequest requestState = new TestHttpServletRequest();
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doGet(requestProxy(requestState), responseProxy(responseState));

        assertEquals("/ctx/login.jsp?auth=required", responseState.redirectLocation);
    }

    @Test
    void studentSessionRedirectsToStudentProfilePage() throws Exception {
        Session persisted = sessionService.createSession(700L);
        TestHttpServletRequest requestState = authenticatedRequest(
                persisted,
                new SessionUser(700L, "Student User", "student-profile@gape.local", null, Set.of(AccessProfileType.STUDENT))
        );
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doGet(requestProxy(requestState), responseProxy(responseState));

        assertEquals("/ctx/student/student-my-profile.jsp", responseState.redirectLocation);
    }

    @Test
    void administratorSessionRedirectsToAdministratorProfilePage() throws Exception {
        Session persisted = sessionService.createSession(701L);
        TestHttpServletRequest requestState = authenticatedRequest(
                persisted,
                new SessionUser(701L, "Admin User", "admin-profile@gape.local", null, Set.of(AccessProfileType.ADMINISTRATOR))
        );
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doGet(requestProxy(requestState), responseProxy(responseState));

        assertEquals("/ctx/admin/admin-my-profile.jsp", responseState.redirectLocation);
    }

    @Test
    void expiredSessionRedirectsToLoginExpiredAndClearsHttpSession() throws Exception {
        Session persisted = sessionService.createSession(700L);
        sessionService.expire(persisted, "127.0.0.1");
        TestHttpServletRequest requestState = authenticatedRequest(
                persisted,
                new SessionUser(700L, "Student User", "student-profile@gape.local", null, Set.of(AccessProfileType.STUDENT))
        );
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doGet(requestProxy(requestState), responseProxy(responseState));

        assertEquals("/ctx/login.jsp?auth=expired", responseState.redirectLocation);
        assertTrue(requestState.session.invalidated);
        assertEquals(SessionState.EXPIRED, sessionService.findById(persisted.id()).orElseThrow().state());
    }

    private TestHttpServletRequest authenticatedRequest(Session persisted, SessionUser sessionUser) {
        TestHttpServletRequest requestState = new TestHttpServletRequest();
        sessionManager.startAuthenticatedSession(requestProxy(requestState), sessionUser, persisted);
        return requestState;
    }

    private void insertUser(Connection connection, long userId, String email) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (?, 'Profile User', ?, 'active', 'pt-PT', '2026-06-01 10:00:00', 'hash', 'salt')
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private HttpServletRequest requestProxy(TestHttpServletRequest requestState) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContextPath" -> "/ctx";
                    case "getRemoteAddr" -> "127.0.0.1";
                    case "getSession" -> {
                        boolean create = args != null && args.length == 1 && args[0] instanceof Boolean value && value;
                        if (requestState.session == null && create) {
                            requestState.session = new TestHttpSession();
                        }
                        yield requestState.session == null ? null : sessionProxy(requestState.session);
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private HttpServletResponse responseProxy(TestHttpServletResponse responseState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendRedirect" -> {
                        responseState.redirectLocation = (String) args[0];
                        yield null;
                    }
                    case "sendError" -> {
                        responseState.errorStatus = (Integer) args[0];
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private HttpSession sessionProxy(TestHttpSession sessionState) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> sessionState.attributes.get((String) args[0]);
                    case "setAttribute" -> {
                        sessionState.attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "removeAttribute" -> {
                        sessionState.attributes.remove((String) args[0]);
                        yield null;
                    }
                    case "invalidate" -> {
                        sessionState.invalidated = true;
                        sessionState.attributes.clear();
                        yield null;
                    }
                    case "setMaxInactiveInterval" -> {
                        sessionState.maxInactiveInterval = (Integer) args[0];
                        yield null;
                    }
                    case "getMaxInactiveInterval" -> sessionState.maxInactiveInterval;
                    case "getId" -> "http-session-profile";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static final class TestHttpServletRequest {
        private TestHttpSession session;
    }

    private static final class TestHttpSession {
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated;
        private int maxInactiveInterval;
    }

    private static final class TestHttpServletResponse {
        private String redirectLocation;
        private Integer errorStatus;
    }
}
