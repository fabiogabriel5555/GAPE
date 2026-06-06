package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class LogoutServletTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private SessionService sessionService;
    private SessionManager sessionManager;
    private LogoutServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            insertUser(connection);
        }

        sessionService = new SessionService(
                new SessionDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK),
                FIXED_CLOCK
        );
        sessionManager = new SessionManager();
        servlet = new LogoutServlet(sessionService, sessionManager);
    }

    @Test
    void getLogoutIsRejectedAndDoesNotCloseSession() throws Exception {
        Session persisted = sessionService.createSession(500L);
        TestHttpServletRequest requestState = authenticatedRequest(persisted);
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doGet(requestProxy(requestState), responseProxy(responseState));

        assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, responseState.errorStatus);
        assertFalse(requestState.session.invalidated);
        assertEquals(SessionState.ACTIVE, sessionService.findById(persisted.id()).orElseThrow().state());
    }

    @Test
    void postLogoutRejectsInvalidCsrfToken() throws Exception {
        Session persisted = sessionService.createSession(500L);
        TestHttpServletRequest requestState = authenticatedRequest(persisted);
        requestState.parameters.put("csrfToken", "invalid-token");
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doPost(requestProxy(requestState), responseProxy(responseState));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseState.errorStatus);
        assertFalse(requestState.session.invalidated);
        assertEquals(SessionState.ACTIVE, sessionService.findById(persisted.id()).orElseThrow().state());
    }

    @Test
    void postLogoutWithValidCsrfClosesSessionAndClearsHttpSession() throws Exception {
        Session persisted = sessionService.createSession(500L);
        TestHttpServletRequest requestState = authenticatedRequest(persisted);
        requestState.parameters.put(
                "csrfToken",
                (String) requestState.session.attributes.get("gape.auth.logoutCsrfToken")
        );
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        servlet.doPost(requestProxy(requestState), responseProxy(responseState));

        assertEquals("/ctx/login.jsp?logout=1", responseState.redirectLocation);
        assertTrue(requestState.session.invalidated);
        assertEquals(SessionState.CLOSED, sessionService.findById(persisted.id()).orElseThrow().state());
    }

    private TestHttpServletRequest authenticatedRequest(Session persisted) {
        TestHttpServletRequest requestState = new TestHttpServletRequest();
        sessionManager.startAuthenticatedSession(
                requestProxy(requestState),
                new SessionUser(500L, "Logout User", "logout@gape.local", null, Set.of(AccessProfileType.STUDENT)),
                persisted
        );
        return requestState;
    }

    private void insertUser(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (500, 'Logout User', 'logout@gape.local', 'active', 'pt-PT', '2026-06-01 10:00:00', 'hash', 'salt')
                """)) {
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
                    case "getParameter" -> requestState.parameters.get((String) args[0]);
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
                    case "getId" -> "http-session-logout";
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
        private final Map<String, String> parameters = new HashMap<>();
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
