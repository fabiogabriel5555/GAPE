package pt.isel.gape.transversal.model;

public record DirectMessageContent(
        String body,
        String attachmentPath,
        String attachmentName
) {

    public static DirectMessageContent text(String body) {
        return new DirectMessageContent(body, null, null);
    }

    public static DirectMessageContent attachment(String body, String attachmentPath, String attachmentName) {
        return new DirectMessageContent(body, attachmentPath, attachmentName);
    }
}
