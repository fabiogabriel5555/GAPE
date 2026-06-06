package pt.isel.gape.access.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.Permission;
import pt.isel.gape.common.config.ConnectionProvider;

public final class PermissionService {

    private final PermissionDAO permissionDAO;

    public PermissionService(PermissionDAO permissionDAO) {
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
    }

    public PermissionService(ConnectionProvider connectionProvider) {
        this(new PermissionDAO(connectionProvider));
    }

    public Optional<Permission> findByCode(String permissionCode) {
        try {
            return permissionDAO.findByCode(permissionCode);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load permission " + permissionCode, exception);
        }
    }

    public Permission requireActive(String permissionCode) {
        Permission permission = findByCode(permissionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown permission: " + permissionCode));
        if (!permission.isActive()) {
            throw new IllegalArgumentException("Inactive permission cannot be used: " + permissionCode);
        }
        return permission;
    }
}
