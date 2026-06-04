package pt.isel.gape.common.config;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionProvider {

    Connection getConnection() throws SQLException;

    static ConnectionProvider defaultProvider() {
        return DatabaseConfig::getConnection;
    }
}
