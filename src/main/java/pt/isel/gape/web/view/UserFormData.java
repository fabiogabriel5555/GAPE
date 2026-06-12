package pt.isel.gape.web.view;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.AccessProfileContextAssignment;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class UserFormData {

    private final Long id;
    private final String name;
    private final String email;
    private final String state;
    private final String language;
    private final String photo;
    private final String documentType;
    private final String documentNumber;
    private final String password;
    private final boolean administratorProfile;
    private final boolean coordinatorProfile;
    private final boolean teacherProfile;
    private final boolean studentProfile;
    private final Set<Long> managedOrganizationIds;
    private final Set<AdministratorPermissionAssignment> adminPermissionAssignments;
    private final Set<AccessProfileContextAssignment> profileContextAssignments;

    public UserFormData(
            Long id,
            String name,
            String email,
            String state,
            String language,
            String photo,
            String documentType,
            String documentNumber,
            String password,
            boolean administratorProfile,
            boolean coordinatorProfile,
            boolean teacherProfile,
            boolean studentProfile,
            Set<Long> managedOrganizationIds
    ) {
        this(
                id,
                name,
                email,
                state,
                language,
                photo,
                documentType,
                documentNumber,
                password,
                administratorProfile,
                coordinatorProfile,
                teacherProfile,
                studentProfile,
                managedOrganizationIds,
                assignmentsFromManagedOrganizations(managedOrganizationIds),
                Set.of()
        );
    }

    public UserFormData(
            Long id,
            String name,
            String email,
            String state,
            String language,
            String photo,
            String documentType,
            String documentNumber,
            String password,
            boolean administratorProfile,
            boolean coordinatorProfile,
            boolean teacherProfile,
            boolean studentProfile,
            Set<Long> managedOrganizationIds,
            Set<AdministratorPermissionAssignment> adminPermissionAssignments
    ) {
        this(
                id,
                name,
                email,
                state,
                language,
                photo,
                documentType,
                documentNumber,
                password,
                administratorProfile,
                coordinatorProfile,
                teacherProfile,
                studentProfile,
                managedOrganizationIds,
                adminPermissionAssignments,
                Set.of()
        );
    }

    public UserFormData(
            Long id,
            String name,
            String email,
            String state,
            String language,
            String photo,
            String documentType,
            String documentNumber,
            String password,
            boolean administratorProfile,
            boolean coordinatorProfile,
            boolean teacherProfile,
            boolean studentProfile,
            Set<Long> managedOrganizationIds,
            Set<AdministratorPermissionAssignment> adminPermissionAssignments,
            Set<AccessProfileContextAssignment> profileContextAssignments
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.state = state;
        this.language = language;
        this.photo = photo;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.password = password;
        this.administratorProfile = administratorProfile;
        this.coordinatorProfile = coordinatorProfile;
        this.teacherProfile = teacherProfile;
        this.studentProfile = studentProfile;
        this.managedOrganizationIds = managedOrganizationIds == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(managedOrganizationIds));
        this.adminPermissionAssignments = adminPermissionAssignments == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(adminPermissionAssignments));
        this.profileContextAssignments = profileContextAssignments == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(profileContextAssignments));
    }

    public static UserFormData blank() {
        return new UserFormData(
                null,
                "",
                "",
                UserState.ACTIVE.name(),
                SupportedLanguageCatalog.defaultLanguageCode(),
                "",
                "",
                "",
                "",
                false,
                false,
                false,
                true,
                Set.of()
        );
    }

    public static UserFormData from(User user) {
        return from(user, Set.of(), Set.of());
    }

    public static UserFormData from(User user, Set<Long> managedOrganizationIds) {
        return from(user, managedOrganizationIds, assignmentsFromManagedOrganizations(managedOrganizationIds));
    }

    public static UserFormData from(
            User user,
            Set<Long> managedOrganizationIds,
            Set<AdministratorPermissionAssignment> adminPermissionAssignments
    ) {
        return from(user, managedOrganizationIds, adminPermissionAssignments, Set.of());
    }

    public static UserFormData from(
            User user,
            Set<Long> managedOrganizationIds,
            Set<AdministratorPermissionAssignment> adminPermissionAssignments,
            Set<AccessProfileContextAssignment> profileContextAssignments
    ) {
        UserView view = UserView.from(user);
        return new UserFormData(
                user.id(),
                user.name(),
                user.email(),
                user.state().name(),
                user.language(),
                nullToEmpty(user.photo()),
                nullToEmpty(user.documentType()),
                nullToEmpty(user.documentNumber()),
                "",
                view.isAdministratorProfile(),
                view.isCoordinatorProfile(),
                view.isTeacherProfile(),
                view.isStudentProfile(),
                managedOrganizationIds,
                adminPermissionAssignments,
                profileContextAssignments
        );
    }

    public static UserFormData from(HttpServletRequest request, Long id) {
        return new UserFormData(
                id,
                value(request, "name"),
                value(request, "email"),
                value(request, "state"),
                value(request, "language"),
                value(request, "photo"),
                value(request, "documentType"),
                value(request, "documentNumber"),
                value(request, "password"),
                request.getParameter("administratorProfile") != null,
                request.getParameter("coordinatorProfile") != null,
                request.getParameter("teacherProfile") != null,
                request.getParameter("studentProfile") != null,
                selectedManagedOrganizationIds(request),
                selectedAdminPermissionAssignments(request),
                selectedProfileContextAssignments(request)
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state;
    }

    public String getLanguage() {
        return language;
    }

    public String getPhoto() {
        return photo;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdministratorProfile() {
        return administratorProfile;
    }

    public boolean isCoordinatorProfile() {
        return coordinatorProfile;
    }

    public boolean isTeacherProfile() {
        return teacherProfile;
    }

    public boolean isStudentProfile() {
        return studentProfile;
    }

    public Set<Long> getManagedOrganizationIds() {
        return managedOrganizationIds;
    }

    public boolean hasManagedOrganizationAssignment(long organizationId) {
        return managedOrganizationIds.contains(organizationId);
    }

    public Set<AdministratorPermissionAssignment> getAdminPermissionAssignments() {
        return adminPermissionAssignments;
    }

    public Set<AccessProfileContextAssignment> getProfileContextAssignments() {
        return profileContextAssignments;
    }

    public boolean hasAdminPermissionAssignment(String permissionCode, String contextType, long contextId) {
        if (permissionCode == null || contextType == null) {
            return false;
        }
        AdministratorPermissionAssignment assignment = new AdministratorPermissionAssignment(
                AuthorizationPolicy.canonicalAdminPermission(permissionCode),
                AccessEntityType.valueOf(contextType),
                contextId
        );
        return adminPermissionAssignments.contains(assignment);
    }

    public boolean hasAdminPermissionCode(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(permissionCode);
        return adminPermissionAssignments.stream()
                .anyMatch(assignment -> assignment.permissionCode().equals(canonicalPermission));
    }

    public boolean hasProfileContextAssignment(
            String profileType,
            String contextType,
            long contextId,
            long parentContextId
    ) {
        if (profileType == null || contextType == null) {
            return false;
        }
        Long normalizedParentContextId = parentContextId <= 0L ? null : parentContextId;
        AccessProfileContextAssignment assignment = new AccessProfileContextAssignment(
                AccessProfileType.valueOf(profileType),
                AccessEntityType.valueOf(contextType),
                contextId,
                normalizedParentContextId
        );
        return profileContextAssignments.contains(assignment);
    }

    private static Set<Long> selectedManagedOrganizationIds(HttpServletRequest request) {
        String[] values = request.getParameterValues("managedOrganizationIds");
        if (values == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                ids.add(Long.parseLong(value));
            }
        }
        return ids;
    }

    private static Set<AdministratorPermissionAssignment> selectedAdminPermissionAssignments(HttpServletRequest request) {
        String[] values = request.getParameterValues("adminPermissionAssignments");
        if (values == null) {
            return Set.of();
        }
        Set<AdministratorPermissionAssignment> assignments = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String[] parts = value.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid administrator permission assignment");
            }
            assignments.add(new AdministratorPermissionAssignment(
                    parts[0],
                    AccessEntityType.valueOf(parts[1]),
                    Long.parseLong(parts[2])
            ));
        }
        return Set.copyOf(assignments);
    }

    private static Set<AccessProfileContextAssignment> selectedProfileContextAssignments(HttpServletRequest request) {
        String[] values = request.getParameterValues("profileContextAssignments");
        if (values == null) {
            return Set.of();
        }
        Set<AccessProfileContextAssignment> assignments = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String[] parts = value.split(":");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid access profile context assignment");
            }
            long parentContextId = Long.parseLong(parts[3]);
            assignments.add(new AccessProfileContextAssignment(
                    AccessProfileType.valueOf(parts[0]),
                    AccessEntityType.valueOf(parts[1]),
                    Long.parseLong(parts[2]),
                    parentContextId <= 0L ? null : parentContextId
            ));
        }
        return Set.copyOf(assignments);
    }

    private static Set<AdministratorPermissionAssignment> assignmentsFromManagedOrganizations(Set<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Set.of();
        }
        Set<AdministratorPermissionAssignment> assignments = new LinkedHashSet<>();
        for (Long organizationId : organizationIds) {
            if (organizationId != null && organizationId > 0L) {
                assignments.add(new AdministratorPermissionAssignment(
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        AccessEntityType.ORGANIZATION,
                        organizationId
                ));
            }
        }
        return Set.copyOf(assignments);
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
