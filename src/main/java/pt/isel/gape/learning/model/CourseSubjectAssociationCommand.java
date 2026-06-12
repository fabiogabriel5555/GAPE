package pt.isel.gape.learning.model;

public record CourseSubjectAssociationCommand(
        long courseId,
        long subjectId,
        Integer curricularYear,
        CurricularTerm term,
        boolean mandatory,
        CourseSubjectState state
) {
}
