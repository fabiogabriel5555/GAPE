package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardServletSupportTest {

    @Test
    void safeReturnPathAcceptsOnlyInternalAttributeSafePaths() {
        assertTrue(DashboardServletSupport.isSafeReturnPath("/learning/lessons?classGroupId=50"));
        assertTrue(DashboardServletSupport.isSafeReturnPath("/student/calendar"));

        assertFalse(DashboardServletSupport.isSafeReturnPath("//evil.test/path"));
        assertFalse(DashboardServletSupport.isSafeReturnPath("/learning/lessons\" onclick=\"alert(1)"));
        assertFalse(DashboardServletSupport.isSafeReturnPath("/learning/<script>"));
        assertFalse(DashboardServletSupport.isSafeReturnPath("/learning/lessons return"));
        assertFalse(DashboardServletSupport.isSafeReturnPath("/learning/lessons\r\nSet-Cookie:bad=true"));
        assertFalse(DashboardServletSupport.isSafeReturnPath("https://evil.test/learning"));
    }
}
