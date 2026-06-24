package pt.isel.gape.structure.model;

import java.time.LocalDate;

public record CoordinateSubjectAssignment(
        long coordinatorUserId,
        long subjectId,
        RoleAssignmentState state,
        LocalDate startDate,
        LocalDate endDate,
        String coordinatorName,
        String coordinatorEmail
) {
}
