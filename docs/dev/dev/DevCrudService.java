package pt.isel.gape.dev;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pt.isel.gape.common.config.DatabaseConfig;

final class DevCrudService {

    private final DevTableMetadataService metadataService = new DevTableMetadataService();
    private final SqlErrorTranslator errorTranslator = new SqlErrorTranslator();

    void viewData(DevConsoleInputReader input) {
        String tableName = "-";
        Map<String, Object> payload = Map.of();
        try (Connection connection = DatabaseConfig.getConnection()) {
            tableName = chooseTable(connection, input);
            if (tableName == null) {
                return;
            }
            DevTableMetadataService.TableMetadata table = metadataService.describeTable(connection, tableName);
            List<Map<String, Object>> rows = metadataService.fetchRows(connection, tableName, 200);

            if (rows.isEmpty()) {
                System.out.println("Sem registos.");
                return;
            }

            printTableSummary(table, rows);
            if (table.primaryKeys().isEmpty()) {
                System.out.println("Tabela sem chave primaria. Detalhe por ID indisponivel.");
                return;
            }

            if (!input.confirm("Ver detalhe de um registo?")) {
                return;
            }

            Map<String, Object> pk = readPrimaryKey(input, table, true);
            if (pk == null) {
                return;
            }
            payload = pk;
            Map<String, Object> row = metadataService.findByPrimaryKey(connection, table, pk);
            if (row == null) {
                System.out.println("Registo nao encontrado.");
                return;
            }

            System.out.println("Detalhe do registo:");
            row.forEach((k, v) -> System.out.println(" - " + k + " = " + Objects.toString(v, "NULL")));
        } catch (SQLException exception) {
            printFailure("SELECT", tableName, payload, exception);
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERRO] " + exception.getMessage());
        }
    }

    void createData(DevConsoleInputReader input) {
        String tableName = "-";
        Map<String, Object> values = Map.of();
        try (Connection connection = DatabaseConfig.getConnection()) {
            tableName = chooseTable(connection, input);
            if (tableName == null) {
                return;
            }
            DevTableMetadataService.TableMetadata table = metadataService.describeTable(connection, tableName);
            values = readInsertValues(input, table);

            int inserted = metadataService.insert(connection, table, values);
            Long newId = metadataService.lastGeneratedId(connection);

            System.out.println("[OK] Insercao realizada com sucesso.");
            System.out.println("Tabela: " + tableName);
            System.out.println("Linhas inseridas: " + inserted);
            if (newId != null && newId > 0) {
                System.out.println("ID criado: " + newId);
            }
        } catch (SQLException exception) {
            printFailure("INSERT", tableName, values, exception);
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERRO] Valor invalido: " + exception.getMessage());
        }
    }

    void updateData(DevConsoleInputReader input) {
        String tableName = "-";
        Map<String, Object> payload = Map.of();
        try (Connection connection = DatabaseConfig.getConnection()) {
            tableName = chooseTable(connection, input);
            if (tableName == null) {
                return;
            }
            DevTableMetadataService.TableMetadata table = metadataService.describeTable(connection, tableName);
            if (table.primaryKeys().isEmpty()) {
                System.out.println("A tabela nao tem chave primaria. Atualizacao por identificador indisponivel.");
                return;
            }

            List<Map<String, Object>> rows = metadataService.fetchRows(connection, tableName, 100);
            printTableSummary(table, rows);

            Map<String, Object> pk = readPrimaryKey(input, table, true);
            if (pk == null) {
                return;
            }
            Map<String, Object> current = metadataService.findByPrimaryKey(connection, table, pk);
            if (current == null) {
                System.out.println("Registo nao encontrado.");
                return;
            }

            System.out.println("Valores atuais:");
            current.forEach((k, v) -> System.out.println(" - " + k + " = " + Objects.toString(v, "NULL")));

            Map<String, Object> changes = readUpdateValues(input, table, current);
            if (changes.isEmpty()) {
                System.out.println("Sem alteracoes para aplicar.");
                return;
            }
            payload = new LinkedHashMap<>(changes);
            payload.putAll(pk);

            int updated = metadataService.update(connection, table, pk, changes);
            if (updated == 0) {
                System.out.println("Nenhuma linha atualizada.");
                return;
            }

            System.out.println("[OK] Atualizacao realizada com sucesso.");
            System.out.println("Tabela: " + tableName);
            System.out.println("Linhas atualizadas: " + updated);
        } catch (SQLException exception) {
            printFailure("UPDATE", tableName, payload, exception);
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERRO] Valor invalido: " + exception.getMessage());
        }
    }

    void deleteData(DevConsoleInputReader input) {
        String tableName = "-";
        Map<String, Object> payload = Map.of();
        try (Connection connection = DatabaseConfig.getConnection()) {
            tableName = chooseTable(connection, input);
            if (tableName == null) {
                return;
            }
            DevTableMetadataService.TableMetadata table = metadataService.describeTable(connection, tableName);
            if (table.primaryKeys().isEmpty()) {
                System.out.println("A tabela nao tem chave primaria. Remocao por identificador indisponivel.");
                return;
            }

            List<Map<String, Object>> rows = metadataService.fetchRows(connection, tableName, 100);
            printTableSummary(table, rows);

            Map<String, Object> pk = readPrimaryKey(input, table, true);
            if (pk == null) {
                return;
            }
            payload = pk;
            Map<String, Object> current = metadataService.findByPrimaryKey(connection, table, pk);
            if (current == null) {
                System.out.println("Registo nao encontrado.");
                return;
            }

            System.out.println("Registo a apagar:");
            current.forEach((k, v) -> System.out.println(" - " + k + " = " + Objects.toString(v, "NULL")));

            if (!input.confirm("Confirmar eliminacao?")) {
                System.out.println("Operacao cancelada.");
                return;
            }

            int deleted = metadataService.delete(connection, table, pk);
            if (deleted == 0) {
                System.out.println("Nenhuma linha removida.");
                return;
            }

            System.out.println("[OK] Eliminacao realizada com sucesso.");
            System.out.println("Tabela: " + tableName);
            System.out.println("Linhas removidas: " + deleted);
        } catch (SQLException exception) {
            printFailure("DELETE", tableName, payload, exception);
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERRO] " + exception.getMessage());
        }
    }

    private String chooseTable(Connection connection, DevConsoleInputReader input) throws SQLException {
        List<String> tables = metadataService.listTables(connection);
        if (tables.isEmpty()) {
            System.out.println("Nao existem tabelas disponiveis.");
            return null;
        }

        System.out.println("Escolha a tabela:");
        for (int i = 0; i < tables.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, tables.get(i));
        }
        System.out.println("0. Voltar");

        int choice = input.readInt("Tabela: ", 0, tables.size());
        if (choice == 0) {
            return null;
        }
        return tables.get(choice - 1);
    }

    private void printTableSummary(DevTableMetadataService.TableMetadata table, List<Map<String, Object>> rows) {
        System.out.println("Tabela escolhida: " + table.tableName());
        if (rows.isEmpty()) {
            System.out.println("Sem registos.");
            return;
        }
        System.out.println("Registos:");
        for (Map<String, Object> row : rows) {
            System.out.println(" - " + compactRow(row, table));
        }
    }

    private String compactRow(Map<String, Object> row, DevTableMetadataService.TableMetadata table) {
        StringBuilder builder = new StringBuilder();
        if (!table.primaryKeys().isEmpty()) {
            for (String pk : table.primaryKeys()) {
                builder.append(pk).append('=').append(Objects.toString(row.get(pk), "NULL")).append(' ');
            }
        }

        int shown = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (table.primaryKeys().contains(entry.getKey())) {
                continue;
            }
            builder.append(entry.getKey()).append('=').append(Objects.toString(entry.getValue(), "NULL")).append(' ');
            shown++;
            if (shown >= 3) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private Map<String, Object> readInsertValues(
            DevConsoleInputReader input,
            DevTableMetadataService.TableMetadata table
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        System.out.println("Campos da tabela:");

        for (DevTableMetadataService.ColumnMetadata column : table.columns()) {
            if (column.autoIncrement()) {
                continue;
            }

            String required = column.requiredOnInsert() ? "obrigatorio" : "opcional";
            String prompt = String.format(
                    "%s [%s, %s] (vazio = manter default/NULL): ",
                    column.name(),
                    column.typeName(),
                    required
            );
            String raw = input.readLine(prompt);
            if (raw.isBlank()) {
                continue;
            }
            values.put(column.name(), parseValue(raw, column));
        }
        return values;
    }

    private Map<String, Object> readUpdateValues(
            DevConsoleInputReader input,
            DevTableMetadataService.TableMetadata table,
            Map<String, Object> current
    ) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (DevTableMetadataService.ColumnMetadata column : table.columns()) {
            if (table.primaryKeys().contains(column.name())) {
                continue;
            }

            String prompt = String.format(
                    "%s [%s] atual='%s' (Enter para manter, :null para NULL): ",
                    column.name(),
                    column.typeName(),
                    Objects.toString(current.get(column.name()), "NULL")
            );

            String raw = input.readLine(prompt);
            if (raw.isBlank()) {
                continue;
            }
            if (":null".equalsIgnoreCase(raw)) {
                values.put(column.name(), null);
            } else {
                values.put(column.name(), parseValue(raw, column));
            }
        }
        return values;
    }

    private Map<String, Object> readPrimaryKey(
            DevConsoleInputReader input,
            DevTableMetadataService.TableMetadata table,
            boolean allowCancel
    ) {
        Map<String, Object> pkValues = new LinkedHashMap<>();
        Map<String, DevTableMetadataService.ColumnMetadata> byName = new LinkedHashMap<>();
        for (DevTableMetadataService.ColumnMetadata column : table.columns()) {
            byName.put(column.name(), column);
        }

        for (String pk : table.primaryKeys()) {
            DevTableMetadataService.ColumnMetadata column = byName.get(pk);
            while (true) {
                String raw = input.readLine(
                        "Escolha um ID para '" + pk + "' [" + column.typeName() + "] ou 0 para voltar: "
                );
                if (allowCancel && "0".equals(raw)) {
                    return null;
                }
                if (raw.isBlank()) {
                    System.out.println("Campo obrigatorio.");
                    continue;
                }
                pkValues.put(pk, parseValue(raw, column));
                break;
            }
        }
        return pkValues;
    }

    private Object parseValue(String raw, DevTableMetadataService.ColumnMetadata column) {
        String value = raw.trim();
        if (value.isBlank()) {
            return null;
        }
        return switch (column.sqlType()) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> Integer.valueOf(value);
            case Types.BIGINT -> Long.valueOf(value);
            case Types.DECIMAL, Types.NUMERIC -> new BigDecimal(value);
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> Double.valueOf(value);
            case Types.BIT, Types.BOOLEAN -> parseBoolean(value);
            case Types.DATE -> Date.valueOf(value);
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> Time.valueOf(value);
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> Timestamp.valueOf(value);
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB ->
                    value.getBytes(StandardCharsets.UTF_8);
            default -> value;
        };
    }

    private boolean parseBoolean(String value) {
        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "t".equalsIgnoreCase(value)) {
            return true;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value) || "f".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Booleano invalido. Usa true/false ou 1/0.");
    }

    private void printFailure(String operation, String table, Map<String, Object> data, SQLException exception) {
        SqlErrorTranslator.SqlErrorTranslation translation = errorTranslator.translate(exception);
        System.out.println("[ERRO] Operacao rejeitada.");
        System.out.println("Tabela: " + table);
        System.out.println("Operacao: " + operation);
        System.out.println("Dados enviados: " + data);
        System.out.println("Motivo provavel: " + translation.simpleReason());
        System.out.println("Restricao provavel: " +
                (translation.probableConstraint() == null ? "Nao identificada" : translation.probableConstraint()));
        System.out.println("Erro tecnico: " + exception.getMessage());
    }
}
