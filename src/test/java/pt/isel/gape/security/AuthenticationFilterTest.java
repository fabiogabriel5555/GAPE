package pt.isel.gape.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import pt.isel.gape.web.filter.AuthenticationFilter;

class AuthenticationFilterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ConnectionProvider connectionProvider;
    private SessionService sessionService;
    private SessionManager sessionManager;
    private AuthenticationFilter filter;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            insertUser(connection, 400L);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;

        sessionService = new SessionService(
                new SessionDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK),
                FIXED_CLOCK
        );
        sessionManager = new SessionManager();
        filter = instantiateFilter(sessionService, sessionManager);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void validSessionAllowsAccessAndUpdatesActivity() throws Exception {
        Session persisted = sessionService.createSession(400L);
        TestHttpSession httpSession = new TestHttpSession();
        httpSession.setAttribute("gape.auth.user", new SessionUser(400L, "Filter User", "filter@gape.local", null, Set.of(AccessProfileType.STUDENT)));
        httpSession.setAttribute("gape.auth.sessionId", persisted.id());
        httpSession.setAttribute("gape.auth.sessionToken", persisted.token());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/student/student-home.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);

        Session refreshed = sessionService.findById(persisted.id()).orElseThrow();
        assertTrue(!refreshed.lastActivity().isBefore(persisted.lastActivity()));
    }

    @Test
    void missingSessionRedirectsToLogin() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-dashbord.jsp", null),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals("/ctx/login.jsp?auth=required", responseState.redirectLocation);
    }

    @Test
    void authenticationFilterDoesNotMakeAuthorizationDecisions() throws Exception {
        Session persisted = sessionService.createSession(400L);
        TestHttpSession httpSession = authenticatedHttpSession(persisted, AccessProfileType.STUDENT);

        assertAllowed("/admin/admin-dashbord.jsp", httpSession);
        assertAllowed("/coordinator/coordinator-home.jsp", httpSession);
        assertAllowed("/instructor/instructor-home.jsp", httpSession);
        assertAllowed("/student/student-home.jsp", httpSession);
    }

    @Test
    void authenticatedUserCanAccessEveryPageMatchingAnyOwnedProfile() throws Exception {
        Session persisted = sessionService.createSession(400L);
        TestHttpSession httpSession = authenticatedHttpSession(
                persisted,
                Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.STUDENT)
        );

        assertAllowed("/student/student-home.jsp", httpSession);
        assertAllowed("/admin/admin-dashbord.jsp", httpSession);
    }

    @Test
    void protectedNonDashboardPageRequiresSession() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/profile.jsp", null),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals("/ctx/login.jsp?auth=required", responseState.redirectLocation);
    }

    @Test
    void expiredSessionIsBlockedAndMarkedExpired() throws Exception {
        long sessionId = insertSession(400L, "tok-expired", "2026-06-04 08:00:00", "2026-06-04 08:30:00", "active", null);
        Session persisted = sessionService.findById(sessionId).orElseThrow();

        TestHttpSession httpSession = new TestHttpSession();
        httpSession.setAttribute("gape.auth.user", new SessionUser(400L, "Filter User", "filter@gape.local", null, Set.of(AccessProfileType.STUDENT)));
        httpSession.setAttribute("gape.auth.sessionId", persisted.id());
        httpSession.setAttribute("gape.auth.sessionToken", persisted.token());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-dashbord.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals("/ctx/login.jsp?auth=expired", responseState.redirectLocation);

        Session expired = sessionService.findById(sessionId).orElseThrow();
        assertEquals(SessionState.EXPIRED, expired.state());

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = 'SESSION_EXPIRED' AND id_session = ?"
             )) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }
    }

    private void assertRedirectedToAllowedDashboard(
            String servletPath,
            AccessProfileType profileType,
            String expectedLocation
    ) throws Exception {
        Session persisted = sessionService.createSession(400L);
        TestHttpSession httpSession = authenticatedHttpSession(persisted, profileType);
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy(servletPath, httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals(expectedLocation, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    private void assertAllowed(String servletPath, TestHttpSession httpSession) throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy(servletPath, httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    private TestHttpSession authenticatedHttpSession(Session persisted, AccessProfileType profileType) {
        return authenticatedHttpSession(persisted, Set.of(profileType));
    }

    private TestHttpSession authenticatedHttpSession(Session persisted, Set<AccessProfileType> profileTypes) {
        TestHttpSession httpSession = new TestHttpSession();
        httpSession.setAttribute("gape.auth.user", new SessionUser(400L, "Filter User", "filter@gape.local", null, profileTypes));
        httpSession.setAttribute("gape.auth.sessionId", persisted.id());
        httpSession.setAttribute("gape.auth.sessionToken", persisted.token());
        return httpSession;
    }

    private AuthenticationFilter instantiateFilter(SessionService service, SessionManager manager) throws Exception {
        var constructor = AuthenticationFilter.class.getDeclaredConstructor(SessionService.class, SessionManager.class);
        constructor.setAccessible(true);
        return constructor.newInstance(service, manager);
    }

    private static void insertUser(Connection connection, long userId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (?, 'Filter User', 'filter@gape.local', 'active', 'pt-PT', '2026-06-01 10:00:00', 'hash', 'salt')
                """)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private long insertSession(
            long userId,
            String token,
            String startAt,
            String lastActivity,
            String state,
            String endAt
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_session (id_user, token, state, start_at, last_activity, end_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, token);
            statement.setString(3, state);
            statement.setString(4, startAt);
            statement.setString(5, lastActivity);
            if (endAt == null) {
                statement.setNull(6, java.sql.Types.TIMESTAMP);
            } else {
                statement.setString(6, endAt);
            }
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private HttpServletRequest requestProxy(String servletPath, TestHttpSession sessionState) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServletPath" -> servletPath;
                    case "getContextPath" -> "/ctx";
                    case "getRemoteAddr" -> "127.0.0.1";
                    case "getSession" -> {
                        boolean create = args != null && args.length == 1 && args[0] instanceof Boolean b && b;
                        if (sessionState == null && !create) {
                            yield null;
                        }
                        yield sessionProxy(sessionState == null ? new TestHttpSession() : sessionState);
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

    private FilterChain chainProxy(TestFilterChain chainState) {
        return (FilterChain) Proxy.newProxyInstance(
                FilterChain.class.getClassLoader(),
                new Class<?>[]{FilterChain.class},
                (proxy, method, args) -> {
                    if ("doFilter".equals(method.getName())) {
                        chainState.called = true;
                    }
                    return null;
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
                    case "getId" -> sessionState.id;
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

    private static final class TestHttpSession {
        private final String id = "http-session-1";
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated;
        private int maxInactiveInterval;

        private void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }
    }

    private static final class TestHttpServletResponse {
        private String redirectLocation;
        private Integer errorStatus;
    }

    private static final class TestFilterChain {
        private boolean called;
    }
}
