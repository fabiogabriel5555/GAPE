package pt.isel.gape.learning.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.QuestionUpdateCommand;

public final class QuestionDAO {

    private final ConnectionProvider connectionProvider;

    public QuestionDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, QuestionCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO question (
                    id_assessment, cod_question, statement, type, order_no,
                    required_flag, score, expected_answer
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.assessmentId());
            setStatementValues(statement, command);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating question failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Question> findById(long questionId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, questionId);
        }
    }

    public Optional<Question> findById(Connection connection, long questionId) throws SQLException {
        String sql = selectQuestionSql() + " WHERE id_question = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapQuestion(resultSet));
            }
        }
    }

    public Optional<Question> lockById(Connection connection, long questionId) throws SQLException {
        String sql = selectQuestionSql() + " WHERE id_question = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapQuestion(resultSet));
            }
        }
    }

    public List<Question> findByAssessment(Connection connection, long assessmentId) throws SQLException {
        String sql = selectQuestionSql() + """
                WHERE id_assessment = ?
                ORDER BY order_no, id_question
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapQuestions(resultSet);
            }
        }
    }

    public List<Question> lockByAssessment(Connection connection, long assessmentId) throws SQLException {
        String sql = selectQuestionSql() + """
                WHERE id_assessment = ?
                ORDER BY order_no, id_question
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapQuestions(resultSet);
            }
        }
    }

    public List<Question> findByAssessment(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByAssessment(connection, assessmentId);
        }
    }

    public int countByAssessment(long assessmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM question WHERE id_assessment = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public int countActiveByAssessment(Connection connection, long assessmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM question WHERE id_assessment = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public int countActiveByAssessment(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return countActiveByAssessment(connection, assessmentId);
        }
    }

    public void update(Connection connection, long questionId, QuestionUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE question
                SET cod_question = ?, statement = ?, type = ?, order_no = ?,
                    required_flag = ?, score = ?, expected_answer = ?
                WHERE id_question = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setStatementValues(statement, command);
            statement.setLong(8, questionId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Question not found: " + questionId);
            }
        }
    }

    public void shiftOrders(Connection connection, long assessmentId, int offset) throws SQLException {
        String sql = """
                UPDATE question
                SET order_no = order_no + ?
                WHERE id_assessment = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, offset);
            statement.setLong(2, assessmentId);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, long questionId) throws SQLException {
        String sql = "DELETE FROM question WHERE id_question = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Question not found: " + questionId);
            }
        }
    }

    public BigDecimal sumActiveScores(Connection connection, long assessmentId, Long excludedQuestionId)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(score), 0)
                FROM question
                WHERE id_assessment = ?
                  AND (? IS NULL OR id_question <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            if (excludedQuestionId == null) {
                statement.setNull(2, Types.BIGINT);
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(2, excludedQuestionId);
                statement.setLong(3, excludedQuestionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    public long countMissingRequiredResponses(Connection connection, long attemptId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM question q
                LEFT JOIN response r
                  ON r.id_question = q.id_question
                 AND r.id_attempt = ?
                WHERE q.id_assessment = ?
                  AND q.required_flag = 1
                  AND r.id_response IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public long countUnscoredActiveResponses(Connection connection, long attemptId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM question q
                LEFT JOIN response r
                  ON r.id_question = q.id_question
                 AND r.id_attempt = ?
                WHERE q.id_assessment = ?
                  AND (r.id_response IS NULL OR r.score IS NULL)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attemptId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void setStatementValues(PreparedStatement statement, QuestionCreateCommand command)
            throws SQLException {
        statement.setString(2, command.code().trim());
        statement.setString(3, command.statement().trim());
        statement.setString(4, command.type().toDatabaseValue());
        statement.setInt(5, command.orderNo());
        statement.setBoolean(6, command.required());
        statement.setBigDecimal(7, command.score());
        setNullableString(statement, 8, command.expectedAnswer());
    }

    private static void setStatementValues(PreparedStatement statement, QuestionUpdateCommand command)
            throws SQLException {
        statement.setString(1, command.code().trim());
        statement.setString(2, command.statement().trim());
        statement.setString(3, command.type().toDatabaseValue());
        statement.setInt(4, command.orderNo());
        statement.setBoolean(5, command.required());
        statement.setBigDecimal(6, command.score());
        setNullableString(statement, 7, command.expectedAnswer());
    }

    private static String selectQuestionSql() {
        return """
                SELECT id_question, id_assessment, cod_question, statement, type,
                       order_no, required_flag, score, expected_answer
                FROM question
                """;
    }

    private static List<Question> mapQuestions(ResultSet resultSet) throws SQLException {
        List<Question> questions = new ArrayList<>();
        while (resultSet.next()) {
            questions.add(mapQuestion(resultSet));
        }
        return questions;
    }

    private static Question mapQuestion(ResultSet resultSet) throws SQLException {
        return new Question(
                resultSet.getLong("id_question"),
                resultSet.getLong("id_assessment"),
                resultSet.getString("cod_question"),
                resultSet.getString("statement"),
                QuestionType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getInt("order_no"),
                resultSet.getBoolean("required_flag"),
                resultSet.getBigDecimal("score"),
                resultSet.getString("expected_answer")
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
