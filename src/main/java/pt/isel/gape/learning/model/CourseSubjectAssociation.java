package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseSubjectAssociation(
        long courseId,
        long subjectId,
        Integer curricularYear,
        CurricularTerm term,
        boolean mandatory,
        CourseSubjectAssociationState state,
        LocalDate endedAt
) {

    public boolean isActive() {
        return state == CourseSubjectAssociationState.ACTIVE;
    }

    public boolean isHistorical() {
        return state == CourseSubjectAssociationState.HISTORICAL;
    }
}
