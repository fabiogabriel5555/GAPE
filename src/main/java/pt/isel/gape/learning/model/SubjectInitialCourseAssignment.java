package pt.isel.gape.learning.model;

public record SubjectInitialCourseAssignment(
        long courseId,
        Integer curricularYear,
        CurricularTerm term,
        boolean mandatory
) {
}
