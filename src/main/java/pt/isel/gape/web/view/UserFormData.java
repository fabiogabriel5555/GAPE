package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.config.SupportedLanguageCatalog;

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
            boolean studentProfile
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
                true
        );
    }

    public static UserFormData from(User user) {
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
                view.isStudentProfile()
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
                request.getParameter("studentProfile") != null
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

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
