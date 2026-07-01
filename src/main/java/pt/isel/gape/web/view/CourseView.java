package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.common.validation.MediaPathValidator;

public final class CourseView {

    private final long id;
    private final long organizationId;
    private final Long organicUnitId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String description;
    private final BigDecimal ects;
    private final BigDecimal certificateMaxGrade;
    private final String duration;
    private final CourseType type;
    private final CourseState state;
    private final String organizationName;
    private final String organizationAcronym;
    private final String organicUnitName;
    private final String organicUnitAcronym;
    private final int subjectCount;
    private final CourseEnrollment enrollment;

    private CourseView(
            Course course,
            String organizationName,
            String organizationAcronym,
            String organicUnitName,
            String organicUnitAcronym,
            int subjectCount,
            CourseEnrollment enrollment
    ) {
        this.id = course.id();
        this.organizationId = course.organizationId();
        this.organicUnitId = course.organicUnitId();
        this.name = course.name();
        this.acronym = course.acronym();
        this.photo = MediaPathValidator.safeRelativePath(course.photo()).orElse(null);
        this.description = course.description();
        this.ects = course.ects();
        this.certificateMaxGrade = course.certificateMaxGrade();
        this.duration = course.duration();
        this.type = course.type();
        this.state = course.state();
        this.organizationName = organizationName;
        this.organizationAcronym = organizationAcronym;
        this.organicUnitName = organicUnitName;
        this.organicUnitAcronym = organicUnitAcronym;
        this.subjectCount = subjectCount;
        this.enrollment = enrollment;
    }

    public static CourseView from(
            Course course,
            String organizationName,
            String organizationAcronym,
            String organicUnitName,
            String organicUnitAcronym,
            int subjectCount
    ) {
        return new CourseView(course, organizationName, organizationAcronym, organicUnitName, organicUnitAcronym, subjectCount, null);
    }

    public static CourseView from(
            Course course,
            String organizationName,
            String organizationAcronym,
            String organicUnitName,
            String organicUnitAcronym,
            int subjectCount,
            CourseEnrollment enrollment
    ) {
        return new CourseView(course, organizationName, organizationAcronym, organicUnitName, organicUnitAcronym, subjectCount, enrollment);
    }

