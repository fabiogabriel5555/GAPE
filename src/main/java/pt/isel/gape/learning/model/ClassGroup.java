package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record ClassGroup(
        long id,
        long subjectId,
        long courseId,
        long courseOccurrenceId,
        long courseOccurrencePeriodId,
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
    public ClassGroup(
            long id,
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
                id,
                subjectId,
                courseId,
                0,
                0,
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
