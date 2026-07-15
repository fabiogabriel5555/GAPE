package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class CertificateRevocationRemovalTest {

    private static final List<Path> CERTIFICATE_PRODUCTION_SOURCES = List.of(
            Path.of("src/main/java/pt/isel/gape/learning/model/Certificate.java"),
            Path.of("src/main/java/pt/isel/gape/learning/model/CertificateState.java"),
            Path.of("src/main/java/pt/isel/gape/learning/model/CertificateValidationResult.java"),
            Path.of("src/main/java/pt/isel/gape/learning/dao/CertificateDAO.java"),
            Path.of("src/main/java/pt/isel/gape/learning/service/CertificateService.java"),
            Path.of("src/main/java/pt/isel/gape/learning/service/GradeCertificateReadService.java"),
            Path.of("src/main/java/pt/isel/gape/web/controller/GradeCertificateServlet.java"),
            Path.of("src/main/resources/config/xml/certificate-states.xml"),
            Path.of("src/main/resources/config/xsd/gape-config.xsd"),
            Path.of("src/main/resources/sql/schema.sql"),
            Path.of("src/main/resources/sql/seed/base.sql"),
            Path.of("src/main/resources/sql/seed/full.sql"),
            Path.of("src/main/webapp/WEB-INF/fragments/learning-certificates-content.jspf"),
            Path.of("src/main/webapp/WEB-INF/fragments/learning-grades-certificates-content.jspf"),
            Path.of("src/main/webapp/WEB-INF/fragments/student-grades-certificates-content.jspf"),
            Path.of("src/main/webapp/WEB-INF/views/learning/grades-certificates.jsp"),
            Path.of("src/main/webapp/WEB-INF/views/public/certificate-validation.jsp")
    );

    @Test
    void certificateRuntimeSchemaAndViewsExposeNoRevocationConcept() throws Exception {
        for (Path source : CERTIFICATE_PRODUCTION_SOURCES) {
            String content = Files.readString(source).toLowerCase(Locale.ROOT);
            assertFalse(
                    content.contains("revoked")
                            || content.contains("revoked_at")
                            || content.contains("revokecertificate")
                            || content.contains("/revoke"),
                    () -> "Certificate revocation artifact found in " + source
            );
        }
    }
}
