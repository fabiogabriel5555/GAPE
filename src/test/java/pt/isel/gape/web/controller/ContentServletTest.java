package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.ContentFileService;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.service.AuditService;

class ContentServletTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ContentUploadServlet uploadServlet;
    private ContentDownloadServlet downloadServlet;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        SessionManager sessionManager = new SessionManager();
        ContentItemService contentItemService = new ContentItemService(connectionProvider, FIXED_CLOCK);
        PdfUploadService pdfUploadService = new PdfUploadService();
        AuditService auditService = new AuditService(connectionProvider, FIXED_CLOCK);

        uploadServlet = new ContentUploadServlet(
                sessionManager,
                contentItemService,
                new ContentAssociationService(connectionProvider, FIXED_CLOCK),
                new ContentFileService(connectionProvider, FIXED_CLOCK),
                pdfUploadService,
                auditService
        );
        downloadServlet = new ContentDownloadServlet(
                sessionManager,
                contentItemService,
                pdfUploadService,
                auditService
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void uploadWithoutSessionReturnsUnauthorized() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        uploadServlet.doPost(requestProxy(null), responseProxy(responseState));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, responseState.status);
        assertEquals("text/plain;charset=UTF-8", responseState.contentType);
        assertEquals("Authentication is required to upload content.", responseState.body.toString());
    }

    @Test
    void downloadWithoutSessionReturnsUnauthorized() throws Exception {
        TestHttpServletResponse responseState = new TestHttpServletResponse();

        downloadServlet.doGet(requestProxy("/70"), responseProxy(responseState));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, responseState.errorStatus);
    }

    private static HttpServletRequest requestProxy(String pathInfo) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> null;
                    case "getRemoteAddr" -> "127.0.0.1";
                    case "getPathInfo" -> pathInfo;
                    case "getParameter" -> null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static HttpServletResponse responseProxy(TestHttpServletResponse responseState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        responseState.status = (Integer) args[0];
                        yield null;
                    }
                    case "sendError" -> {
                        responseState.errorStatus = (Integer) args[0];
                        yield null;
                    }
                    case "setContentType" -> {
                        responseState.contentType = (String) args[0];
                        yield null;
                    }
                    case "getWriter" -> new PrintWriter(responseState.body, true);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
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

    private static final class TestHttpServletResponse {
        private final StringWriter body = new StringWriter();
        private int status;
        private int errorStatus;
        private String contentType;
    }
}
