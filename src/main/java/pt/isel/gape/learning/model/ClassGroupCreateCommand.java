package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record ClassGroupCreateCommand(
        long subjectId,
        long courseId,
        Long courseOccurrenceId,
        Long courseOccurrencePeriodId,
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
    public ClassGroupCreateCommand(
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
        this(
                subjectId,
                courseId,
                null,
                null,
                code,
                modality,
                state,
                minStudents,
                maxStudents,
                startsAt,
                endsAt,
                shift,
                showContentThumbnails
        );
    }
}
