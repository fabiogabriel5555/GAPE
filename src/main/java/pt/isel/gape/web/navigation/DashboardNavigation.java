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
            case COORDINATOR -> "/coordinator/coordinator-dashboard.jsp";
            case TEACHER -> "/instructor/instructor-dashboard.jsp";
            case STUDENT -> "/student/student-dashbord.jsp";
        };
    }

    private static String profilePageFor(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "/admin/admin-dashbord-my-profile.jsp";
            case COORDINATOR -> "/coordinator/coordinator-dashboard-my-profile.jsp";
            case TEACHER -> "/instructor/instructor-dashboard-my-profile.jsp";
            case STUDENT -> "/student/student-dashbord-my-profile.jsp";
        };
    }
}
