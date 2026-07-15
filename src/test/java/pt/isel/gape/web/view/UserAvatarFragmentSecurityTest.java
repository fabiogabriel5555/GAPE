package pt.isel.gape.web.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UserAvatarFragmentSecurityTest {

    @Test
    void escapesUserControlledAvatarAttributes() throws Exception {
        String fragment = Files.readString(Path.of(
                "src",
                "main",
                "webapp",
                "WEB-INF",
                "fragments",
                "user-avatar-content.jspf"
        ));

        assertTrue(fragment.contains("src=\"${fn:escapeXml(gapeUserAvatarUrl)}\""));
        assertTrue(fragment.contains("alt=\"${fn:escapeXml(sessionScope['gape.auth.userName'])}\""));
        assertFalse(fragment.contains("alt=\"${sessionScope['gape.auth.userName']}\""));
    }
}
