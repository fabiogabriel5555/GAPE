package pt.isel.gape.access.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;

public final class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO is required");
    }

    public UserService(ConnectionProvider connectionProvider) {
        this(new UserDAO(connectionProvider));
    }

    public Optional<User> findByEmail(String email) {
        try {
            return userDAO.findByEmail(email);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load user by email", exception);
        }
    }

    public Optional<User> findById(long userId) {
        try {
            return userDAO.findById(userId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load user by id", exception);
        }
    }
}
