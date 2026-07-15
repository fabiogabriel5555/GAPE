package pt.isel.gape.web.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
public final class LearningNotificationView {

    private final long eventId;
    private final String categoryValue;
    private final String categoryLabel;
    private final String title;
    private final String description;
    private final String contextLabel;
    private final String contextTitle;
    private final String dateLabel;
    private final LocalDateTime occurredAt;
    private final Long classGroupId;
    private final String href;
    private final String iconClass;
    private final String badgeClass;
    private final String stateLabel;
    private final String stateBadgeClass;
    private final boolean read;

    public LearningNotificationView(
            String categoryValue,
            String categoryLabel,
            String title,
            String description,
            String contextLabel,
            String dateLabel,
            LocalDateTime occurredAt,
            Long classGroupId,
            String href,
            String iconClass,
            String badgeClass,
            String stateLabel,
            String stateBadgeClass
    ) {
        this(
                0L,
                categoryValue,
                categoryLabel,
                title,
                description,
                contextLabel,
                contextLabel,
                dateLabel,
                occurredAt,
                classGroupId,
                href,
                iconClass,
                badgeClass,
                stateLabel,
                stateBadgeClass,
                false
        );
    }

    public LearningNotificationView(
            long eventId,
            String categoryValue,
            String categoryLabel,
            String title,
            String description,
            String contextLabel,
            String dateLabel,
            LocalDateTime occurredAt,
            Long classGroupId,
            String href,
            String iconClass,
            String badgeClass,
            String stateLabel,
            String stateBadgeClass,
            boolean read
    ) {
        this(
                eventId,
                categoryValue,
                categoryLabel,
                title,
                description,
                contextLabel,
                contextLabel,
                dateLabel,
                occurredAt,
                classGroupId,
                href,
                iconClass,
                badgeClass,
                stateLabel,
                stateBadgeClass,
                read
        );
    }

    public LearningNotificationView(
            long eventId,
            String categoryValue,
            String categoryLabel,
            String title,
            String description,
            String contextLabel,
            String contextTitle,
            String dateLabel,
            LocalDateTime occurredAt,
            Long classGroupId,
            String href,
            String iconClass,
            String badgeClass,
            String stateLabel,
            String stateBadgeClass,
            boolean read
    ) {
        this.eventId = eventId;
        this.categoryValue = categoryValue;
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.description = description;
        this.contextLabel = contextLabel;
        this.contextTitle = contextTitle;
        this.dateLabel = dateLabel;
        this.occurredAt = occurredAt;
        this.classGroupId = classGroupId;
        this.href = href;
        this.iconClass = iconClass;
        this.badgeClass = badgeClass;
        this.stateLabel = stateLabel;
        this.stateBadgeClass = stateBadgeClass;
        this.read = read;
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : ApplicationDateTimeFormat.dateTime(value);
    }

    public static String date(LocalDate value) {
        return value == null ? "" : ApplicationDateTimeFormat.date(value);
    }

    public String getCategoryValue() {
        return categoryValue;
    }

    public long getEventId() {
        return eventId;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description == null || description.isBlank() ? "" : description;
    }

    public String getContextLabel() {
        return contextLabel == null || contextLabel.isBlank() ? "-" : contextLabel;
    }

    public String getContextTitle() {
        return contextTitle == null || contextTitle.isBlank() ? getContextLabel() : contextTitle;
    }

    public String getContextHtml() {
        String[] labels = splitContext(getContextLabel());
        String[] titles = splitContext(getContextTitle());
        if (labels.length == 0) {
            return "-";
        }
        StringBuilder html = new StringBuilder();
        for (int index = 0; index < labels.length; index++) {
            if (index > 0) {
                html.append(" | ");
            }
            String label = labels[index];
            String title = index < titles.length ? titles[index] : label;
            html.append(contextPartHtml(label, title));
        }
        return html.toString();
    }

    public String getDateLabel() {
        return dateLabel == null || dateLabel.isBlank() ? "-" : dateLabel;
    }

    public String getDateValue() {
        return occurredAt == null ? "" : occurredAt.toLocalDate().toString();
    }

    public String getTimeLabel() {
        return occurredAt == null ? "" : ApplicationDateTimeFormat.time(occurredAt.toLocalTime());
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Long getClassGroupId() {
        return classGroupId;
    }

    public String getHref() {
        return href;
    }

    public String getActionHref() {
        return eventId > 0 ? "/learning/events/" + eventId + "/detail" : href;
    }

    public String getIconClass() {
        return iconClass;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getStateLabel() {
        return stateLabel;
    }

    public String getStateBadgeClass() {
        return stateBadgeClass;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isUnread() {
        return !read;
    }

    public String getReadStateLabel() {
        return read ? "Read" : "Unread";
    }

    public String getReadStateBadgeClass() {
        return read ? "bg-neutral-50 text-neutral-600" : "bg-danger-50 text-danger-600";
    }

    private static String[] splitContext(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return new String[0];
        }
        return value.trim().split("\\s*\\|\\s*");
    }

    private static String contextPartHtml(String label, String title) {
        String compact = label == null || label.isBlank() ? "-" : label;
        String full = title == null || title.isBlank() ? compact : title;
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(full)
                + "\">"
                + escapeHtml(compact)
                + "</span>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
