package pt.isel.gape.web.view;

import pt.isel.gape.access.model.User;

public final class UserOptionView {

    private final long id;
    private final String name;
    private final String email;
    private final boolean selected;

    private UserOptionView(User user, boolean selected) {
        this.id = user.id();
        this.name = user.name();
        this.email = user.email();
        this.selected = selected;
    }

    public static UserOptionView from(User user, boolean selected) {
        return new UserOptionView(user, selected);
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

    public boolean isSelected() {
        return selected;
    }
}
