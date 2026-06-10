package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitCreateCommand;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;
import pt.isel.gape.structure.model.OrganicUnitUpdateCommand;

public final class OrganicUnitDAO {

    private final ConnectionProvider connectionProvider;

    public OrganicUnitDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, OrganicUnitCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO organic_unit (
                    id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.organizationId());
            statement.setString(2, command.code().trim());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.acronym());
            statement.setString(5, command.type().toDatabaseValue());
            statement.setString(6, command.state().toDatabaseValue());
            setNullableLong(statement, 7, command.parentOrganicUnitId());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating organic unit failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<OrganicUnit> findById(long organicUnitId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, organicUnitId);
        }
    }

    public Optional<OrganicUnit> findById(Connection connection, long organicUnitId) throws SQLException {
        String sql = """
                SELECT id_organic_unit, id_organization, cod_organic_unit, name, acronym,
                       type, state, parent_organic_unit_id
                FROM organic_unit
                WHERE id_organic_unit = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organicUnitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOrganicUnit(resultSet));
            }
        }
    }

    public List<OrganicUnit> findByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT id_organic_unit, id_organization, cod_organic_unit, name, acronym,
                       type, state, parent_organic_unit_id
                FROM organic_unit
                WHERE id_organization = ?
                ORDER BY cod_organic_unit
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OrganicUnit> units = new ArrayList<>();
                while (resultSet.next()) {
                    units.add(mapOrganicUnit(resultSet));
                }
                return units;
            }
        }
    }

    public void update(Connection connection, long organicUnitId, OrganicUnitUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE organic_unit
                SET cod_organic_unit = ?, name = ?, acronym = ?, type = ?, state = ?, parent_organic_unit_id = ?
                WHERE id_organic_unit = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.code().trim());
            statement.setString(2, command.name().trim());
            setNullableString(statement, 3, command.acronym());
            statement.setString(4, command.type().toDatabaseValue());
            statement.setString(5, command.state().toDatabaseValue());
            setNullableLong(statement, 6, command.parentOrganicUnitId());
            statement.setLong(7, organicUnitId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organic unit not found: " + organicUnitId);
            }
        }
    }

    public void updateState(Connection connection, long organicUnitId, OrganicUnitState state) throws SQLException {
        String sql = """
                UPDATE organic_unit
                SET state = ?
                WHERE id_organic_unit = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, organicUnitId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organic unit not found: " + organicUnitId);
            }
        }
    }

    public List<String> findCodesByPrefix(Connection connection, long organizationId, String prefix) throws SQLException {
        String sql = """
                SELECT cod_organic_unit
                FROM organic_unit
                WHERE id_organization = ?
                  AND cod_organic_unit LIKE ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            statement.setString(2, prefix + "-%");
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> codes = new ArrayList<>();
                while (resultSet.next()) {
                    codes.add(resultSet.getString("cod_organic_unit"));
                }
                return codes;
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long organicUnitId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM organic_unit WHERE parent_organic_unit_id = ?)
                  + (SELECT COUNT(*) FROM course WHERE id_organic_unit = ?)
                  + (SELECT COUNT(*) FROM physical_room WHERE id_organic_unit = ?)
                  + (SELECT COUNT(*) FROM associate_organic_unit_content WHERE id_organic_unit = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organicUnitId);
            statement.setLong(2, organicUnitId);
            statement.setLong(3, organicUnitId);
            statement.setLong(4, organicUnitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long organicUnitId) throws SQLException {
        String sql = """
                DELETE FROM organic_unit
                WHERE id_organic_unit = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organicUnitId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organic unit not found: " + organicUnitId);
            }
        }
    }

    private static OrganicUnit mapOrganicUnit(ResultSet resultSet) throws SQLException {
        long parentId = resultSet.getLong("parent_organic_unit_id");
        boolean parentWasNull = resultSet.wasNull();
        return new OrganicUnit(
                resultSet.getLong("id_organic_unit"),
                resultSet.getLong("id_organization"),
                resultSet.getString("cod_organic_unit"),
                resultSet.getString("name"),
                resultSet.getString("acronym"),
                OrganicUnitType.fromDatabaseValue(resultSet.getString("type")),
                OrganicUnitState.fromDatabaseValue(resultSet.getString("state")),
                parentWasNull ? null : parentId
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
