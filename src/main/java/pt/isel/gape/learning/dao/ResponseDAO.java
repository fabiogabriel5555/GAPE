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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Response;

public final class ResponseDAO implements pt.isel.gape.transversal.service.ApplicationReadService.Responses {

    private final ConnectionProvider connectionProvider;

    public ResponseDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long attemptId,
            long questionId,
            String code,
            String answer,
            String attachment,
            BigDecimal score,
            LocalDateTime answeredAt
    ) throws SQLException {
        String sql = """
                INSERT INTO response (
                    id_attempt, id_question, cod_response, answer, attachment, score, answered_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, attemptId);
            statement.setLong(2, questionId);
            statement.setString(3, code);
            setNullableString(statement, 4, answer);
            setNullableString(statement, 5, attachment);
            setNullableBigDecimal(statement, 6, score);
            statement.setTimestamp(7, Timestamp.valueOf(answeredAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating response failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Response> findById(long responseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, responseId);
        }
    }

    public Optional<Response> findById(Connection connection, long responseId) throws SQLException {
        String sql = selectResponseSql() + " WHERE id_response = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, responseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapResponse(resultSet));
            }
        }
    }

    public Optional<Response> findByAttemptAndQuestion(Connection connection, long attemptId, long questionId)
            throws SQLException {
        String sql = selectResponseSql() + " WHERE id_attempt = ? AND id_question = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            statement.setLong(2, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapResponse(resultSet));
            }
        }
    }

    public List<Response> findByAttempt(Connection connection, long attemptId) throws SQLException {
        String sql = selectResponseSql() + " WHERE id_attempt = ? ORDER BY id_response";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResponses(resultSet);
            }
        }
    }

    public List<Response> findByAttempt(long attemptId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByAttempt(connection, attemptId);
        }
    }

    public void update(
            Connection connection,
            long responseId,
            String answer,
            String attachment,
            BigDecimal score,
            LocalDateTime answeredAt
    ) throws SQLException {
        String sql = """
                UPDATE response
                SET answer = ?, attachment = ?, score = ?, answered_at = ?
                WHERE id_response = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, answer);
            setNullableString(statement, 2, attachment);
            setNullableBigDecimal(statement, 3, score);
            statement.setTimestamp(4, Timestamp.valueOf(answeredAt));
            statement.setLong(5, responseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Response not found: " + responseId);
            }
        }
    }

    public void updateScore(Connection connection, long responseId, BigDecimal score) throws SQLException {
        String sql = "UPDATE response SET score = ? WHERE id_response = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableBigDecimal(statement, 1, score);
            statement.setLong(2, responseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Response not found: " + responseId);
            }
        }
    }

    public void deleteSelectedOptions(Connection connection, long responseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM response_option WHERE id_response = ?")) {
            statement.setLong(1, responseId);
            statement.executeUpdate();
        }
    }

    public void insertSelectedOptions(Connection connection, long responseId, Collection<Long> optionIds)
            throws SQLException {
        if (optionIds == null || optionIds.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO response_option (id_response, id_option) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Long optionId : optionIds) {
                statement.setLong(1, responseId);
                statement.setLong(2, optionId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public BigDecimal sumScoresByAttempt(Connection connection, long attemptId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(score), 0)
                FROM response
                WHERE id_attempt = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    public BigDecimal sumScoresByActiveQuestionsByAttempt(Connection connection, long attemptId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(score), 0)
                FROM response
                WHERE id_attempt = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    private static String selectResponseSql() {
        return """
                SELECT id_response, id_attempt, id_question, cod_response, answer,
                       attachment, score, answered_at
                FROM response
                """;
    }

    private static List<Response> mapResponses(ResultSet resultSet) throws SQLException {
        List<Response> responses = new ArrayList<>();
        while (resultSet.next()) {
            responses.add(mapResponse(resultSet));
        }
        return responses;
    }

    private static Response mapResponse(ResultSet resultSet) throws SQLException {
        return new Response(
                resultSet.getLong("id_response"),
                resultSet.getLong("id_attempt"),
                resultSet.getLong("id_question"),
                resultSet.getString("cod_response"),
                resultSet.getString("answer"),
                resultSet.getString("attachment"),
                resultSet.getBigDecimal("score"),
                getTimestamp(resultSet, "answered_at")
        );
    }

    private static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
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
