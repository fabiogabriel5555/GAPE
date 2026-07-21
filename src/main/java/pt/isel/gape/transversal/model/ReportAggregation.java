package pt.isel.gape.transversal.model;

/**
 * Stable, presentation-neutral indicator snapshot for a management view.
 */
public record ReportAggregation(
        ManagementViewScope visibilityScope,
        Long scopeContextId,
        Long ownerUserId,
        int courseCount,
        int subjectCount,
        int classGroupCount,
        int activeEnrollmentCount,
        int lessonCount,
        int assessmentCount,
        int submittedAttemptCount,
        int correctedAttemptCount,
        int pendingCorrectionCount,
        int attendanceRecordCount,
        int issuedCertificateCount
) {

    public ManagementViewScope scope() {
        return visibilityScope;
    }

    public int attemptCount() {
        return submittedAttemptCount + correctedAttemptCount;
    }
}
