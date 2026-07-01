package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;

public final class CourseSubjectView {

    private final long courseId;
    private final long subjectId;
    private final SubjectView subject;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;
    private final CourseSubjectState state;
    private final SubjectEnrollment enrollment;

    private CourseSubjectView(CourseSubjectAssociation association, SubjectView subject, SubjectEnrollment enrollment) {
        this.courseId = association.courseId();
        this.subjectId = association.subjectId();
        this.subject = subject;
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
        this.state = association.state();
        this.enrollment = enrollment;
    }

    public static CourseSubjectView from(CourseSubjectAssociation association, SubjectView subject) {
        return new CourseSubjectView(association, subject, null);
    }

    public static CourseSubjectView from(
            CourseSubjectAssociation association,
            SubjectView subject,
            SubjectEnrollment enrollment
    ) {
        return new CourseSubjectView(association, subject, enrollment);
    }

    public long getCourseId() {
        return courseId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public SubjectView getSubject() {
        return subject;
    }

    public String getSubjectName() {
        return subject.getName();
    }

    public String getSubjectAcronym() {
        return subject.getAcronym();
    }

    public String getSubjectEctsLabel() {
        return subject.getEctsLabel();
    }

    public BigDecimal getSubjectEcts() {
        return subject.getEcts();
    }

    public String getSubjectFinalGradeMaxLabel() {
        return subject.getFinalGradeMaxLabel();
    }

    public String getSubjectWorkloadHoursLabel() {
        return subject.getWorkloadHoursLabel();
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public String getTerm() {
        return term == null ? "" : term.name();
    }

    public String getCurricularPositionLabel() {
        if (curricularYear == null || term == null) {
            return "No curricular position";
        }
        return curricularYear + " year | " + labelFor(term);
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
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

    public boolean isArchived() {
        return state == CourseSubjectState.INACTIVE;
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

    public boolean isPendingEnrollment() {
        return enrollment != null && enrollment.state() == EnrollmentState.PENDING;
    }

    public static String labelFor(CurricularTerm term) {
        return switch (term) {
            case ANNUAL -> "Annual";
            case SEMESTER_1 -> "1st semester";
            case SEMESTER_2 -> "2nd semester";
            case TRIMESTER_1 -> "1st trimester";
            case TRIMESTER_2 -> "2nd trimester";
            case TRIMESTER_3 -> "3rd trimester";
        };
    }

    private static boolean isWithinCurrentDate(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }
}
