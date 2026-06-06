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

    private static String landingPageFor(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "/admin/admin-dashbord.jsp";
            case COORDINATOR -> "/coordinator/coordinator-dashboard.jsp";
            case TEACHER -> "/instructor/instructor-dashboard.jsp";
            case STUDENT -> "/student/student-dashbord.jsp";
        };
    }
}
