package pt.isel.gape.security.crypto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import pt.isel.gape.common.config.ConnectionProvider;

/**
 * Idempotently upgrades legacy sensitive values after schema/bootstrap
 * initialization. SQL migrations deliberately do not contain the encryption
 * key, so encryption is completed by this service only after the runtime key
 * has been validated.
 */
public final class SensitiveDataMigrationService {

    private static final String DOCUMENT_NUMBER_PURPOSE = "user_account.document_number";
    private static final String DELETION_REASON_PURPOSE = "deletion_request.reason";
    private static final String JUSTIFICATION_REASON_PURPOSE = "absence_justification.reason";
    private static final String JUSTIFICATION_DECISION_NOTES_PURPOSE = "absence_justification.decision_notes";

    private final ConnectionProvider connectionProvider;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final SessionTokenHasher sessionTokenHasher;

    public SensitiveDataMigrationService() {
        this(ConnectionProvider.defaultProvider(), SensitiveDataCipher.fromRuntimeConfiguration(), new SessionTokenHasher());
    }

    SensitiveDataMigrationService(
            ConnectionProvider connectionProvider,
            SensitiveDataCipher sensitiveDataCipher,
            SessionTokenHasher sessionTokenHasher
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.sensitiveDataCipher = Objects.requireNonNull(sensitiveDataCipher, "sensitiveDataCipher is required");
        this.sessionTokenHasher = Objects.requireNonNull(sessionTokenHasher, "sessionTokenHasher is required");
    }

    public void migrateIfRequired() {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int protectedValues = 0;
                if (sensitiveDataCipher.isEnabled()) {
                    protectedValues += migrateDocuments(connection);
                    protectedValues += migrateCipherColumn(
                            connection,
                            "deletion_request",
                            "id_deletion",
                            "reason",
                            DELETION_REASON_PURPOSE
                    );
                    protectedValues += migrateCipherColumn(
                            connection,
                            "absence_justification",
                            "id_absence_justification",
                            "reason",
                            JUSTIFICATION_REASON_PURPOSE
                    );
                    protectedValues += migrateCipherColumn(
                            connection,
                            "absence_justification",
                            "id_absence_justification",
                            "decision_notes",
                            JUSTIFICATION_DECISION_NOTES_PURPOSE
                    );
                }
                int protectedTokens = migrateSessionTokens(connection);
                connection.commit();
                if (protectedValues > 0 || protectedTokens > 0) {
                    System.out.println("[GAPE][SECURITY] Protected " + protectedValues
                            + " sensitive values and " + protectedTokens + " legacy session tokens at rest.");
                }
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to migrate sensitive data at rest", exception);
        }
    }

    private int migrateDocuments(Connection connection) throws SQLException {
        int migrated = 0;
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT id_user, document_number, document_number_fingerprint
                FROM user_account
                WHERE document_number IS NOT NULL
                """);
             ResultSet resultSet = select.executeQuery();
             PreparedStatement update = connection.prepareStatement("""
                     UPDATE user_account
                     SET document_number = ?, document_number_fingerprint = ?
                     WHERE id_user = ?
                     """)) {
            while (resultSet.next()) {
                long userId = resultSet.getLong("id_user");
                String storedValue = resultSet.getString("document_number");
                String plaintext = SensitiveDataCipher.isEncrypted(storedValue)
                        ? sensitiveDataCipher.decrypt(storedValue, DOCUMENT_NUMBER_PURPOSE)
                        : storedValue;
                String encryptedValue = SensitiveDataCipher.isEncrypted(storedValue)
                        ? storedValue
                        : sensitiveDataCipher.encrypt(plaintext, DOCUMENT_NUMBER_PURPOSE);
                String fingerprint = sensitiveDataCipher.fingerprint(plaintext, DOCUMENT_NUMBER_PURPOSE);
                String existingFingerprint = resultSet.getString("document_number_fingerprint");
                if (encryptedValue.equals(storedValue) && fingerprint.equals(existingFingerprint)) {
                    continue;
                }
                update.setString(1, encryptedValue);
                update.setString(2, fingerprint);
                update.setLong(3, userId);
                migrated += update.executeUpdate();
            }
        }
        return migrated;
    }

    private int migrateCipherColumn(
            Connection connection,
            String table,
            String idColumn,
            String valueColumn,
            String purpose
    ) throws SQLException {
        String selectSql = "SELECT " + idColumn + ", " + valueColumn
                + " FROM " + table + " WHERE " + valueColumn + " IS NOT NULL";
        String updateSql = "UPDATE " + table + " SET " + valueColumn + " = ? WHERE " + idColumn + " = ?";
        int migrated = 0;
        try (PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet resultSet = select.executeQuery();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (resultSet.next()) {
                String storedValue = resultSet.getString(valueColumn);
                if (SensitiveDataCipher.isEncrypted(storedValue)) {
                    // Authenticate an existing envelope now, rather than
                    // discovering a wrong key or tamper only when somebody
                    // later opens the affected record.
                    sensitiveDataCipher.decrypt(storedValue, purpose);
                    continue;
                }
                update.setString(1, sensitiveDataCipher.encrypt(storedValue, purpose));
                update.setLong(2, resultSet.getLong(idColumn));
                migrated += update.executeUpdate();
            }
        }
        return migrated;
    }

    private int migrateSessionTokens(Connection connection) throws SQLException {
        int migrated = 0;
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT id_session, token
                FROM user_session
                """);
             ResultSet resultSet = select.executeQuery();
             PreparedStatement update = connection.prepareStatement("""
                     UPDATE user_session
                     SET token = ?
                     WHERE id_session = ?
                     """)) {
            while (resultSet.next()) {
                String storedToken = resultSet.getString("token");
                if (sessionTokenHasher.isHashed(storedToken)) {
                    continue;
                }
                update.setString(1, sessionTokenHasher.hash(storedToken));
                update.setLong(2, resultSet.getLong("id_session"));
                migrated += update.executeUpdate();
            }
        }
        return migrated;
    }
}
