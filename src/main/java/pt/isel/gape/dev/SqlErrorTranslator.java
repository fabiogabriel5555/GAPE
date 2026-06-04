package pt.isel.gape.dev;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlErrorTranslator {

    private static final Pattern KEY_PATTERN = Pattern.compile("for key '([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSTRAINT_BACKTICK_PATTERN =
            Pattern.compile("constraint `([^`]+)`", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSTRAINT_QUOTE_PATTERN =
            Pattern.compile("constraint '([^']+)'", Pattern.CASE_INSENSITIVE);

    SqlErrorTranslation translate(SQLException exception) {
        String raw = exception.getMessage() == null ? "" : exception.getMessage();
        String constraint = extractConstraint(raw);
        int errorCode = exception.getErrorCode();
        String sqlState = exception.getSQLState();

        String reason = switch (errorCode) {
            case 1062 -> "Ja existe um registo com esse valor unico.";
            case 1452 -> "O registo referencia outro que nao existe.";
            case 1451 -> "O registo nao pode ser removido porque tem dependencias.";
            case 1048 -> "Foi enviado NULL para um campo obrigatorio.";
            case 3819 -> "Um valor nao respeita uma regra da tabela.";
            case 1366 -> "Foi enviado um valor com tipo invalido.";
            case 1292 -> "Foi enviado um valor de data/hora invalido.";
            default -> {
                if (sqlState != null && sqlState.startsWith("23")) {
                    yield "Violacao de integridade na base de dados.";
                }
                yield "Erro SQL durante a operacao.";
            }
        };

        if (constraint != null) {
            String c = constraint.toLowerCase(Locale.ROOT);
            if (c.contains("uq_user_account_email")) {
                reason = "Ja existe um utilizador com esse email.";
            } else if (c.contains("uq_user_account_document")) {
                reason = "Ja existe um utilizador com esse documento.";
            } else if (c.contains("fk_")) {
                reason = "A referencia para outra tabela nao existe ou nao e valida.";
            } else if (c.contains("ck_class_group_students_range")) {
                reason = "O numero minimo de alunos nao pode ser maior que o numero maximo.";
            } else if (c.contains("ck_class_group_dates")) {
                reason = "A data final nao pode ser anterior a data inicial.";
            } else if (c.contains("ck_user_session_end")) {
                reason = "A data final da sessao nao pode ser anterior a inicial.";
            } else if (c.contains("ck_deletion_processed_after_submitted")) {
                reason = "processed_at nao pode ser anterior a submitted_at.";
            } else if (c.contains("ck_content_block_availability")) {
                reason = "available_until nao pode ser anterior a available_from.";
            } else if (c.contains("ck_content_item_state") || c.contains("ck_content_item_format")) {
                reason = "O valor de state/format nao pertence aos valores permitidos.";
            }
        }

        if (constraint == null || constraint.isBlank()) {
            if (errorCode == 1062) {
                constraint = "UNIQUE";
            } else if (errorCode == 1452 || errorCode == 1451) {
                constraint = "FOREIGN KEY";
            } else if (errorCode == 3819) {
                constraint = "CHECK";
            } else if (errorCode == 1048) {
                constraint = "NOT NULL";
            }
        }

        return new SqlErrorTranslation(reason, constraint);
    }

    private String extractConstraint(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher keyMatcher = KEY_PATTERN.matcher(message);
        if (keyMatcher.find()) {
            return keyMatcher.group(1);
        }

        Matcher backtickMatcher = CONSTRAINT_BACKTICK_PATTERN.matcher(message);
        if (backtickMatcher.find()) {
            return backtickMatcher.group(1);
        }

        Matcher quoteMatcher = CONSTRAINT_QUOTE_PATTERN.matcher(message);
        if (quoteMatcher.find()) {
            return quoteMatcher.group(1);
        }

        return null;
    }

    record SqlErrorTranslation(String simpleReason, String probableConstraint) {
    }
}
