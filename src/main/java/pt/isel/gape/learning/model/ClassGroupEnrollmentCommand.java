package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record ClassGroupEnrollmentCommand(
        long studentUserId,
        long classGroupId,
        LocalDate startDate,
        LocalDate endDate
) {
}
