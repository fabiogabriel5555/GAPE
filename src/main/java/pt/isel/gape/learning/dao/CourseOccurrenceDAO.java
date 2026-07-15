package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceContext;
import pt.isel.gape.learning.model.CourseOccurrencePeriod;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CourseOccurrenceDAO implements pt.isel.gape.transversal.service.ApplicationReadService.CourseOccurrences {

    private final ConnectionProvider connectionProvider;

    public CourseOccurrenceDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long courseId,
            int referenceYear,
            String code,
            LocalDate startsAt,
            LocalDate endsAt,
            CourseOccurrenceState state
    ) throws SQLException {
        String sql = """
                INSERT INTO course_occurrence (id_course, reference_year, label, starts_at, ends_at, state)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, courseId);
            statement.setInt(2, referenceYear);
            statement.setString(3, code);
            statement.setDate(4, Date.valueOf(startsAt));
            statement.setDate(5, Date.valueOf(endsAt));
            statement.setString(6, state.toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating course occurrence failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public boolean existsForReferenceYear(Connection connection, long courseId, int referenceYear) throws SQLException {
        String sql = """
                SELECT 1
                FROM course_occurrence
                WHERE id_course = ? AND reference_year = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setInt(2, referenceYear);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<LocalDate> findLatestEndDate(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT MAX(ends_at)
                FROM course_occurrence
                WHERE id_course = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                Date endDate = resultSet.getDate(1);
                return endDate == null ? Optional.empty() : Optional.of(endDate.toLocalDate());
            }
        }
    }

    public long createPeriod(
            Connection connection,
            long courseOccurrenceId,
            int curricularYear,
            CurricularTerm term,
            LocalDate startsAt,
            LocalDate endsAt,
            CourseOccurrenceState state
    ) throws SQLException {
        String sql = """
                INSERT INTO course_occurrence_period (
                    id_course_occurrence, curricular_year, term, starts_at, ends_at, state
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, courseOccurrenceId);
            statement.setInt(2, curricularYear);
            statement.setString(3, term.toDatabaseValue());
            statement.setDate(4, Date.valueOf(startsAt));
            statement.setDate(5, Date.valueOf(endsAt));
            statement.setString(6, state.toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating course occurrence period failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    /**
     * Persists the date-derived lifecycle of occurrences and their concrete
     * periods.  Read queries already expose the same derived value, but this
     * pass keeps stored data and dependent records coherent as time moves.
     */
    public int synchronizeTemporalStates(Connection connection, LocalDate today) throws SQLException {
        String occurrenceSql = """
                UPDATE course_occurrence
                SET state = CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > ? THEN 'scheduled'
                    WHEN ends_at < ? THEN 'completed'
                    ELSE 'active'
                END
                WHERE state <> CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > ? THEN 'scheduled'
                    WHEN ends_at < ? THEN 'completed'
                    ELSE 'active'
                END
                """;
        String periodSql = """
                UPDATE course_occurrence_period
                SET state = CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > ? THEN 'scheduled'
                    WHEN ends_at < ? THEN 'completed'
                    ELSE 'active'
                END
                WHERE state <> CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > ? THEN 'scheduled'
                    WHEN ends_at < ? THEN 'completed'
                    ELSE 'active'
                END
                """;
        return executeTemporalStateUpdate(connection, occurrenceSql, today)
                + executeTemporalStateUpdate(connection, periodSql, today);
    }

    public Optional<CourseOccurrence> findById(long courseOccurrenceId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, courseOccurrenceId);
        }
    }

    public Optional<CourseOccurrence> findById(Connection connection, long courseOccurrenceId) throws SQLException {
        String sql = """
                SELECT id_course_occurrence, id_course, reference_year, label AS code, starts_at, ends_at,
                       CASE
                           WHEN state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > ends_at THEN 'completed'
                           ELSE 'active'
                       END AS state
                FROM course_occurrence
                WHERE id_course_occurrence = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOccurrence(resultSet));
            }
        }
    }

    public List<CourseOccurrence> findAll() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = """
                    SELECT id_course_occurrence, id_course, reference_year, label AS code, starts_at, ends_at,
                           CASE
                               WHEN state = 'cancelled' THEN 'cancelled'
                               WHEN CURRENT_DATE < starts_at THEN 'scheduled'
                               WHEN CURRENT_DATE > ends_at THEN 'completed'
                               ELSE 'active'
                           END AS state
                    FROM course_occurrence
                    ORDER BY starts_at DESC, id_course_occurrence DESC
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                List<CourseOccurrence> occurrences = new ArrayList<>();
                while (resultSet.next()) {
                    occurrences.add(mapOccurrence(resultSet));
                }
                return List.copyOf(occurrences);
            }
        }
    }

    public List<CourseOccurrence> findByCourse(long courseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByCourse(connection, courseId);
        }
    }

    public List<CourseOccurrence> findByCourse(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT id_course_occurrence, id_course, reference_year, label AS code, starts_at, ends_at,
                       CASE
                           WHEN state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > ends_at THEN 'completed'
                           ELSE 'active'
                       END AS state
                FROM course_occurrence
                WHERE id_course = ?
                ORDER BY starts_at DESC, id_course_occurrence DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseOccurrence> occurrences = new ArrayList<>();
                while (resultSet.next()) {
                    occurrences.add(mapOccurrence(resultSet));
                }
                return List.copyOf(occurrences);
            }
        }
    }

    public List<CourseOccurrencePeriod> findPeriodsByOccurrence(long courseOccurrenceId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findPeriodsByOccurrence(connection, courseOccurrenceId);
        }
    }

    public List<CourseOccurrencePeriod> findPeriodsByOccurrence(Connection connection, long courseOccurrenceId)
            throws SQLException {
        String sql = """
                SELECT id_course_occurrence_period, id_course_occurrence, curricular_year, term,
                       starts_at, ends_at,
                       CASE
                           WHEN state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > ends_at THEN 'completed'
                           ELSE 'active'
                       END AS state
                FROM course_occurrence_period
                WHERE id_course_occurrence = ?
                ORDER BY curricular_year, starts_at, id_course_occurrence_period
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseOccurrencePeriod> periods = new ArrayList<>();
                while (resultSet.next()) {
                    periods.add(mapPeriod(resultSet));
                }
                return List.copyOf(periods);
            }
        }
    }

    public Optional<CourseOccurrenceContext> findContextByPeriodId(
            long courseOccurrencePeriodId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findContextByPeriodId(connection, courseOccurrencePeriodId);
        }
    }

    public Optional<CourseOccurrenceContext> findContextByPeriodId(
            Connection connection,
            long courseOccurrencePeriodId
    ) throws SQLException {
        String sql = """
                SELECT co.id_course_occurrence, co.id_course, co.reference_year, co.label AS code, co.starts_at AS occurrence_starts_at,
                       co.ends_at AS occurrence_ends_at,
                       CASE
                           WHEN co.state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < co.starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > co.ends_at THEN 'completed'
                           ELSE 'active'
                       END AS occurrence_state,
                       cop.id_course_occurrence_period, cop.curricular_year, cop.term,
                       cop.starts_at AS period_starts_at, cop.ends_at AS period_ends_at,
                       CASE
                           WHEN cop.state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < cop.starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > cop.ends_at THEN 'completed'
                           ELSE 'active'
                       END AS period_state
                FROM course_occurrence_period cop
                JOIN course_occurrence co ON co.id_course_occurrence = cop.id_course_occurrence
                WHERE cop.id_course_occurrence_period = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseOccurrencePeriodId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapContext(resultSet));
            }
        }
    }

    public CourseOccurrenceContext resolveClassGroupContext(
            Connection connection,
            long courseId,
            long subjectId,
            Long requestedOccurrenceId,
            Long requestedPeriodId,
            LocalDate startsAt,
            LocalDate endsAt
    ) throws SQLException {
        if (requestedPeriodId != null && requestedPeriodId > 0) {
            CourseOccurrenceContext context = findContextByPeriodId(connection, requestedPeriodId)
                    .orElseThrow(() -> new IllegalArgumentException("Course occurrence period not found"));
            if (requestedOccurrenceId != null
                    && requestedOccurrenceId > 0
                    && context.occurrence().id() != requestedOccurrenceId) {
                throw new IllegalArgumentException("Course occurrence period does not belong to the selected occurrence");
            }
            return context;
        }

        String occurrenceFilter = requestedOccurrenceId != null && requestedOccurrenceId > 0
                ? "AND co.id_course_occurrence = ?"
                : "";
        String sql = """
                SELECT co.id_course_occurrence, co.id_course, co.reference_year, co.label AS code, co.starts_at AS occurrence_starts_at,
                       co.ends_at AS occurrence_ends_at,
                       CASE
                           WHEN co.state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < co.starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > co.ends_at THEN 'completed'
                           ELSE 'active'
                       END AS occurrence_state,
                       cop.id_course_occurrence_period, cop.curricular_year, cop.term,
                       cop.starts_at AS period_starts_at, cop.ends_at AS period_ends_at,
                       CASE
                           WHEN cop.state = 'cancelled' THEN 'cancelled'
                           WHEN CURRENT_DATE < cop.starts_at THEN 'scheduled'
                           WHEN CURRENT_DATE > cop.ends_at THEN 'completed'
                           ELSE 'active'
                       END AS period_state
                FROM integrate_subject isub
                JOIN course_occurrence co ON co.id_course = isub.id_course
                JOIN course_occurrence_period cop
                  ON cop.id_course_occurrence = co.id_course_occurrence
                 AND cop.curricular_year = isub.curricular_year
                 AND cop.term = isub.term
                WHERE isub.id_course = ?
                  AND isub.id_subject = ?
                  %s
                  AND (? IS NULL OR cop.starts_at <= ?)
                  AND (? IS NULL OR cop.ends_at >= ?)
                ORDER BY
                  CASE WHEN CURRENT_DATE BETWEEN co.starts_at AND co.ends_at THEN 0 ELSE 1 END,
                  co.starts_at DESC,
                  cop.starts_at DESC
                LIMIT 1
                """.formatted(occurrenceFilter);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setLong(index++, courseId);
            statement.setLong(index++, subjectId);
            if (requestedOccurrenceId != null && requestedOccurrenceId > 0) {
                statement.setLong(index++, requestedOccurrenceId);
            }
            setDate(statement, index++, startsAt);
            setDate(statement, index++, startsAt);
            setDate(statement, index++, endsAt);
            setDate(statement, index, endsAt);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "No course occurrence period matches the class group course, subject and dates"
                    );
                }
                return mapContext(resultSet);
            }
        }
    }

    private static CourseOccurrenceContext mapContext(ResultSet resultSet) throws SQLException {
        CourseOccurrence occurrence = new CourseOccurrence(
                resultSet.getLong("id_course_occurrence"),
                resultSet.getLong("id_course"),
                resultSet.getInt("reference_year"),
                resultSet.getString("code"),
                resultSet.getDate("occurrence_starts_at").toLocalDate(),
                resultSet.getDate("occurrence_ends_at").toLocalDate(),
                CourseOccurrenceState.fromDatabaseValue(resultSet.getString("occurrence_state"))
        );
        CourseOccurrencePeriod period = new CourseOccurrencePeriod(
                resultSet.getLong("id_course_occurrence_period"),
                occurrence.id(),
                resultSet.getInt("curricular_year"),
                CurricularTerm.fromDatabaseValue(resultSet.getString("term")),
                resultSet.getDate("period_starts_at").toLocalDate(),
                resultSet.getDate("period_ends_at").toLocalDate(),
                CourseOccurrenceState.fromDatabaseValue(resultSet.getString("period_state"))
        );
        return new CourseOccurrenceContext(occurrence, period);
    }

    private static CourseOccurrence mapOccurrence(ResultSet resultSet) throws SQLException {
        return new CourseOccurrence(
                resultSet.getLong("id_course_occurrence"),
                resultSet.getLong("id_course"),
                resultSet.getInt("reference_year"),
                resultSet.getString("code"),
                resultSet.getDate("starts_at").toLocalDate(),
                resultSet.getDate("ends_at").toLocalDate(),
                CourseOccurrenceState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static CourseOccurrencePeriod mapPeriod(ResultSet resultSet) throws SQLException {
        return new CourseOccurrencePeriod(
                resultSet.getLong("id_course_occurrence_period"),
                resultSet.getLong("id_course_occurrence"),
                resultSet.getInt("curricular_year"),
                CurricularTerm.fromDatabaseValue(resultSet.getString("term")),
                resultSet.getDate("starts_at").toLocalDate(),
                resultSet.getDate("ends_at").toLocalDate(),
                CourseOccurrenceState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private static int executeTemporalStateUpdate(
            Connection connection,
            String sql,
            LocalDate today
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));
            statement.setDate(3, Date.valueOf(today));
            statement.setDate(4, Date.valueOf(today));
            return statement.executeUpdate();
        }
    }
}
