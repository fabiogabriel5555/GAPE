package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.DeletionRequestState;

public final class DeletionRequestView {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DeletionRequest request;

    private DeletionRequestView(DeletionRequest request) {
        this.request = request;
    }

    public static DeletionRequestView from(DeletionRequest request) {
        return new DeletionRequestView(request);
    }

    public long getId() {
        return request.id();
    }

    public long getSubmitterUserId() {
        return request.submitterUserId();
    }

    public Long getProcessorAdminUserId() {
        return request.processorAdminUserId();
    }

    public String getSubmittedAt() {
        return format(request.submittedAt());
    }

    public String getProcessedAt() {
        return request.processedAt() == null ? "-" : format(request.processedAt());
    }

    public String getReason() {
        return request.reason();
    }

    public String getState() {
        return request.state().name();
    }

    public String getStateLabel() {
        return switch (request.state()) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
        };
    }

    public String getStateLabelEnglish() {
        return switch (request.state()) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
        };
    }

    public boolean isFinalState() {
        return request.state().isFinal();
    }

    public String getStateBadgeClass() {
        return switch (request.state()) {
            case SUBMITTED -> "bg-warning-30 text-warning-600";
            case UNDER_REVIEW -> "bg-main-50 text-main-600";
            case APPROVED -> "bg-success-50 text-success-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-neutral-40 text-neutral-600";
        };
    }

    public String getSubmittedAtInputMinimum() {
        return request.submittedAt().toString();
    }

    public static DeletionRequestState stateFrom(String value) {
        return DeletionRequestState.valueOf(value);
    }

    private static String format(LocalDateTime dateTime) {
        return DATE_TIME.format(dateTime);
    }
}
