package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record ClassGroupCreateCommand(
        long subjectId,
        long courseId,
        String code,
        ClassGroupModality modality,
        ClassGroupState state,
        Integer minStudents,
        Integer maxStudents,
        LocalDate startsAt,
        LocalDate endsAt,
        ClassGroupShift shift,
        boolean showContentThumbnails
) {
}
