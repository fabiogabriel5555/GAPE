package pt.isel.gape.learning.model;

public record CourseSubjectAssociation(
        long courseId,
        long subjectId,
        Integer curricularYear,
        CurricularTerm term,
        boolean mandatory,
        CourseSubjectState state
) {
}
