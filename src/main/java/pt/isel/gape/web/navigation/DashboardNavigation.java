package pt.isel.gape.web.navigation;

import java.util.Optional;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.security.session.SessionUser;

public final class DashboardNavigation {

    private DashboardNavigation() {
    }

    public static Optional<String> landingPageFor(SessionUser sessionUser) {
        return sessionUser.primaryProfileType().map(DashboardNavigation::landingPageFor);
    }

    public static Optional<String> profilePageFor(SessionUser sessionUser) {
        return sessionUser.primaryProfileType().map(DashboardNavigation::profilePageFor);
    }

    private static String landingPageFor(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "/admin/admin-dashbord.jsp";
            case COORDINATOR -> "/coordinator/coordinator-home.jsp";
            case TEACHER -> "/instructor/instructor-home.jsp";
            case STUDENT -> "/student/student-home.jsp";
        };
    }

    private static String profilePageFor(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "/admin/admin-my-profile.jsp";
            case COORDINATOR -> "/coordinator/coordinator-my-profile.jsp";
            case TEACHER -> "/instructor/instructor-my-profile.jsp";
            case STUDENT -> "/student/student-my-profile.jsp";
        };
    }
}
