package pt.isel.gape.learning.model;

public record UploadedContentFile(
        String relativePath,
        String originalFileName,
        String contentType,
        long size,
        String originalRelativePath,
        long originalSize,
        long finalSize,
        String sha256,
        String thumbnailRelativePath,
        String originalContentType
) {

    public UploadedContentFile(
            String relativePath,
            String originalFileName,
            String contentType,
            long size
    ) {
        this(relativePath, originalFileName, contentType, size, null, size, size, null, null, contentType);
    }
}
