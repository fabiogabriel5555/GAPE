package pt.isel.gape.learning.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;

public final class AttemptDAO {

    private final ConnectionProvider connectionProvider;

    public AttemptDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long studentUserId,
            long assessmentId,
            int attemptNumber,
            AttemptState state,
            LocalDateTime startedAt
    ) throws SQLException {
        String sql = """
                INSERT INTO attempt (
                    id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
                ) VALUES (?, ?, ?, NULL, ?, ?, NULL)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            statement.setInt(3, attemptNumber);
            statement.setString(4, state.toDatabaseValue());
            statement.setTimestamp(5, Timestamp.valueOf(startedAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating attempt failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Attempt> findById(long attemptId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, attemptId);
        }
    }

    public Optional<Attempt> findById(Connection connection, long attemptId) throws SQLException {
        String sql = selectAttemptSql() + " WHERE id_attempt = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttempt(resultSet));
            }
        }
    }

    public List<Attempt> findByAssessment(long assessmentId) throws SQLException {
        String sql = selectAttemptSql() + """
                WHERE id_assessment = ?
                ORDER BY started_at DESC, id_attempt DESC
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        }
    }

    public List<Attempt> findByStudent(long studentUserId) throws SQLException {
        String sql = selectAttemptSql() + """
                WHERE id_student_user = ?
                ORDER BY started_at DESC, id_attempt DESC
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        }
    }

    public List<Attempt> findByStudentAndAssessment(long studentUserId, long assessmentId) throws SQLException {
        String sql = selectAttemptSql() + """
                WHERE id_student_user = ?
                  AND id_assessment = ?
                ORDER BY attempt_number DESC, id_attempt DESC
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        }
    }

    public Optional<Attempt> findLatestInProgress(long studentUserId, long assessmentId) throws SQLException {
        String sql = selectAttemptSql() + """
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'in_progress'
                ORDER BY attempt_number DESC, id_attempt DESC
                LIMIT 1
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttempt(resultSet));
            }
        }
    }

    public int countByAssessment(long assessmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attempt WHERE id_assessment = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public Optional<Attempt> lockById(Connection connection, long attemptId) throws SQLException {
        String sql = selectAttemptSql() + " WHERE id_attempt = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttempt(resultSet));
            }
        }
    }

    public void submit(
            Connection connection,
            long attemptId,
            BigDecimal score,
            AttemptState state,
            LocalDateTime submittedAt
    ) throws SQLException {
        String sql = """
                UPDATE attempt
                SET score = ?, state = ?, submitted_at = ?
                WHERE id_attempt = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableBigDecimal(statement, 1, score);
            statement.setString(2, state.toDatabaseValue());
            statement.setTimestamp(3, Timestamp.valueOf(submittedAt));
            statement.setLong(4, attemptId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Attempt not found: " + attemptId);
            }
        }
    }

    public void updateScoreAndState(
            Connection connection,
            long attemptId,
            BigDecimal score,
            AttemptState state
    ) throws SQLException {
        String sql = """
                UPDATE attempt
                SET score = ?, state = ?
                WHERE id_attempt = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableBigDecimal(statement, 1, score);
            statement.setString(2, state.toDatabaseValue());
            statement.setLong(3, attemptId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Attempt not found: " + attemptId);
            }
        }
    }

    private static String selectAttemptSql() {
        return """
                SELECT id_attempt, id_student_user, id_assessment, attempt_number,
                       score, state, started_at, submitted_at
                FROM attempt
                """;
    }

    private static Attempt mapAttempt(ResultSet resultSet) throws SQLException {
        return new Attempt(
                resultSet.getLong("id_attempt"),
                resultSet.getLong("id_student_user"),
                resultSet.getLong("id_assessment"),
                resultSet.getInt("attempt_number"),
                resultSet.getBigDecimal("score"),
                AttemptState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getTimestamp("started_at").toLocalDateTime(),
                getTimestamp(resultSet, "submitted_at")
        );
    }

    private static List<Attempt> mapAttempts(ResultSet resultSet) throws SQLException {
        List<Attempt> attempts = new ArrayList<>();
        while (resultSet.next()) {
            attempts.add(mapAttempt(resultSet));
        }
        return attempts;
    }

    private static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableBigDecimal(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }
}
