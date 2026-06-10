package pt.isel.gape.structure.model;

import java.time.LocalDate;

import pt.isel.gape.access.model.UserState;

public record OrganizationAdministratorAssignment(
        long userId,
        String name,
        String email,
        UserState userState,
        RoleAssignmentState assignmentState,
        LocalDate startDate,
        LocalDate endDate
) {
}
