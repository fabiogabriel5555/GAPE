package pt.isel.gape.web.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;

public final class UserView {

    private final long id;
    private final String name;
    private final String email;
    private final UserState state;
    private final String language;
    private final String photo;
    private final String createdAt;
    private final String createdAtSort;
    private final String documentType;
    private final String documentNumber;
    private final Map<AccessProfileType, String> profileCodes;

    private UserView(User user) {
        this.id = user.id();
        this.name = user.name();
        this.email = user.email();
        this.state = user.state();
        this.language = user.language();
        this.photo = user.photo();
        this.createdAt = ApplicationDateTimeFormat.dateTime(user.createdAt());
        this.createdAtSort = user.createdAt().toString();
        this.documentType = user.documentType();
        this.documentNumber = user.documentNumber();
        this.profileCodes = new LinkedHashMap<>();
        for (AccessProfile profile : user.accessProfiles()) {
            this.profileCodes.put(profile.type(), profile.code());
        }
    }

    public static UserView from(User user) {
        return new UserView(user);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case BLOCKED -> "Blocked";
        };
    }

    public String getStateLabelEnglish() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case BLOCKED -> "Blocked";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
            case BLOCKED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return state == UserState.ACTIVE;
    }

    public boolean isBlocked() {
        return state == UserState.BLOCKED;
    }

    public boolean isInactive() {
        return state == UserState.INACTIVE;
    }

    public String getLanguage() {
        return language;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean isHasPhoto() {
        return photo != null && !photo.isBlank();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtSort() {
        return createdAtSort;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getDocumentLabel() {
        if (documentType == null || documentNumber == null) {
            return "No document";
        }
        return SupportedDocumentTypeCatalog.labelFor(documentType) + " / " + documentNumber;
    }

    public String getProfileSummary() {
        if (profileCodes.isEmpty()) {
            return "No profile";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (AccessProfileType profileType : AccessProfileType.values()) {
            if (profileCodes.containsKey(profileType)) {
                joiner.add(profileLabel(profileType));
            }
        }
        return joiner.toString();
    }

    public boolean isAdministratorProfile() {
        return profileCodes.containsKey(AccessProfileType.ADMINISTRATOR);
    }

    public boolean isCoordinatorProfile() {
        return profileCodes.containsKey(AccessProfileType.COORDINATOR);
    }

    public boolean isTeacherProfile() {
        return profileCodes.containsKey(AccessProfileType.TEACHER);
    }

    public boolean isStudentProfile() {
        return profileCodes.containsKey(AccessProfileType.STUDENT);
    }

    private static String profileLabel(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        };
    }
}
