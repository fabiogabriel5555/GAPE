package pt.isel.gape.learning.model;

public record CertificateIssueCommand(
        long courseId,
        long studentUserId
) {
}
