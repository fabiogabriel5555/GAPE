package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record SubjectCreateCommand(
        long organizationId,
        String name,
        String acronym,
        String photo,
        String description,
        BigDecimal ects,
        Integer workloadHours,
        SubjectState state,
        long initialCourseId,
        Integer initialCurricularYear,
        CurricularTerm initialTerm,
        boolean initialMandatory,
        Set<Long> coordinatorUserIds,
        List<SubjectInitialCourseAssignment> initialCourseAssignments
) {

    public SubjectCreateCommand(
            long organizationId,
            String name,
            String acronym,
            String photo,
            String description,
            BigDecimal ects,
            Integer workloadHours,
            SubjectState state,
            long initialCourseId,
            Integer initialCurricularYear,
            CurricularTerm initialTerm,
            boolean initialMandatory,
            Set<Long> coordinatorUserIds
    ) {
        this(
                organizationId,
                name,
                acronym,
                photo,
                description,
                ects,
                workloadHours,
                state,
                initialCourseId,
                initialCurricularYear,
                initialTerm,
                initialMandatory,
                coordinatorUserIds,
                List.of(new SubjectInitialCourseAssignment(
                        initialCourseId,
                        initialCurricularYear,
                        initialTerm,
                        initialMandatory
                ))
        );
    }
}
