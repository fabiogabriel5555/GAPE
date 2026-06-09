package pt.isel.gape.access.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.ProfileDAO;
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;

public final class ProfileService {

    private final ProfileDAO profileDAO;

    public ProfileService(ProfileDAO profileDAO) {
        this.profileDAO = Objects.requireNonNull(profileDAO, "profileDAO is required");
    }

    public ProfileService(ConnectionProvider connectionProvider) {
        this(new ProfileDAO(connectionProvider));
    }

    public Set<AccessProfile> findByUserId(long userId) {
        try {
            return profileDAO.findByUserId(userId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load profiles for user " + userId, exception);
        }
    }

    public boolean exists(long userId, AccessProfileType profileType) {
        try {
            return profileDAO.exists(userId, profileType);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check profile " + profileType + " for user " + userId, exception);
        }
    }
}
