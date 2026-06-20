package pt.isel.gape.structure.model;

import java.time.LocalDate;

public record TeacherClassGroupAssignment(
        long teacherUserId,
        long classGroupId,
        RoleAssignmentState state,
        LocalDate startDate,
        LocalDate endDate,
        String teacherName,
        String teacherEmail
) {
}
