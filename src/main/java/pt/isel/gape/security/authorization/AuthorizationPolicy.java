package pt.isel.gape.security.authorization;

import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;

public final class AuthorizationPolicy {

    public static final String MANAGE_ALL = "MANAGE_ALL";
    public static final String MANAGE_ORGANIZATION_STRUCTURE = "MANAGE_ORGANIZATION_STRUCTURE";
    public static final String MANAGE_LEARNING = "MANAGE_LEARNING";
    public static final String MANAGE_ENROLLMENTS = "MANAGE_ENROLLMENTS";

    public static final String VIEW_REPORTS = "VIEW_REPORTS";
    public static final String MANAGE_USERS = MANAGE_ALL;
    public static final String MANAGE_PERMISSIONS = MANAGE_ALL;
    public static final String MANAGE_SETTINGS = MANAGE_ALL;
    public static final String MANAGE_ORGANIZATIONS = MANAGE_ORGANIZATION_STRUCTURE;
    public static final String MANAGE_COURSES = MANAGE_LEARNING;
    public static final String MANAGE_SUBJECTS = MANAGE_LEARNING;
    public static final String VIEW_PERSONAL_DATA = MANAGE_ALL;
    public static final String MANAGE_PERSONAL_DATA = MANAGE_ALL;
    public static final String PROCESS_DELETION_REQUESTS = MANAGE_ALL;

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
        if (path.equals("/dashboard") || path.equals("/profile") || isPathOrChild(path, "/messages")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.COORDINATOR,
                            AccessProfileType.TEACHER, AccessProfileType.STUDENT),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/learning/")) {
            return Optional.of(new AuthorizationRule(
                    Set.of(AccessProfileType.ADMINISTRATOR, AccessProfileType.COORDINATOR, AccessProfileType.TEACHER),
                    VIEW_REPORTS,
                    AccessEntityType.GLOBAL
            ));
        }
        if (path.startsWith("/contents/")) {
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
            return MANAGE_ALL;
        }
        if (containsAny(path, "organization", "organizations", "organic-unit", "organic-units")) {
            return MANAGE_ORGANIZATION_STRUCTURE;
        }
        if (containsAny(path, "course", "courses")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "subject", "subjects", "discipline", "disciplines",
                "class-group", "class-groups", "content-block", "content-blocks")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "enrollment", "enrollments")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "settings", "config", "configuration")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "deletion", "delete-request", "right-to-be-forgotten")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "audit", "activity-log")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "my-profile")) {
            return VIEW_REPORTS;
        }
        if (containsAny(path, "personal-data", "profile")) {
            return MANAGE_ALL;
        }
        if (containsAny(path, "users", "user", "user-management", "accounts", "account", "roles", "assignments")) {
            return MANAGE_ALL;
        }
        return VIEW_REPORTS;
    }

    public static String canonicalAdminPermission(String permissionCode) {
        return switch (permissionCode) {
            case "MANAGE_USERS", "MANAGE_PERMISSIONS", "MANAGE_SETTINGS", "VIEW_PERSONAL_DATA",
                 "MANAGE_PERSONAL_DATA", "PROCESS_DELETION_REQUESTS", MANAGE_ALL -> MANAGE_ALL;
            case "MANAGE_ORGANIZATIONS", MANAGE_ORGANIZATION_STRUCTURE -> MANAGE_ORGANIZATION_STRUCTURE;
            case "MANAGE_COURSES", "MANAGE_SUBJECTS" -> MANAGE_LEARNING;
            default -> permissionCode;
        };
    }

    public static boolean isAdminPermission(String permissionCode) {
        String canonical = canonicalAdminPermission(permissionCode);
        return MANAGE_ALL.equals(canonical)
                || MANAGE_ORGANIZATION_STRUCTURE.equals(canonical);
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
                || path.equals("/courses")
                || isPathOrChild(path, "/courses")
                || path.equals("/courses.jsp")
                || path.equals("/course.jsp")
                || path.equals("/course-list-view.jsp")
                || path.equals("/course-details.jsp")
                || path.equals("/about-four.jsp")
                || path.equals("/tutor.jsp")
                || path.equals("/tutor-details.jsp")
                || path.equals("/events.jsp")
                || path.equals("/event-details.jsp")
                || path.equals("/apply-admission.jsp")
                || path.equals("/error-403.jsp")
                || path.equals("/error-404.jsp")
                || path.equals("/error-500.jsp")
                || isPathOrChild(path, "/certificates/validate")
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
