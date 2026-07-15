package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CoursePeriodTemplate;
import pt.isel.gape.learning.model.CoursePeriodTemplateCommand;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CoursePeriodTemplateDAO implements pt.isel.gape.transversal.service.ApplicationReadService.CoursePeriodTemplates {

    private final ConnectionProvider connectionProvider;

    public CoursePeriodTemplateDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<CoursePeriodTemplate> findByCourse(long courseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByCourse(connection, courseId);
        }
    }

    public List<CoursePeriodTemplate> findByCourse(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT id_course_period_template, id_course, curricular_year, term,
                       starts_month, starts_day, ends_month, ends_day
                FROM course_period_template
                WHERE id_course = ?
                ORDER BY curricular_year, starts_month, starts_day, id_course_period_template
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CoursePeriodTemplate> templates = new ArrayList<>();
                while (resultSet.next()) {
                    templates.add(map(resultSet));
                }
                return List.copyOf(templates);
            }
        }
    }

    public void replaceForCourse(
            Connection connection,
            long courseId,
            List<CoursePeriodTemplateCommand> templates
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM course_period_template WHERE id_course = ?"
        )) {
            delete.setLong(1, courseId);
            delete.executeUpdate();
        }
        String sql = """
                INSERT INTO course_period_template (
                    id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (CoursePeriodTemplateCommand template : templates) {
                statement.setLong(1, courseId);
                statement.setInt(2, template.curricularYear());
                statement.setString(3, template.term().toDatabaseValue());
                statement.setInt(4, template.startsMonth());
                statement.setInt(5, template.startsDay());
                statement.setInt(6, template.endsMonth());
                statement.setInt(7, template.endsDay());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static CoursePeriodTemplate map(ResultSet resultSet) throws SQLException {
        return new CoursePeriodTemplate(
                resultSet.getLong("id_course_period_template"),
                resultSet.getLong("id_course"),
                resultSet.getInt("curricular_year"),
                CurricularTerm.fromDatabaseValue(resultSet.getString("term")),
                resultSet.getInt("starts_month"),
                resultSet.getInt("starts_day"),
                resultSet.getInt("ends_month"),
                resultSet.getInt("ends_day")
        );
    }
}
