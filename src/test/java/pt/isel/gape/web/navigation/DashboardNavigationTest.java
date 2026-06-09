package pt.isel.gape.web.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.security.session.SessionUser;

class DashboardNavigationTest {

    @Test
    void profilePageMatchesPrimaryUserProfile() {
        assertEquals(
                "/admin/admin-my-profile.jsp",
                DashboardNavigation.profilePageFor(sessionUserWith(AccessProfileType.ADMINISTRATOR)).orElseThrow()
        );
        assertEquals(
                "/coordinator/coordinator-my-profile.jsp",
                DashboardNavigation.profilePageFor(sessionUserWith(AccessProfileType.COORDINATOR)).orElseThrow()
        );
        assertEquals(
                "/instructor/instructor-my-profile.jsp",
                DashboardNavigation.profilePageFor(sessionUserWith(AccessProfileType.TEACHER)).orElseThrow()
        );
        assertEquals(
                "/student/student-my-profile.jsp",
                DashboardNavigation.profilePageFor(sessionUserWith(AccessProfileType.STUDENT)).orElseThrow()
        );
    }

    @Test
    void profilePageUsesExistingPrimaryProfilePriorityForUsersWithMultipleProfiles() {
        SessionUser sessionUser = new SessionUser(
                1L,
                "Multi Profile User",
                "multi@gape.local",
                null,
                Set.of(AccessProfileType.STUDENT, AccessProfileType.ADMINISTRATOR)
        );

        assertEquals(
                "/admin/admin-my-profile.jsp",
                DashboardNavigation.profilePageFor(sessionUser).orElseThrow()
        );
    }

    private static SessionUser sessionUserWith(AccessProfileType profileType) {
        return new SessionUser(1L, "Navigation User", "navigation@gape.local", null, Set.of(profileType));
    }
}
