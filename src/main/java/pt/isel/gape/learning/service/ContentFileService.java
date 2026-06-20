package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ContentFileDAO;
import pt.isel.gape.learning.model.UploadedContentFile;

public final class ContentFileService {

    private final ConnectionProvider connectionProvider;
    private final ContentFileDAO contentFileDAO;
    private final Clock clock;

    public ContentFileService(ConnectionProvider connectionProvider, Clock clock) {
        this(connectionProvider, new ContentFileDAO(), clock);
    }

    ContentFileService(ConnectionProvider connectionProvider, ContentFileDAO contentFileDAO, Clock clock) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.contentFileDAO = Objects.requireNonNull(contentFileDAO, "contentFileDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public void recordReadyFile(long contentItemId, UploadedContentFile uploadedFile) {
        Objects.requireNonNull(uploadedFile, "uploadedFile is required");
        if (contentItemId <= 0) {
            throw new IllegalArgumentException("content item id is required");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            contentFileDAO.create(connection, contentItemId, uploadedFile, LocalDateTime.now(clock));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to record content file metadata", exception);
        }
    }
}
