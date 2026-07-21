package pt.isel.gape.web.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A course-detail enrollment row is a student, with the student's individual
 * course-occurrence enrollments kept below it.  Keeping this grouping in the
 * view layer prevents the JSP from having to infer student boundaries from a
 * flat, ordered list.
 */
public final class CourseEnrollmentStudentGroupView {

    private static final Comparator<EnrollmentManagementView> STUDENT_ORDER = Comparator
            .comparingLong(EnrollmentManagementView::getStudentUserId)
            .reversed();

    private static final Comparator<EnrollmentManagementView> ENROLLMENT_ORDER = Comparator
            .comparing(EnrollmentManagementView::getStartDateValue, Comparator.reverseOrder())
            .thenComparing(Comparator.comparingLong(EnrollmentManagementView::getCourseOccurrenceId).reversed());

    private final List<EnrollmentManagementView> enrollments;
    private final List<StateSummaryView> stateSummaries;

    private CourseEnrollmentStudentGroupView(List<EnrollmentManagementView> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            throw new IllegalArgumentException("A student enrollment group requires at least one enrollment");
        }
        this.enrollments = List.copyOf(enrollments);
        this.stateSummaries = stateSummaries(enrollments);
    }

    public static List<CourseEnrollmentStudentGroupView> group(List<EnrollmentManagementView> enrollments) {
        Objects.requireNonNull(enrollments, "enrollments is required");
        Map<Long, List<EnrollmentManagementView>> byStudent = new LinkedHashMap<>();
        enrollments.stream()
                .sorted(STUDENT_ORDER.thenComparing(ENROLLMENT_ORDER))
                .forEach(enrollment -> byStudent
                        .computeIfAbsent(enrollment.getStudentUserId(), ignored -> new ArrayList<>())
                        .add(enrollment));

        return byStudent.values().stream()
                .map(studentEnrollments -> studentEnrollments.stream().sorted(ENROLLMENT_ORDER).toList())
                .map(CourseEnrollmentStudentGroupView::new)
                .toList();
    }

    public long getStudentUserId() {
        return primaryEnrollment().getStudentUserId();
    }

    public String getStudentName() {
        return primaryEnrollment().getStudentName();
    }

    public String getStudentEmail() {
        return primaryEnrollment().getStudentEmail();
    }

    public int getEnrollmentCount() {
        return enrollments.size();
    }

    public String getLatestEnrollmentStartDateValue() {
        return primaryEnrollment().getStartDateValue();
    }

    public String getStateSortLabel() {
        return stateSummaries.isEmpty() ? "" : stateSummaries.get(0).getLabel();
    }

    public List<EnrollmentManagementView> getEnrollments() {
        return enrollments;
    }

    public List<StateSummaryView> getStateSummaries() {
        return stateSummaries;
    }

    private EnrollmentManagementView primaryEnrollment() {
        return enrollments.get(0);
    }

    private static List<StateSummaryView> stateSummaries(List<EnrollmentManagementView> enrollments) {
        Map<String, StateSummaryAccumulator> summaries = new LinkedHashMap<>();
        for (EnrollmentManagementView enrollment : enrollments) {
            summaries.computeIfAbsent(
                            enrollment.getStateValue(),
                            ignored -> new StateSummaryAccumulator(
                                    enrollment.getStateLabel(),
                                    enrollment.getStateBadgeClass()
                            )
                    )
                    .increment();
        }
        return summaries.values().stream()
                .map(summary -> new StateSummaryView(summary.count, summary.label, summary.badgeClass))
                .toList();
    }

    public static final class StateSummaryView {
        private final int count;
        private final String label;
        private final String badgeClass;

        private StateSummaryView(int count, String label, String badgeClass) {
            this.count = count;
            this.label = label;
            this.badgeClass = badgeClass;
        }

        public int getCount() {
            return count;
        }

        public String getLabel() {
            return label;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }

    private static final class StateSummaryAccumulator {
        private final String label;
        private final String badgeClass;
        private int count;

        private StateSummaryAccumulator(String label, String badgeClass) {
            this.label = label;
            this.badgeClass = badgeClass;
        }

        private void increment() {
            count++;
        }
    }
}
