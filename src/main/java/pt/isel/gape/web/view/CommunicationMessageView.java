package pt.isel.gape.web.view;

import java.time.LocalDateTime;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageSummary;
import pt.isel.gape.transversal.model.MessageType;

public final class CommunicationMessageView {

    private final MessageSummary message;
    private final long currentUserId;

    public CommunicationMessageView(MessageSummary message, long currentUserId) {
        this.message = message;
        this.currentUserId = currentUserId;
    }

    public long getId() {
        return message.id();
    }

    public long getChannelId() {
        return message.channelId();
    }

    public Long getSenderUserId() {
        return message.senderUserId();
    }

    public String getSenderName() {
        if (message.senderUserId() == null) {
            return "System";
        }
        return isBlank(message.senderName()) ? "Unknown sender" : message.senderName();
    }

    public String getSenderEmail() {
        return message.senderEmail();
    }

    public Long getParentMessageId() {
        return message.parentMessageId();
    }

    public boolean isReply() {
        return message.parentMessageId() != null;
    }

    public String getTitle() {
        if (!isBlank(message.title())) {
            return message.title();
        }
        return getTypeLabel();
    }

    public String getBody() {
        return message.body();
    }

    public String getBodyPreview() {
        String body = isBlank(message.body()) ? getTitle() : message.body().trim();
        return body.length() <= 96 ? body : body.substring(0, 93) + "...";
    }

    public String getTypeValue() {
        return message.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (message.type()) {
            case TEXT -> "Message";
            case COMMENT -> "Comment";
            case ANNOUNCEMENT -> "Announcement";
            case WARNING -> "Warning";
            case ALERT -> "Alert";
            case REMINDER -> "Reminder";
            case NOTIFICATION -> "Notification";
            case SYSTEM -> "System";
            case ATTACHMENT -> "Attachment";
            case OTHER -> "Other";
        };
    }

    public String getTypeIconClass() {
        return switch (message.type()) {
            case COMMENT -> "ph ph-chat-centered-text";
            case ANNOUNCEMENT -> "ph ph-megaphone";
            case WARNING -> "ph ph-warning-circle";
            case ALERT -> "ph ph-bell-ringing";
            case REMINDER -> "ph ph-clock-countdown";
            case NOTIFICATION -> "ph ph-bell";
            case SYSTEM -> "ph ph-cpu";
            case ATTACHMENT -> "ph ph-paperclip";
            case OTHER -> "ph ph-dots-three-circle";
            case TEXT -> "ph ph-chat-dots";
        };
    }

    public String getPriorityLabel() {
        MessagePriority priority = message.priority();
        if (priority == null) {
            return "Normal";
        }
        return switch (priority) {
            case LOW -> "Low";
            case NORMAL -> "Normal";
            case HIGH -> "High";
            case URGENT -> "Urgent";
        };
    }

    public String getPriorityValue() {
        return message.priority() == null ? "normal" : message.priority().toDatabaseValue();
    }

    public String getPriorityBadgeClass() {
        MessagePriority priority = message.priority();
        if (priority == MessagePriority.URGENT) {
            return "bg-danger-50 text-danger-600";
        }
        if (priority == MessagePriority.HIGH) {
            return "bg-warning-50 text-warning-600";
        }
        if (priority == MessagePriority.LOW) {
            return "bg-neutral-30 text-neutral-500";
        }
        return "bg-main-50 text-main-600";
    }

    public String getAttachment() {
        return message.attachment();
    }

    public boolean isHasAttachment() {
        return !isBlank(message.attachment());
    }

    public String getAttachmentFileName() {
        if (!isBlank(message.title()) && message.type() == MessageType.ATTACHMENT) {
            return message.title();
        }
        if (isBlank(message.attachment())) {
            return "Attachment";
        }
        String normalized = message.attachment().replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    public boolean isImageAttachment() {
        return hasAttachmentExtension(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp");
    }

    public boolean isVideoAttachment() {
        return hasAttachmentExtension(".mp4", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".mpeg", ".mpg", ".3gp", ".3gpp");
    }

    public String getCreatedLabel() {
        return format(message.createdAt());
    }

    public String getTimestampLabel() {
        if (message.sentAt() != null) {
            return format(message.sentAt());
        }
        if (message.scheduledAt() != null) {
            return format(message.scheduledAt());
        }
        return format(message.createdAt());
    }

    public String getScheduledLabel() {
        return format(message.scheduledAt());
    }

    public String getDeliveredLabel() {
        return format(message.deliveredAt());
    }

    public String getReadLabel() {
        return format(message.readAt());
    }

    public String getStateLabel() {
        if (isFromCurrentUser()) {
            if (message.state() == MessageState.SCHEDULED) {
                return "Scheduled";
            }
            if (message.readAt() != null) {
                return "Read";
            }
            if (message.deliveredAt() != null) {
                return "Delivered";
            }
            return "Sent";
        }
        if (message.state() == MessageState.SENT && isUnread()) {
            return "Unread";
        }
        if (message.state() == MessageState.SENT && message.readAt() != null) {
            return "Read";
        }
        return switch (message.state()) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case SENT -> message.readAt() == null ? "Delivered" : "Read";
            case ACTIVE -> "Active";
            case EDITED -> "Edited";
            case DELETED -> "Deleted";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getStateBadgeClass() {
        if (message.state() == MessageState.SCHEDULED) {
            return "bg-warning-50 text-warning-600";
        }
        if (message.state() == MessageState.DELETED || message.state() == MessageState.CANCELLED) {
            return "bg-danger-50 text-danger-600";
        }
        if (message.readAt() != null) {
            return "bg-success-50 text-success-600";
        }
        if (message.deliveredAt() != null) {
            return "bg-info-50 text-info-600";
        }
        return "bg-neutral-30 text-neutral-500";
    }

    public boolean isFromCurrentUser() {
        return message.senderUserId() != null && message.senderUserId() == currentUserId;
    }

    public boolean isSystemGenerated() {
        return message.senderUserId() == null || message.type().isSystemGeneratedType();
    }

    public boolean isScheduled() {
        return message.state() == MessageState.SCHEDULED;
    }

    public boolean isUnread() {
        return message.deliveredAt() != null && message.readAt() == null && !isFromCurrentUser();
    }

    public boolean isReadable() {
        return !isFromCurrentUser() && message.deliveredAt() != null && message.readAt() == null;
    }

    public String getChannelTitle() {
        return message.channelTitle();
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : ApplicationDateTimeFormat.dateTime(dateTime);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasAttachmentExtension(String... extensions) {
        if (isBlank(message.attachment()) && isBlank(message.title())) {
            return false;
        }
        String attachment = (isBlank(message.attachment()) ? message.title() : message.attachment()).toLowerCase();
        for (String extension : extensions) {
            if (attachment.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
