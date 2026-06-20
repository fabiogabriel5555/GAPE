package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import pt.isel.gape.learning.model.UploadedContentFile;

public final class ContentFileDAO {

    public void create(
            Connection connection,
            long contentItemId,
            UploadedContentFile uploadedFile,
            LocalDateTime createdAt
    ) throws SQLException {
        String sql = """
                INSERT INTO content_file (
                    id_content_item, original_filename, original_mime_type, final_mime_type,
                    original_bytes, final_bytes, sha256, original_path, final_path,
                    thumbnail_path, processing_state, processing_error, created_at, processed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ready', NULL, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            statement.setString(2, uploadedFile.originalFileName());
            statement.setString(3, uploadedFile.originalContentType());
            statement.setString(4, uploadedFile.contentType());
            statement.setLong(5, uploadedFile.originalSize());
            statement.setLong(6, uploadedFile.finalSize());
            setNullableString(statement, 7, uploadedFile.sha256());
            setNullableString(statement, 8, uploadedFile.originalRelativePath());
            statement.setString(9, uploadedFile.relativePath());
            setNullableString(statement, 10, uploadedFile.thumbnailRelativePath());
            Timestamp timestamp = Timestamp.valueOf(createdAt);
            statement.setTimestamp(11, timestamp);
            statement.setTimestamp(12, timestamp);
            statement.executeUpdate();
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
