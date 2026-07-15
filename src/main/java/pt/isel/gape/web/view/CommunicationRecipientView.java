package pt.isel.gape.web.view;

import java.util.Locale;

import pt.isel.gape.transversal.model.MessageRecipientSummary;

public final class CommunicationRecipientView {

    private final MessageRecipientSummary recipient;

    public CommunicationRecipientView(MessageRecipientSummary recipient) {
        this.recipient = recipient;
    }

    public long getUserId() {
        return recipient.userId();
    }

    public String getName() {
        return isBlank(recipient.name()) ? "Unknown user" : recipient.name();
    }

    public String getEmail() {
        return recipient.email();
    }

    public String getLabel() {
        if (isBlank(recipient.email())) {
            return getName();
        }
        return getName() + " - " + recipient.email();
    }

    public String getInitials() {
        String name = getName().trim();
        if (name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        if (parts.length == 1) {
            return first.toUpperCase(Locale.ROOT);
        }
        String second = parts[parts.length - 1].substring(0, 1);
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
