package pt.isel.gape.security.authorization;

import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;

public final class AuthorizationPolicy {

    public static final String VIEW_REPORTS = "VIEW_REPORTS";
    public static final String MANAGE_USERS = "MANAGE_USERS";
    public static final String MANAGE_PERMISSIONS = "MANAGE_PERMISSIONS";
    public static final String MANAGE_SETTINGS = "MANAGE_SETTINGS";
    public static final String VIEW_PERSONAL_DATA = "VIEW_PERSONAL_DATA";
    public static final String MANAGE_PERSONAL_DATA = "MANAGE_PERSONAL_DATA";
    public static final String PROCESS_DELETION_REQUESTS = "PROCESS_DELETION_REQUESTS";

    private AuthorizationPolicy() {
    }

    public static Optional<AuthorizationRule> ruleFor(String servletPath) {
        String path = normalizePath(servletPath);
        if (isPublic(path)) {
            return Optional.empty();
        }
        if (path.startsWith("/admin/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR),
                    adminPermissionFor(path),
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/coordinator/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.COORDINATOR),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/instructor/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.TEACHER),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/student/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.STUDENT),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.equals("/dashboard") || path.equals("/profile")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.COORDINATOR,
                            AccessProfileType.TEACHER, AccessProfileType.STUDENT),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/account/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.COORDINATOR,
                            AccessProfileType.TEACHER, AccessProfileType.STUDENT),
                    VIEW_REPORTS,
                    AccessEntityType.SELF
            ));
        }
        if (isProtectedRootPage(path)) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.COORDINATOR,
                            AccessProfileType.TEACHER, AccessProfileType.STUDENT),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        return Optional.empty();
    }

    private static String adminPermissionFor(String path) {
        if (containsAny(path, "permission", "permissions", "grant", "grants")) {
            return MANAGE_PERMISSIONS;
        }
        if (containsAny(path, "settings", "config", "configuration")) {
            return MANAGE_SETTINGS;
        }
        if (containsAny(path, "deletion", "delete-request", "right-to-be-forgotten")) {
            return PROCESS_DELETION_REQUESTS;
        }
        if (containsAny(path, "audit", "activity-log")) {
            return VIEW_REPORTS;
        }
        if (containsAny(path, "personal-data", "profile")) {
            return MANAGE_PERSONAL_DATA;
        }
        if (containsAny(path, "users", "user-management", "accounts", "roles", "assignments")) {
            return MANAGE_USERS;
        }
        return VIEW_REPORTS;
    }

    public static boolean isProtected(String servletPath) {
        String path = normalizePath(servletPath);
        return !isPublic(path);
    }

    public static boolean isPublic(String servletPath) {
        String path = normalizePath(servletPath);
        return path.equals("/")
                || path.equals("/index.jsp")
                || path.equals("/login.jsp")
                || path.equals("/sign-in.jsp")
                || path.equals("/sign-up.jsp")
                || path.equals("/contact.jsp")
                || path.equals("/courses.jsp")
                || path.equals("/course.jsp")
                || path.equals("/course-details.jsp")
                || path.equals("/about-four.jsp")
                || path.equals("/instructor/instructor.jsp")
                || path.equals("/instructor/instructor-details.jsp")
                || path.equals("/tutor.jsp")
                || path.equals("/tutor-details.jsp")
                || path.equals("/events.jsp")
                || path.equals("/event-details.jsp")
                || path.equals("/apply-admission.jsp")
                || path.equals("/error-403.jsp")
                || path.equals("/error-404.jsp")
                || path.equals("/error-500.jsp")
                || isPathOrChild(path, "/assets")
                || isPathOrChild(path, "/media")
                || isPathOrChild(path, "/auth");
    }

    private static boolean isProtectedRootPage(String path) {
        return path.equals("/content.jsp")
                || path.equals("/messages.jsp")
                || path.equals("/forms.jsp")
                || path.equals("/tables.jsp")
                || path.equals("/profile.jsp")
                || path.equals("/my-propyl.jsp")
                || path.equals("/dashbord.jsp")
                || path.equals("/lesson-details.jsp");
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }

    private static boolean isPathOrChild(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static boolean containsAny(String path, String... fragments) {
        for (String fragment : fragments) {
            if (path.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
