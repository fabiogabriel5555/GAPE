package pt.isel.gape.learning.model;

public record BlockContentPlacement(
        String itemType,
        long sourceContentBlockId,
        long targetContentBlockId,
        long itemId,
        int orderNo
) {
    public boolean isContent() {
        return "content".equals(itemType);
    }

    public boolean isLesson() {
        return "lesson".equals(itemType);
    }

    public boolean isAssessment() {
        return "assessment".equals(itemType);
    }
}
