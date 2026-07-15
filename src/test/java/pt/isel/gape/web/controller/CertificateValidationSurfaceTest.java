package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificateValidationSurfaceTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void publicValidationRouteIsAuthorizedAndRendersEscapedOccurrenceData() throws Exception {
        String servlet = read("src/main/java/pt/isel/gape/web/controller/GradeCertificateServlet.java");
        String policy = read("src/main/java/pt/isel/gape/security/authorization/AuthorizationPolicy.java");
        String jsp = read("src/main/webapp/WEB-INF/views/public/certificate-validation.jsp");

        assertTrue(servlet.contains("\"/certificates/validate\"")
                && servlet.contains("certificateService.validateCertificate")
                && servlet.contains("findCourseOccurrenceById"));
        assertTrue(policy.contains("isPathOrChild(path, \"/certificates/validate\")"));
        assertTrue(jsp.contains("<c:out value=\"${validation.occurrenceLabel}\"/>")
                && jsp.contains("<c:out value='${validationCode}'/>")
                && jsp.contains("maxlength=\"80\""));
    }

    @Test
    void issuedCertificateSurfacesAndSchemaUseValidationCode() throws Exception {
        String manager = read("src/main/webapp/WEB-INF/fragments/learning-certificates-content.jspf");
        String student = read("src/main/webapp/WEB-INF/fragments/student-grades-certificates-content.jspf");
        String schema = read("src/main/resources/sql/schema.sql");

        assertTrue(manager.contains("${certificate.validationCode}")
                && student.contains("${certificate.validationCode}"));
        assertTrue(schema.contains("validation_code VARCHAR(80) NULL")
                && schema.contains("UNIQUE KEY uq_certificate_validation_code (validation_code)")
                && schema.contains("AND validation_code IS NOT NULL"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
