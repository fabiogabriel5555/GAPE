package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionState;
import pt.isel.gape.learning.model.QuestionOptionUpdateCommand;

public final class QuestionOptionDAO {

    private final ConnectionProvider connectionProvider;

    public QuestionOptionDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, QuestionOptionCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO question_option (id_question, order_no, text, correct_flag, state)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.questionId());
            statement.setInt(2, command.orderNo());
            statement.setString(3, command.text().trim());
            setNullableBoolean(statement, 4, command.correct());
            statement.setString(5, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating question option failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<QuestionOption> findById(long optionId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, optionId);
        }
    }

    public Optional<QuestionOption> findById(Connection connection, long optionId) throws SQLException {
        String sql = selectOptionSql() + " WHERE id_option = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, optionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOption(resultSet));
            }
        }
    }

    public Optional<QuestionOption> lockById(Connection connection, long optionId) throws SQLException {
        String sql = selectOptionSql() + " WHERE id_option = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, optionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOption(resultSet));
            }
        }
    }

    public List<QuestionOption> findByQuestion(Connection connection, long questionId) throws SQLException {
        String sql = selectOptionSql() + """
                WHERE id_question = ?
                ORDER BY order_no, id_option
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapOptions(resultSet);
            }
        }
    }

    public List<QuestionOption> lockByQuestion(Connection connection, long questionId) throws SQLException {
        String sql = selectOptionSql() + """
                WHERE id_question = ?
                ORDER BY order_no, id_option
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapOptions(resultSet);
            }
        }
    }

    public List<QuestionOption> findByQuestion(long questionId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByQuestion(connection, questionId);
        }
    }

    public List<QuestionOption> findActiveByQuestion(long questionId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findActiveByQuestion(connection, questionId);
        }
    }

    public List<QuestionOption> findSelectedOptions(long responseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findSelectedOptions(connection, responseId);
        }
    }

    public List<QuestionOption> findActiveByQuestion(Connection connection, long questionId) throws SQLException {
        String sql = selectOptionSql() + """
                WHERE id_question = ?
                  AND state = 'active'
                ORDER BY order_no, id_option
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapOptions(resultSet);
            }
        }
    }

    public List<QuestionOption> findSelectedOptions(Connection connection, long responseId) throws SQLException {
        String sql = selectOptionSql() + """
                JOIN response_option ro ON ro.id_option = question_option.id_option
                WHERE ro.id_response = ?
                ORDER BY question_option.order_no, question_option.id_option
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, responseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapOptions(resultSet);
            }
        }
    }

    public void update(Connection connection, long optionId, QuestionOptionUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE question_option
                SET order_no = ?, text = ?, correct_flag = ?, state = ?
                WHERE id_option = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, command.orderNo());
            statement.setString(2, command.text().trim());
            setNullableBoolean(statement, 3, command.correct());
            statement.setString(4, command.state().toDatabaseValue());
            statement.setLong(5, optionId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Question option not found: " + optionId);
            }
        }
    }

    public void shiftOrders(Connection connection, long questionId, int offset) throws SQLException {
        String sql = """
                UPDATE question_option
                SET order_no = order_no + ?
                WHERE id_question = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, offset);
            statement.setLong(2, questionId);
            statement.executeUpdate();
        }
    }

    public long countActiveCorrectOptions(Connection connection, long questionId, Long excludedOptionId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM question_option
                WHERE id_question = ?
                  AND state = 'active'
                  AND correct_flag = 1
                  AND (? IS NULL OR id_option <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            if (excludedOptionId == null) {
                statement.setNull(2, Types.BIGINT);
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(2, excludedOptionId);
                statement.setLong(3, excludedOptionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public long countActiveOptions(Connection connection, long questionId, Long excludedOptionId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM question_option
                WHERE id_question = ?
                  AND state = 'active'
                  AND (? IS NULL OR id_option <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            if (excludedOptionId == null) {
                statement.setNull(2, Types.BIGINT);
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(2, excludedOptionId);
                statement.setLong(3, excludedOptionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public boolean allOptionsBelongToQuestion(
            Connection connection,
            long questionId,
            Collection<Long> optionIds
    ) throws SQLException {
        if (optionIds == null || optionIds.isEmpty()) {
            return true;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(optionIds.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM question_option
                WHERE id_question = ?
                  AND state = 'active'
                  AND id_option IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, questionId);
            int index = 2;
            for (Long optionId : optionIds) {
                statement.setLong(index++, optionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == optionIds.size();
            }
        }
    }

    private static String selectOptionSql() {
        return """
                SELECT question_option.id_option, question_option.id_question, question_option.order_no,
                       question_option.text, question_option.correct_flag, question_option.state
                FROM question_option
                """;
    }

    private static List<QuestionOption> mapOptions(ResultSet resultSet) throws SQLException {
        List<QuestionOption> options = new ArrayList<>();
        while (resultSet.next()) {
            options.add(mapOption(resultSet));
        }
        return options;
    }

    private static QuestionOption mapOption(ResultSet resultSet) throws SQLException {
        Boolean correct = resultSet.getBoolean("correct_flag");
        if (resultSet.wasNull()) {
            correct = null;
        }
        return new QuestionOption(
                resultSet.getLong("id_option"),
                resultSet.getLong("id_question"),
                resultSet.getInt("order_no"),
                resultSet.getString("text"),
                correct,
                QuestionOptionState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static void setNullableBoolean(PreparedStatement statement, int index, Boolean value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BOOLEAN);
        } else {
            statement.setBoolean(index, value);
        }
    }
}
