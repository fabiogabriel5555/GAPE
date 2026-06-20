package pt.isel.gape.dev;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

final class DevTableMetadataService {

    List<String> listTables(Connection connection) throws SQLException {
        String schema = currentSchema(connection);
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> tables = new ArrayList<>();

        try (ResultSet resultSet = metaData.getTables(schema, null, "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }
        }

        tables.sort(Comparator.naturalOrder());
        return tables;
    }

    TableMetadata describeTable(Connection connection, String tableName) throws SQLException {
        String schema = currentSchema(connection);
        DatabaseMetaData metaData = connection.getMetaData();
        List<ColumnMetadata> columns = new ArrayList<>();

        try (ResultSet resultSet = metaData.getColumns(schema, null, tableName, "%")) {
            while (resultSet.next()) {
                columns.add(new ColumnMetadata(
                        resultSet.getString("COLUMN_NAME"),
                        resultSet.getInt("DATA_TYPE"),
                        resultSet.getString("TYPE_NAME"),
                        Objects.equals("YES", resultSet.getString("IS_NULLABLE")),
                        Objects.equals("YES", resultSet.getString("IS_AUTOINCREMENT")),
                        resultSet.getString("COLUMN_DEF"),
                        resultSet.getInt("ORDINAL_POSITION")
                ));
            }
        }

        if (columns.isEmpty()) {
            throw new SQLException("Tabela nao encontrada: " + tableName);
        }

        columns.sort(Comparator.comparingInt(ColumnMetadata::ordinalPosition));

        List<String> primaryKeys = new ArrayList<>();
        Map<Short, String> pkBySeq = new TreeMap<>();
        try (ResultSet resultSet = metaData.getPrimaryKeys(schema, null, tableName)) {
            while (resultSet.next()) {
                pkBySeq.put(resultSet.getShort("KEY_SEQ"), resultSet.getString("COLUMN_NAME"));
            }
        }
        primaryKeys.addAll(pkBySeq.values());

        return new TableMetadata(tableName, columns, primaryKeys);
    }

    List<Map<String, Object>> fetchRows(Connection connection, String tableName, int limit) throws SQLException {
        String sql = "SELECT * FROM " + q(tableName) + " LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return toRows(resultSet);
            }
        }
    }

    Map<String, Object> findByPrimaryKey(
            Connection connection,
            TableMetadata tableMetadata,
            Map<String, Object> primaryKeyValues
    ) throws SQLException {
        String where = buildWhere(tableMetadata.primaryKeys());
        String sql = "SELECT * FROM " + q(tableMetadata.tableName()) + " WHERE " + where + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillPrimaryKeyParams(statement, tableMetadata.primaryKeys(), primaryKeyValues, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = toRows(resultSet);
                return rows.isEmpty() ? null : rows.getFirst();
            }
        }
    }

    int insert(Connection connection, TableMetadata tableMetadata, Map<String, Object> values) throws SQLException {
        if (values.isEmpty()) {
            throw new SQLException("Sem valores para inserir.");
        }

        List<String> cols = new ArrayList<>(values.keySet());
        String sql = "INSERT INTO " + q(tableMetadata.tableName()) +
                " (" + String.join(", ", cols.stream().map(this::q).toList()) + ") VALUES (" +
                String.join(", ", cols.stream().map(c -> "?").toList()) + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < cols.size(); i++) {
                statement.setObject(i + 1, values.get(cols.get(i)));
            }
            return statement.executeUpdate();
        }
    }

    Long lastGeneratedId(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    int update(
            Connection connection,
            TableMetadata tableMetadata,
            Map<String, Object> primaryKeyValues,
            Map<String, Object> values
    ) throws SQLException {
        if (values.isEmpty()) {
            return 0;
        }

        List<String> cols = new ArrayList<>(values.keySet());
        String setClause = String.join(", ", cols.stream().map(c -> q(c) + " = ?").toList());
        String sql = "UPDATE " + q(tableMetadata.tableName()) + " SET " + setClause +
                " WHERE " + buildWhere(tableMetadata.primaryKeys());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (String col : cols) {
                statement.setObject(index++, values.get(col));
            }
            fillPrimaryKeyParams(statement, tableMetadata.primaryKeys(), primaryKeyValues, index);
            return statement.executeUpdate();
        }
    }

    int delete(Connection connection, TableMetadata tableMetadata, Map<String, Object> primaryKeyValues) throws SQLException {
        String sql = "DELETE FROM " + q(tableMetadata.tableName()) + " WHERE " + buildWhere(tableMetadata.primaryKeys());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillPrimaryKeyParams(statement, tableMetadata.primaryKeys(), primaryKeyValues, 1);
            return statement.executeUpdate();
        }
    }

    private void fillPrimaryKeyParams(
            PreparedStatement statement,
            List<String> primaryKeys,
            Map<String, Object> primaryKeyValues,
            int startIndex
    ) throws SQLException {
        int index = startIndex;
        for (String pk : primaryKeys) {
            statement.setObject(index++, primaryKeyValues.get(pk));
        }
    }

    private List<Map<String, Object>> toRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columns = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columns; i++) {
                row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private String buildWhere(List<String> primaryKeys) throws SQLException {
        if (primaryKeys.isEmpty()) {
            throw new SQLException("A tabela nao tem chave primaria.");
        }
        return String.join(" AND ", primaryKeys.stream().map(pk -> q(pk) + " = ?").toList());
    }

    private String q(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String currentSchema(Connection connection) throws SQLException {
        String schema = connection.getCatalog();
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new SQLException("Nao foi possivel determinar o schema atual.");
    }

    record ColumnMetadata(
            String name,
            int sqlType,
            String typeName,
            boolean nullable,
            boolean autoIncrement,
            String defaultValue,
            int ordinalPosition
    ) {
        boolean requiredOnInsert() {
            return !nullable && !autoIncrement && defaultValue == null;
        }
    }

    record TableMetadata(String tableName, List<ColumnMetadata> columns, List<String> primaryKeys) {
    }
}
