package pt.isel.gape.learning.model;

public record SubjectInitialCourseAssignment(
        long courseId,
        Integer curricularYear,
        CurricularTerm term,
        boolean mandatory,
        EnrollmentApprovalMode approvalMode
) {

    public SubjectInitialCourseAssignment(
            long courseId,
            Integer curricularYear,
            CurricularTerm term,
            boolean mandatory
    ) {
        this(courseId, curricularYear, term, mandatory, EnrollmentApprovalMode.MANUAL);
    }
}
