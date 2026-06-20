package pt.isel.gape.learning.model;

public record ContentStorageContext(
        Long classGroupId,
        Long contentBlockId,
        long contentItemId,
        Long uploaderUserId
) {

    public ContentStorageContext {
        if (contentItemId <= 0) {
            throw new IllegalArgumentException("content item id is required");
        }
        requirePositive(classGroupId, "class group id");
        requirePositive(contentBlockId, "content block id");
        requirePositive(uploaderUserId, "uploader user id");
    }

    public static ContentStorageContext forContentBlock(long classGroupId, long contentBlockId, long contentItemId) {
        return new ContentStorageContext(classGroupId, contentBlockId, contentItemId, null);
    }

    public static ContentStorageContext forContentBlock(
            long classGroupId,
            long contentBlockId,
            long contentItemId,
            long uploaderUserId
    ) {
        return new ContentStorageContext(classGroupId, contentBlockId, contentItemId, uploaderUserId);
    }

    public static ContentStorageContext forClassGroup(long classGroupId, long contentItemId) {
        return new ContentStorageContext(classGroupId, null, contentItemId, null);
    }

    public static ContentStorageContext forClassGroup(long classGroupId, long contentItemId, long uploaderUserId) {
        return new ContentStorageContext(classGroupId, null, contentItemId, uploaderUserId);
    }

    public static ContentStorageContext forContentItem(long contentItemId) {
        return new ContentStorageContext(null, null, contentItemId, null);
    }

    public static ContentStorageContext forContentItem(long contentItemId, long uploaderUserId) {
        return new ContentStorageContext(null, null, contentItemId, uploaderUserId);
    }

    public boolean hasContentBlock() {
        return classGroupId != null && contentBlockId != null;
    }

    public boolean hasClassGroup() {
        return classGroupId != null;
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
