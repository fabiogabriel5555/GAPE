package pt.isel.gape.learning.model;

public record ContentContext(
        ContentAssociationType type,
        long targetId,
        Long organizationId,
        Long organicUnitId,
        Long courseId,
        Long subjectId,
        Long classGroupId,
        Long contentBlockId,
        Long assessmentId,
        String state
) {
    public boolean isActive() {
        return "active".equals(state);
    }
}
