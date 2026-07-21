package pt.isel.gape.web.view;

import java.util.Objects;

import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ReportAggregation;

/**
 * JSP-safe projection of a management panel and, when it is open, its
 * authorised indicator snapshot.  The JSP never receives a DAO or a service.
 */
public final class ManagementDashboardView {

    private final ManagementView managementView;
    private final ReportAggregation aggregation;
    private final boolean configurable;

    private ManagementDashboardView(
            ManagementView managementView,
            ReportAggregation aggregation,
            boolean configurable
    ) {
        this.managementView = Objects.requireNonNull(managementView, "managementView is required");
        this.aggregation = aggregation;
        this.configurable = configurable;
    }

    public static ManagementDashboardView from(
            ManagementView managementView,
            ReportAggregation aggregation,
            boolean configurable
    ) {
        return new ManagementDashboardView(managementView, aggregation, configurable);
    }

    public long getId() {
        return managementView.id();
    }

    public String getTitle() {
        return managementView.title();
    }

    public String getDescription() {
        return managementView.description() == null || managementView.description().isBlank()
                ? "No description was provided for this panel."
                : managementView.description();
    }

    /** Raw editable value; unlike {@link #getDescription()} it never inserts display copy. */
    public String getConfigurationDescription() {
        return managementView.description() == null ? "" : managementView.description();
    }

    public String getTypeValue() {
        return managementView.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (managementView.type()) {
            case DASHBOARD -> "Dashboard";
            case REPORT -> "Report";
            case CONTROL_PANEL -> "Control panel";
            case OTHER -> "Other";
        };
    }

    public String getTypeBadgeClass() {
        return switch (managementView.type()) {
            case DASHBOARD -> "bg-main-50 text-main-600";
            case REPORT -> "bg-success-50 text-success-600";
            case CONTROL_PANEL -> "bg-warning-30 text-warning-700";
            case OTHER -> "bg-neutral-20 text-neutral-600";
        };
    }

    public String getScopeValue() {
        return managementView.visibilityScope().toDatabaseValue();
    }

    public String getScopeLabel() {
        return scopeLabel(managementView.visibilityScope());
    }

    public Long getScopeTargetId() {
        return managementView.scopeContextId();
    }

    public String getStateValue() {
        return managementView.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (managementView.state()) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (managementView.state()) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-700";
            case ARCHIVED -> "bg-neutral-20 text-neutral-600";
        };
    }

    public boolean isDashboard() {
        return managementView.type() == ManagementViewType.DASHBOARD;
    }

    public boolean isReport() {
        return managementView.type() == ManagementViewType.REPORT;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public boolean isOpen() {
        return aggregation != null;
    }

    public int getCourseCount() {
        return indicator(ReportAggregation::courseCount);
    }

    public int getSubjectCount() {
        return indicator(ReportAggregation::subjectCount);
    }

    public int getClassGroupCount() {
        return indicator(ReportAggregation::classGroupCount);
    }

    public int getActiveEnrollmentCount() {
        return indicator(ReportAggregation::activeEnrollmentCount);
    }

    public int getLessonCount() {
        return indicator(ReportAggregation::lessonCount);
    }

    public int getAssessmentCount() {
        return indicator(ReportAggregation::assessmentCount);
    }

    public int getSubmittedAttemptCount() {
        return indicator(ReportAggregation::submittedAttemptCount);
    }

    public int getCorrectedAttemptCount() {
        return indicator(ReportAggregation::correctedAttemptCount);
    }

    public int getPendingCorrectionCount() {
        return indicator(ReportAggregation::pendingCorrectionCount);
    }

    public int getAttendanceRecordCount() {
        return indicator(ReportAggregation::attendanceRecordCount);
    }

    public int getIssuedCertificateCount() {
        return indicator(ReportAggregation::issuedCertificateCount);
    }

    private int indicator(java.util.function.ToIntFunction<ReportAggregation> extractor) {
        return aggregation == null ? 0 : extractor.applyAsInt(aggregation);
    }

    public static String scopeLabel(ManagementViewScope scope) {
        return switch (scope) {
            case GLOBAL -> "Global";
            case ORGANIZATION -> "Organization";
            case COURSE -> "Course";
            case SUBJECT -> "Subject";
            case CLASS_GROUP -> "Class group";
            case PERSONAL -> "Personal";
        };
    }
}
