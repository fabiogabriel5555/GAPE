package pt.isel.gape.web.view;

import pt.isel.gape.access.model.User;

public final class OrganicUnitAdministratorView {

    private final long userId;
    private final String name;
    private final String email;
    private final boolean canRevoke;

    private OrganicUnitAdministratorView(User user, boolean canRevoke) {
        this.userId = user.id();
        this.name = user.name();
        this.email = user.email();
        this.canRevoke = canRevoke;
    }

    public static OrganicUnitAdministratorView from(User user, boolean canRevoke) {
        return new OrganicUnitAdministratorView(user, canRevoke);
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isCanRevoke() {
        return canRevoke;
    }
}