    public long getId() {
        return id;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public Long getOrganicUnitId() {
        return organicUnitId;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? "-" : acronym;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean isHasPhoto() {
        return photo != null && !photo.isBlank();
    }

    public String getDescription() {
        return description == null || description.isBlank() ? "Course information will be updated by the academic team." : description;
    }

    public String getEctsLabel() {
        return ects == null ? "-" : formatDecimal(ects) + " ECTS";
    }

    public String getCertificateMaxGradeLabel() {
        return certificateMaxGrade == null ? "-" : formatDecimal(certificateMaxGrade);
    }

    public String getDurationLabel() {
        return duration == null || duration.isBlank() ? "-" : duration;
    }

    public String getType() {
        return type.name();
    }

    public String getTypeLabel() {
        return labelFor(type);
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return state == CourseState.ACTIVE;
    }

    public boolean isArchived() {
        return state == CourseState.INACTIVE;
    }

    public String getOrganizationName() {
        return organizationName == null || organizationName.isBlank() ? "Unknown organization" : organizationName;
    }

    public String getOrganizationAcronym() {
        return organizationAcronym == null || organizationAcronym.isBlank() ? "" : organizationAcronym;
    }

    public String getOrganizationContextLabel() {
        return compactPart(getOrganizationAcronym(), getOrganizationName());
    }

    public String getOrganizationContextHtml() {
        return contextPartHtml(getOrganizationAcronym(), getOrganizationName());
    }

    public String getOrganicUnitLabel() {
        return organicUnitName == null || organicUnitName.isBlank() ? "No organic unit" : organicUnitName;
    }

    public String getOrganicUnitAcronym() {
        return organicUnitAcronym == null || organicUnitAcronym.isBlank() ? "" : organicUnitAcronym;
    }

    public boolean isHasOrganicUnit() {
        return organicUnitId != null;
    }

    public String getCourseManagementContextLabel() {
        return isHasOrganicUnit()
                ? compactPart(getOrganicUnitAcronym(), getOrganicUnitLabel()) + " | " + getOrganizationContextLabel()
                : getOrganizationContextLabel();
    }

    public String getCourseManagementContextHtml() {
        return isHasOrganicUnit()
                ? contextPartHtml(getOrganicUnitAcronym(), getOrganicUnitLabel()) + " | " + getOrganizationContextHtml()
                : getOrganizationContextHtml();
    }

    public String getCourseManagementContextTitle() {
        return isHasOrganicUnit()
                ? getOrganicUnitLabel() + " | " + getOrganizationName()
                : getOrganizationName();
    }

    public String getSubjectManagementContextLabel() {
        return isHasOrganicUnit()
                ? getAcronym() + " | " + compactPart(getOrganicUnitAcronym(), getOrganicUnitLabel()) + " | " + getOrganizationContextLabel()
                : getAcronym() + " | " + getOrganizationContextLabel();
    }

    public String getSubjectManagementContextHtml() {
        return isHasOrganicUnit()
                ? contextPartHtml(getAcronym(), getName()) + " | " + contextPartHtml(getOrganicUnitAcronym(), getOrganicUnitLabel()) + " | " + getOrganizationContextHtml()
                : contextPartHtml(getAcronym(), getName()) + " | " + getOrganizationContextHtml();
    }

    public String getSubjectManagementContextTitle() {
        return isHasOrganicUnit()
                ? getName() + " | " + getOrganicUnitLabel() + " | " + getOrganizationName()
                : getName() + " | " + getOrganizationName();
    }

    public int getSubjectCount() {
        return subjectCount;
    }

    public String getSubjectCountLabel() {
        return subjectCount == 1 ? "1 Subject" : subjectCount + " Subjects";
    }

    public String getThumbnail() {
        if (isHasPhoto()) {
            return "media/" + photo;
        }
        return null;
    }

    public boolean isEnrolled() {
        return enrollment != null;
    }

    public boolean isActiveEnrollment() {
        return enrollment != null
                && enrollment.state() == EnrollmentState.ACTIVE
                && isWithinCurrentDate(enrollment.startDate(), enrollment.endDate());
    }

    public String getEnrollmentStateLabel() {
        if (enrollment == null) {
            return "Not enrolled";
        }
        return switch (enrollment.state()) {
            case PENDING -> "Pending approval";
            case ACTIVE -> "Enrolled";
            case INACTIVE -> "Inactive";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
            case WITHDRAWN -> "Left";
        };
    }

    public String getEnrollmentBadgeClass() {
        if (enrollment == null) {
            return "bg-neutral-30 text-neutral-600";
        }
        return switch (enrollment.state()) {
            case PENDING -> "bg-warning-30 text-warning-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-info-50 text-info-600";
            case WITHDRAWN -> "bg-warning-30 text-warning-600";
        };
    }

    public EnrollmentState getEnrollmentState() {
        return enrollment == null ? null : enrollment.state();
    }

    public String getEnrollmentStartDate() {
        return enrollment == null || enrollment.startDate() == null ? "-" : enrollment.startDate().toString();
    }

    public String getEnrollmentEndDate() {
        return enrollment == null || enrollment.endDate() == null ? "-" : enrollment.endDate().toString();
    }

    public static String labelFor(CourseType type) {
        return switch (type) {
            case DEGREE -> "Degree";
            case MASTER -> "Master";
            case SHORT_COURSE -> "Short course";
            case PROFESSIONAL_TRAINING -> "Professional training";
            case OTHER -> "Other";
        };
    }

    private static String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static boolean isWithinCurrentDate(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }

    private static String compactPart(String acronym, String name) {
        if (acronym != null && !acronym.isBlank()) {
            return acronym;
        }
        return name == null || name.isBlank() ? "-" : name;
    }

    private static String contextPartHtml(String acronym, String name) {
        String compact = compactPart(acronym, name);
        if (acronym == null || acronym.isBlank()) {
            return escapeHtml(compact);
        }
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(name == null || name.isBlank() ? compact : name)
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
