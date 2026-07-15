package pt.isel.gape.learning.model;

public record CertificateIssueCommand(
        long courseId,
        Long courseOccurrenceId,
        long studentUserId
) {
    public CertificateIssueCommand(
            long courseId,
            long studentUserId
    ) {
        this(courseId, null, studentUserId);
    }
}
