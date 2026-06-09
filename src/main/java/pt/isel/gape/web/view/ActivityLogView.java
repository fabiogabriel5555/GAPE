package pt.isel.gape.web.view;

import java.time.format.DateTimeFormatter;

import pt.isel.gape.transversal.model.ActivityLog;

public final class ActivityLogView {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityLog log;

    private ActivityLogView(ActivityLog log) {
        this.log = log;
    }

    public static ActivityLogView from(ActivityLog log) {
        return new ActivityLogView(log);
    }

    public long getId() {
        return log.id();
    }

    public Long getUserId() {
        return log.userId();
    }

    public Long getSessionId() {
        return log.sessionId();
    }

    public String getOperationType() {
        return log.operationType();
    }

    public String getAffectedEntityType() {
        return log.affectedEntityType();
    }

    public String getAffectedEntityIdentifier() {
        return log.affectedEntityIdentifier();
    }

    public String getOccurredAt() {
        return DATE_TIME.format(log.occurredAt());
    }

    public String getOutcome() {
        return log.outcome();
    }

    public String getSourceIp() {
        return log.sourceIp();
    }
}
