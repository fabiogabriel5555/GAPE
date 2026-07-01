package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Connection;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.service.AssessmentPdfService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AssessmentPdfServiceTest {

    private final ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("full.sql"));
        }
    }

    @Test
    void submittedAttemptPdfDoesNotExposeScoresBeforeCorrection() throws IOException {
        AssessmentPdfService service = new AssessmentPdfService(connectionProvider);
        String text = pdfText(service.renderAttemptResponsesPdf(199L, 501L));

        assertTrue(text.contains("Score"));
        assertTrue(text.contains("Not assigned yet"));
        assertFalse(text.contains("4 / 20"));
        assertFalse(text.contains("2 / 2"));
        assertFalse(text.contains("0 / 2"));
    }

    private static String pdfText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
