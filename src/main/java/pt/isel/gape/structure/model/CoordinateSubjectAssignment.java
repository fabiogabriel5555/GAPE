package pt.isel.gape.structure.model;

public record CoordinateSubjectAssignment(
        long coordinatorUserId,
        long subjectId,
        RoleAssignmentState state,
        String coordinatorName,
        String coordinatorEmail
) {
}
