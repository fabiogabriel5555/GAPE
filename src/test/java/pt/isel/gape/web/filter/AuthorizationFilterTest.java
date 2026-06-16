package pt.isel.gape.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class AuthorizationFilterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private AuthorizationFilter filter;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            insertAdminWithoutGrants(connection);
            insertAdminWithDashboardOnlyAccess(connection);
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

        AuditService auditService = new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK);
        filter = new AuthorizationFilter(
                new PermissionChecker(connectionProvider),
                new SessionManager(),
                auditService
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
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
    void mediaServletRequestWithoutSessionIsPublic() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/media", null),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void authServletRequestWithoutSessionIsPublic() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/auth", null),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void profileRouteAllowsAuthenticatedUserWithRequiredGrant() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(4L, Set.of(AccessProfileType.STUDENT));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/profile", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void administratorWithRequiredGrantCanAccessAdminSettings() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(1L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-dashbord-settings.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void administratorWithManageUsersCanAccessUserManagementUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(1L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/users.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void administratorCanAccessLearningClassGroupsUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(1L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/learning/class-groups", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void teacherCanAccessLearningClassGroupsUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(3L, Set.of(AccessProfileType.TEACHER));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/learning/class-groups", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void studentCannotAccessLearningClassGroupsManagementUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(4L, Set.of(AccessProfileType.STUDENT));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/learning/class-groups", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals("/ctx/student/student-home.jsp", responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void administratorWithoutManageUsersCannotAccessUserManagementUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(601L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/users.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseState.errorStatus);
    }

    @Test
    void administratorWithoutManagePermissionsCannotAccessPermissionManagementUrl() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(601L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/permissions.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseState.errorStatus);
    }

    @Test
    void administratorWithBaseGrantCanAccessOwnProfilePage() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(601L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-my-profile.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    @Test
    void directAdminUrlRedirectsStudentToAuthorizedDashboard() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(4L, Set.of(AccessProfileType.STUDENT));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-dashbord.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertFalse(chainState.called);
        assertEquals("/ctx/student/student-home.jsp", responseState.redirectLocation);
        assertEquals(null, responseState.errorStatus);
        assertEquals(1, countAuthorizationRedirectedAudits());
    }

    @Test
    void administratorWithoutManagementGrantCanAccessOwnDashboard() throws Exception {
        TestHttpSession httpSession = authenticatedHttpSession(600L, Set.of(AccessProfileType.ADMINISTRATOR));
        TestHttpServletResponse responseState = new TestHttpServletResponse();
        TestFilterChain chainState = new TestFilterChain();

        filter.doFilter(
                requestProxy("/admin/admin-dashbord.jsp", httpSession),
                responseProxy(responseState),
                chainProxy(chainState)
        );

        assertTrue(chainState.called);
        assertEquals(null, responseState.errorStatus);
    }

    private static void insertAdminWithoutGrants(Connection connection) throws Exception {
        try (PreparedStatement user = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (600, 'No Grant Admin', 'nogrant-admin@gape.local', 'active', 'pt-PT',
                          '2026-06-01 10:00:00', 'hash', 'salt')
                """);
             PreparedStatement profile = connection.prepareStatement(
                     "INSERT INTO administrator_profile (id_user, cod_administrator) VALUES (600, 'ADM-NOGRANT')"
             )) {
            user.executeUpdate();
            profile.executeUpdate();
        }
    }

    private static void insertAdminWithDashboardOnlyAccess(Connection connection) throws Exception {
        try (PreparedStatement user = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (601, 'View Only Admin', 'view-admin@gape.local', 'active', 'pt-PT',
                          '2026-06-01 10:00:00', 'hash', 'salt')
                """);
             PreparedStatement profile = connection.prepareStatement(
                     "INSERT INTO administrator_profile (id_user, cod_administrator) VALUES (601, 'ADM-VIEW')"
             )) {
            user.executeUpdate();
            profile.executeUpdate();
        }
    }

    private int countAuthorizationDeniedAudits() throws Exception {
        return countAuthorizationAudits("AUTHORIZATION_DENIED");
    }

    private int countAuthorizationRedirectedAudits() throws Exception {
        return countAuthorizationAudits("AUTHORIZATION_REDIRECTED");
    }

    private int countAuthorizationAudits(String operationType) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = ?"
             )) {
            statement.setString(1, operationType);
            try (ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
            }
        }
    }

    private TestHttpSession authenticatedHttpSession(long userId, Set<AccessProfileType> profileTypes) {
        TestHttpSession httpSession = new TestHttpSession();
        httpSession.setAttribute("gape.auth.user", new SessionUser(userId, "Filter User", "filter@gape.local", null, profileTypes));
        return httpSession;
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
                        sessionState.attributes.clear();
                        yield null;
                    }
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
        private final Map<String, Object> attributes = new HashMap<>();

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
