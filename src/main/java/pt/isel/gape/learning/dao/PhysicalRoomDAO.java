package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomCreateCommand;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.PhysicalRoomUpdateCommand;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class PhysicalRoomDAO implements pt.isel.gape.transversal.service.ApplicationReadService.PhysicalRooms {

    private final ConnectionProvider connectionProvider;

    public PhysicalRoomDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public String create(Connection connection, PhysicalRoomCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO physical_room (
                    cod_physical_room, id_organization, id_organic_unit, name,
                    description, capacity, location, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.code().trim());
            statement.setLong(2, command.organizationId());
            setNullableLong(statement, 3, command.organicUnitId());
            statement.setString(4, command.name().trim());
            setNullableString(statement, 5, command.description());
            statement.setInt(6, command.capacity());
            setNullableString(statement, 7, command.location());
            statement.setString(8, command.state().toDatabaseValue());
            statement.executeUpdate();
            return command.code().trim();
        }
    }

    public Optional<PhysicalRoom> findByCode(String code) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByCode(connection, code);
        }
    }

    public Optional<PhysicalRoom> findByCode(Connection connection, String code) throws SQLException {
        String sql = """
                SELECT cod_physical_room, id_organization, id_organic_unit, name,
                       description, capacity, location, state
                FROM physical_room
                WHERE cod_physical_room = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPhysicalRoom(resultSet));
            }
        }
    }

    public Optional<PhysicalRoom> lockByCode(Connection connection, String code) throws SQLException {
        String sql = """
                SELECT cod_physical_room, id_organization, id_organic_unit, name,
                       description, capacity, location, state
                FROM physical_room
                WHERE cod_physical_room = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPhysicalRoom(resultSet));
            }
        }
    }

    public List<PhysicalRoom> findByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT cod_physical_room, id_organization, id_organic_unit, name,
                       description, capacity, location, state
                FROM physical_room
                WHERE id_organization = ?
                ORDER BY cod_physical_room
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapPhysicalRooms(resultSet);
            }
        }
    }

    public List<PhysicalRoom> findByOrganicUnit(long organicUnitId) throws SQLException {
        String sql = """
                SELECT cod_physical_room, id_organization, id_organic_unit, name,
                       description, capacity, location, state
                FROM physical_room
                WHERE id_organic_unit = ?
                ORDER BY cod_physical_room
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organicUnitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapPhysicalRooms(resultSet);
            }
        }
    }

    public void update(Connection connection, String code, PhysicalRoomUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE physical_room
                SET id_organization = ?, id_organic_unit = ?, name = ?, description = ?,
                    capacity = ?, location = ?, state = ?
                WHERE cod_physical_room = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.organizationId());
            setNullableLong(statement, 2, command.organicUnitId());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.description());
            statement.setInt(5, command.capacity());
            setNullableString(statement, 6, command.location());
            statement.setString(7, command.state().toDatabaseValue());
            statement.setString(8, code);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Physical room not found: " + code);
            }
        }
    }

    public void updateState(Connection connection, String code, PhysicalRoomState state) throws SQLException {
        String sql = "UPDATE physical_room SET state = ? WHERE cod_physical_room = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setString(2, code);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Physical room not found: " + code);
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, String code) throws SQLException {
        String sql = """
                SELECT (
                    SELECT COUNT(*)
                    FROM lesson
                    WHERE cod_physical_room = ?
                ) + (
                    SELECT COUNT(*)
                    FROM assessment
                    WHERE cod_physical_room = ?
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setString(2, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasReservedLessons(Connection connection, String code) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM lesson
                WHERE cod_physical_room = ?
                  AND state IN ('scheduled', 'active')
                  AND type IN ('onsite', 'hybrid')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasReservedLessonExceedingCapacity(
            Connection connection,
            String code,
            int capacity
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM lesson l
                WHERE l.cod_physical_room = ?
                  AND l.state IN ('scheduled', 'active')
                  AND l.type IN ('onsite', 'hybrid')
                  AND (
                      SELECT COUNT(*)
                      FROM enroll_class_group ecg
                      WHERE ecg.id_class_group = l.id_class_group
                        AND ecg.state = 'active'
                        AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                        AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                  ) > ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setInt(2, capacity);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public void delete(Connection connection, String code) throws SQLException {
        String sql = "DELETE FROM physical_room WHERE cod_physical_room = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Physical room not found: " + code);
            }
        }
    }

    public boolean organizationExists(Connection connection, long organizationId) throws SQLException {
        return exists(connection, """
                SELECT COUNT(*)
                FROM organization
                WHERE id_organization = ?
                  AND state = 'active'
                """, organizationId);
    }

    public boolean organicUnitBelongsToOrganization(
            Connection connection,
            long organicUnitId,
            long organizationId
    ) throws SQLException {
        return exists(connection, """
                SELECT COUNT(*)
                FROM organic_unit
                WHERE id_organic_unit = ?
                  AND id_organization = ?
                  AND state = 'active'
                """, organicUnitId, organizationId);
    }

    public boolean coordinatorCanManageOrganization(
            Connection connection,
            long coordinatorUserId,
            long organizationId
    ) throws SQLException {
        return exists(connection, """
                SELECT COUNT(*)
                FROM coordinate_subject cs
                JOIN subject s ON s.id_subject = cs.id_subject
                JOIN coordinator_profile cp ON cp.id_user = cs.id_coordinator_user
                JOIN user_account u ON u.id_user = cp.id_user
                JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                JOIN permission p ON p.cod_permission = gc.cod_permission
                WHERE cs.id_coordinator_user = ?
                  AND s.id_organization = ?
                  AND cs.state = 'active'
                  AND s.state = 'active'
                  AND u.state = 'active'
                  AND gc.cod_permission = ?
                  AND p.state = 'active'
                """, coordinatorUserId, organizationId, AuthorizationPolicy.MANAGE_LEARNING);
    }

    public boolean teacherCanReadOrganization(
            Connection connection,
            long teacherUserId,
            long organizationId
    ) throws SQLException {
        return exists(connection, """
                SELECT COUNT(*)
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                JOIN course c ON c.id_course = cg.id_course
                WHERE tcg.id_teacher_user = ?
                  AND c.id_organization = ?
                  AND tcg.state = 'active'
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND u.state = 'active'
                  AND gt.cod_permission = ?
                  AND p.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """, teacherUserId, organizationId, AuthorizationPolicy.MANAGE_LEARNING);
    }

    private static boolean exists(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                Object value = values[index];
                if (value instanceof Long longValue) {
                    statement.setLong(index + 1, longValue);
                } else if (value instanceof String stringValue) {
                    statement.setString(index + 1, stringValue);
                } else {
                    throw new IllegalArgumentException("Unsupported SQL parameter type: " + value);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static List<PhysicalRoom> mapPhysicalRooms(ResultSet resultSet) throws SQLException {
        List<PhysicalRoom> rooms = new ArrayList<>();
        while (resultSet.next()) {
            rooms.add(mapPhysicalRoom(resultSet));
        }
        return rooms;
    }

    private static PhysicalRoom mapPhysicalRoom(ResultSet resultSet) throws SQLException {
        return new PhysicalRoom(
                resultSet.getString("cod_physical_room"),
                resultSet.getLong("id_organization"),
                nullableLong(resultSet, "id_organic_unit"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("capacity"),
                resultSet.getString("location"),
                PhysicalRoomState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
