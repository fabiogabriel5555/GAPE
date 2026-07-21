package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.AttendanceRecordDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.ScheduleEventDAO;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.ScheduleEvent;

/**
 * Read-only, student-scoped metrics used by the student dashboard. The
 * dashboard never aggregates data outside the authenticated student's own
 * academic contexts.
 */
public final class StudentDashboardService {

    private static final int ACTIVITY_MONTHS = 6;

    private final ConnectionProvider connectionProvider;
    private final Clock clock;
    private final EnrollmentDAO enrollmentDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final LessonDAO lessonDAO;
    private final AssessmentDAO assessmentDAO;
    private final AttemptDAO attemptDAO;
    private final AttendanceRecordDAO attendanceRecordDAO;
    private final GradeRecordDAO gradeRecordDAO;
    private final CertificateDAO certificateDAO;
    private final ScheduleEventDAO scheduleEventDAO;

    public StudentDashboardService(ConnectionProvider connectionProvider, Clock clock) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.enrollmentDAO = new EnrollmentDAO(connectionProvider);
        this.classGroupEnrollmentDAO = new ClassGroupEnrollmentDAO(connectionProvider);
        this.lessonDAO = new LessonDAO(connectionProvider);
        this.assessmentDAO = new AssessmentDAO(connectionProvider);
        this.attemptDAO = new AttemptDAO(connectionProvider);
        this.attendanceRecordDAO = new AttendanceRecordDAO(connectionProvider);
        this.gradeRecordDAO = new GradeRecordDAO(connectionProvider);
        this.certificateDAO = new CertificateDAO(connectionProvider);
        this.scheduleEventDAO = new ScheduleEventDAO(connectionProvider);
    }

    public Snapshot load(long studentUserId) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            List<Lesson> lessons = lessonDAO.findForStudent(studentUserId);
            var attempts = attemptDAO.findByStudent(studentUserId);
            List<AttendanceRecord> attendance;
            List<GradeRecord> grades;
            List<Certificate> certificates;
            List<ScheduleEvent> events;
            try (Connection connection = connectionProvider.getConnection()) {
                attendance = attendanceRecordDAO.findByStudent(connection, studentUserId);
                grades = gradeRecordDAO.findVisiblePublishedByStudent(connection, studentUserId);
                certificates = certificateDAO.findByStudent(connection, studentUserId);
                events = scheduleEventDAO.findVisibleForStudent(connection, studentUserId);
            }

            int currentCourses = (int) enrollmentDAO.findCourseEnrollmentsByStudent(studentUserId).stream()
                    .filter(enrollment -> enrollment.state() == EnrollmentState.ACTIVE)
                    .filter(enrollment -> isEffective(enrollment.startDate(), enrollment.endDate(), today))
                    .map(enrollment -> enrollment.courseId())
                    .distinct()
                    .count();
            int currentClassGroups = (int) classGroupEnrollmentDAO.findByStudent(studentUserId).stream()
                    .filter(enrollment -> enrollment.state() == EnrollmentState.ACTIVE)
                    .filter(enrollment -> isEffective(enrollment.startDate(), enrollment.endDate(), today))
                    .map(enrollment -> enrollment.classGroupId())
                    .distinct()
                    .count();
            int upcomingLessons = (int) lessons.stream()
                    .filter(lesson -> lesson.state() == LessonState.SCHEDULED || lesson.state() == LessonState.ACTIVE)
                    .filter(lesson -> !lesson.endsAt().isBefore(now))
                    .count();
            int availableAssessments = assessmentDAO.findActiveAccessibleByStudent(studentUserId).size();
            List<AttendanceRecord> effectiveAttendance = attendance.stream()
                    .filter(record -> record.state() != AttendanceState.CANCELLED)
                    .toList();
            int attended = (int) effectiveAttendance.stream()
                    .filter(record -> record.status() == AttendanceStatus.PRESENT
                            || record.status() == AttendanceStatus.LATE
                            || record.status() == AttendanceStatus.PARTIAL)
                    .count();
            int notAttended = effectiveAttendance.size() - attended;
            int attendanceRate = effectiveAttendance.isEmpty()
                    ? 0
                    : Math.round((attended * 100f) / effectiveAttendance.size());
            List<BigDecimal> gradeValues = grades.stream()
                    .map(record -> record.value())
                    .filter(Objects::nonNull)
                    .toList();
            String averageGrade = gradeValues.isEmpty()
                    ? "-"
                    : gradeValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(gradeValues.size()), 1, RoundingMode.HALF_UP)
                    .toPlainString();
            int issuedCertificates = (int) certificates.stream()
                    .filter(certificate -> certificate.state() == CertificateState.ISSUED)
                    .count();
            int upcomingEvents = (int) events.stream()
                    .filter(event -> !event.endsAt().isBefore(now))
                    .count();

            List<String> labels = new ArrayList<>(ACTIVITY_MONTHS);
            List<Integer> lessonActivity = new ArrayList<>(ACTIVITY_MONTHS);
            List<Integer> assessmentActivity = new ArrayList<>(ACTIVITY_MONTHS);
            YearMonth month = YearMonth.from(today).minusMonths(ACTIVITY_MONTHS - 1L);
            for (int index = 0; index < ACTIVITY_MONTHS; index++, month = month.plusMonths(1)) {
                YearMonth targetMonth = month;
                labels.add(targetMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                lessonActivity.add((int) lessons.stream()
                        .filter(lesson -> YearMonth.from(lesson.startsAt()).equals(targetMonth))
                        .count());
                assessmentActivity.add((int) attempts.stream()
                        .filter(attempt -> YearMonth.from(attempt.startedAt()).equals(targetMonth))
                        .count());
            }

            return new Snapshot(
                    currentCourses,
                    currentClassGroups,
                    upcomingLessons,
                    availableAssessments,
                    attendanceRate,
                    averageGrade,
                    issuedCertificates,
                    upcomingEvents,
                    attended,
                    notAttended,
                    String.join(",", labels),
                    commaSeparated(lessonActivity),
                    commaSeparated(assessmentActivity)
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load the student dashboard", exception);
        }
    }

    private static boolean isEffective(LocalDate startDate, LocalDate endDate, LocalDate today) {
        return (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }

    private static String commaSeparated(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public record Snapshot(
            int currentCourses,
            int currentClassGroups,
            int upcomingLessons,
            int availableAssessments,
            int attendanceRate,
            String averageGrade,
            int issuedCertificates,
            int upcomingEvents,
            int attended,
            int notAttended,
            String activityLabels,
            String lessonActivity,
            String assessmentActivity
    ) {
        public int getCurrentCourses() { return currentCourses; }
        public int getCurrentClassGroups() { return currentClassGroups; }
        public int getUpcomingLessons() { return upcomingLessons; }
        public int getAvailableAssessments() { return availableAssessments; }
        public int getAttendanceRate() { return attendanceRate; }
        public String getAverageGrade() { return averageGrade; }
        public int getIssuedCertificates() { return issuedCertificates; }
        public int getUpcomingEvents() { return upcomingEvents; }
        public int getAttended() { return attended; }
        public int getNotAttended() { return notAttended; }
        public String getActivityLabels() { return activityLabels; }
        public String getLessonActivity() { return lessonActivity; }
        public String getAssessmentActivity() { return assessmentActivity; }
    }
}
