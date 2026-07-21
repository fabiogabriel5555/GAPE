package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Subject;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.AttemptView;
import pt.isel.gape.web.view.ClassGroupView;

/**
 * Prepares the assessment catalogue displayed alongside student lessons.
 * Assessment attempts and mutations remain handled by StudentAssessmentServlet;
 * this support class only builds the read model for the combined page.
 */
final class StudentAssessmentCatalogSupport {

    private final ApplicationReadService.Assessments assessmentDAO;
    private final ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO;
    private final ApplicationReadService.Attempts attemptDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final AssessmentViewFactory viewFactory;
    private final LearningViewFactory learningViewFactory;

    StudentAssessmentCatalogSupport(ApplicationReadService readService) {
        this.assessmentDAO = readService.assessments();
        this.assessmentEnrollmentDAO = readService.assessmentEnrollments();
        this.attemptDAO = readService.attempts();
        this.classGroupDAO = readService.classGroups();
        this.classGroupEnrollmentDAO = readService.classGroupEnrollments();
        this.courseDAO = readService.courses();
        this.subjectDAO = readService.subjects();
        this.viewFactory = new AssessmentViewFactory(
                readService.assessments(),
                readService.questions(),
                readService.questionOptions(),
                readService.attempts(),
                readService.responses(),
                readService.subjects(),
                readService.contentBlocks(),
                readService.classGroups(),
                readService.users()
        );
        this.learningViewFactory = new LearningViewFactory(
                readService.organizations(),
                readService.organicUnits(),
                readService.courses(),
                readService.courseOccurrences(),
                readService.subjects(),
                readService.courseSubjects(),
                readService.enrollments(),
                readService.classGroups(),
                readService.classGroupEnrollments(),
                readService.contentBlocks(),
                readService.users(),
                readService.teachClassGroups()
        );
    }

    void prepare(HttpServletRequest request, long studentUserId) throws SQLException {
        assessmentEnrollmentDAO.syncAllAutomaticEnrollments();
        List<AssessmentView> assessments = viewFactory.assessmentViews(
                assessmentDAO.findActiveAccessibleByStudent(studentUserId)
        );
        List<AssessmentView> completedAssessments = viewFactory.assessmentViews(
                completedAssessmentsVisibleToStudent(studentUserId)
        );
        List<AssessmentView> allAssessments = new ArrayList<>(assessments);
        allAssessments.addAll(completedAssessments);
        Map<Long, AttemptView> latestAttemptByAssessment = latestAttemptByAssessment(studentUserId);
        Map<Long, AssessmentEnrollment> enrollmentByAssessment = assessmentEnrollmentByAssessment(
                studentUserId,
                allAssessments
        );

        request.setAttribute("assessments", assessments);
        request.setAttribute("assessmentCompletedCourseGroups", assessmentCourseGroups(
                completedAssessments,
                studentUserId,
                true
        ));
        request.setAttribute("assessmentCourseGroups", assessmentCourseGroups(assessments, studentUserId, false));
        request.setAttribute("latestAttemptByAssessment", latestAttemptByAssessment);
        request.setAttribute("assessmentEnrollmentByAssessment", enrollmentByAssessment);
        request.setAttribute("assessmentEnrollmentStateByAssessment", enrollmentStates(enrollmentByAssessment));
        request.setAttribute("assessmentCount", assessments.size());
        request.setAttribute("activeAssessmentCount", assessments.size());
        request.setAttribute("completedAssessmentCount", completedAssessments.size());
        request.setAttribute("availableForms", assessments.stream().filter(AssessmentView::isForm).count());
        request.setAttribute("availableTests", assessments.stream().filter(AssessmentView::isTest).count());
        request.setAttribute("availableExams", assessments.stream().filter(AssessmentView::isExam).count());
    }

    private List<pt.isel.gape.learning.model.Assessment> completedAssessmentsVisibleToStudent(long studentUserId)
            throws SQLException {
        List<pt.isel.gape.learning.model.Assessment> completed = assessmentDAO.findAll().stream()
                .filter(assessment -> assessment.state() == AssessmentState.COMPLETED)
                .toList();
        if (completed.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ClassGroupEnrollment>> enrollmentsByClassGroup = new LinkedHashMap<>();
        for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByStudent(studentUserId)) {
            enrollmentsByClassGroup.computeIfAbsent(enrollment.classGroupId(), ignored -> new ArrayList<>())
                    .add(enrollment);
        }
        Map<Long, List<Long>> applicableClassGroups = assessmentDAO
                .findApplicableClassGroupIdsByAssessmentIds(completed.stream()
                        .map(pt.isel.gape.learning.model.Assessment::id)
                        .toList());
        java.util.Set<Long> attemptedAssessmentIds = attemptDAO.findByStudent(studentUserId).stream()
                .map(Attempt::assessmentId)
                .collect(java.util.stream.Collectors.toSet());
        return completed.stream()
                .filter(assessment -> attemptedAssessmentIds.contains(assessment.id())
                        || applicableClassGroups.getOrDefault(assessment.id(), List.of()).stream()
                                .anyMatch(classGroupId -> enrollmentsByClassGroup
                                        .getOrDefault(classGroupId, List.of()).stream()
                                        .anyMatch(enrollment -> enrollment.state() != EnrollmentState.PENDING
                                                && enrollment.state() != EnrollmentState.REJECTED)))
                .sorted(Comparator.comparing(pt.isel.gape.learning.model.Assessment::availableUntil,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingLong(pt.isel.gape.learning.model.Assessment::id))
                .toList();
    }

    private Map<Long, AttemptView> latestAttemptByAssessment(long studentUserId) throws SQLException {
        Map<Long, AttemptView> result = new LinkedHashMap<>();
        for (Attempt attempt : attemptDAO.findByStudent(studentUserId)) {
            result.putIfAbsent(attempt.assessmentId(), viewFactory.attemptView(attempt));
        }
        return result;
    }

    private Map<Long, AssessmentEnrollment> assessmentEnrollmentByAssessment(
            long studentUserId,
            List<AssessmentView> assessments
    ) throws SQLException {
        Map<Long, AssessmentEnrollment> result = new LinkedHashMap<>();
        for (AssessmentView assessment : assessments) {
            assessmentEnrollmentDAO.findEnrollment(studentUserId, assessment.getId())
                    .ifPresent(enrollment -> result.put(assessment.getId(), enrollment));
        }
        return result;
    }

    private static Map<Long, String> enrollmentStates(Map<Long, AssessmentEnrollment> enrollmentByAssessment) {
        Map<Long, String> result = new LinkedHashMap<>();
        enrollmentByAssessment.forEach((assessmentId, enrollment) -> result.put(
                assessmentId,
                enrollment.state().name()
        ));
        return result;
    }

    private List<StudentAssessmentCourseGroupView> assessmentCourseGroups(
            List<AssessmentView> assessments,
            long studentUserId,
            boolean includeHistoricalEnrollments
    )
            throws SQLException {
        Map<Long, Course> coursesById = new LinkedHashMap<>();
        for (Course course : courseDAO.findCatalogCourses(null, null, null)) {
            coursesById.put(course.id(), course);
        }
        Map<Long, Subject> subjectsById = new LinkedHashMap<>();
        for (Subject subject : subjectDAO.findAll()) {
            subjectsById.put(subject.id(), subject);
        }
        Map<Long, ClassGroup> classGroupsById = new LinkedHashMap<>();
        for (ClassGroup group : classGroupDAO.findAll()) {
            classGroupsById.put(group.id(), group);
        }
        Map<Long, ClassGroupView> classGroupViewsById = new LinkedHashMap<>();
        learningViewFactory.classGroupViews(new ArrayList<>(classGroupsById.values()))
                .forEach(group -> classGroupViewsById.put(group.getId(), group));
        Map<Long, List<ClassGroupEnrollment>> studentClassGroupEnrollments = new LinkedHashMap<>();
        for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByStudent(studentUserId)) {
            studentClassGroupEnrollments.computeIfAbsent(enrollment.classGroupId(), ignored -> new ArrayList<>())
                    .add(enrollment);
        }
        Map<Long, List<Long>> applicableClassGroupIds = assessmentDAO
                .findApplicableClassGroupIdsByAssessmentIds(assessments.stream().map(AssessmentView::getId).toList());
        Map<Long, StudentAssessmentCourseGroupView> courses = new LinkedHashMap<>();
        Map<String, StudentAssessmentSubjectGroupView> subjects = new LinkedHashMap<>();
        Map<String, StudentAssessmentOccurrenceGroupView> occurrences = new LinkedHashMap<>();
        Map<String, StudentAssessmentClassGroupView> groups = new LinkedHashMap<>();
        List<AssessmentPlacement> placements = new ArrayList<>();
        assessments.forEach(assessment -> {
            List<Long> ids = assessment.getClassGroupId() == null
                    ? applicableClassGroupIds.getOrDefault(assessment.getId(), List.of())
                    : List.of(assessment.getClassGroupId());
            if (ids.isEmpty()) {
                placements.add(new AssessmentPlacement(assessment, null));
            } else {
                ids.stream()
                        .distinct()
                        .filter(id -> studentClassGroupEnrollments.getOrDefault(id, List.of()).stream()
                                .anyMatch(enrollment -> includeHistoricalEnrollments
                                        ? enrollment.state() != EnrollmentState.PENDING
                                                && enrollment.state() != EnrollmentState.REJECTED
                                        : enrollment.state() == EnrollmentState.ACTIVE))
                        .forEach(id -> placements.add(
                                new AssessmentPlacement(assessment, classGroupViewsById.get(id))));
            }
        });
        placements.stream()
                .sorted(Comparator
                        .comparing((AssessmentPlacement placement) -> placement.classGroupView() == null
                                ? "Other assessments" : placement.classGroupView().getCourseName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(placement -> placement.classGroupView() == null
                                ? placement.assessment().getSubjectName() : placement.classGroupView().getSubjectName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(placement -> placement.classGroupView() == null
                                ? "" : placement.classGroupView().getOccurrenceLabel(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(placement -> placement.classGroupView() == null
                                ? "" : placement.classGroupView().getCode(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(placement -> placement.assessment().getTitle(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(placement -> placement.assessment().getId()))
                .forEach(placement -> {
                    AssessmentView assessment = placement.assessment();
                    ClassGroupView classGroupView = placement.classGroupView();
                    long courseId = classGroupView == null ? 0L : classGroupView.getCourseId();
                    long subjectId = classGroupView == null && assessment.getSubjectId() != null
                            ? assessment.getSubjectId() : classGroupView == null ? 0L : classGroupView.getSubjectId();
                    String courseName = courseId == 0L || coursesById.get(courseId) == null
                            ? "Other assessments" : coursesById.get(courseId).name();
                    String subjectName = subjectId == 0L || subjectsById.get(subjectId) == null
                            ? assessment.getSubjectName() : subjectsById.get(subjectId).name();
                    StudentAssessmentCourseGroupView course = courses.computeIfAbsent(courseId,
                            id -> new StudentAssessmentCourseGroupView(id, courseName));
                    String subjectKey = courseId + ":" + subjectId;
                    StudentAssessmentSubjectGroupView subject = subjects.computeIfAbsent(subjectKey, key -> {
                        StudentAssessmentSubjectGroupView created = new StudentAssessmentSubjectGroupView(subjectId, subjectName);
                        course.addSubjectGroup(created);
                        return created;
                    });
                    long occurrenceId = classGroupView == null ? 0L : classGroupView.getCourseOccurrenceId();
                    String occurrenceKey = subjectKey + ":" + occurrenceId;
                    StudentAssessmentOccurrenceGroupView occurrence = occurrences.computeIfAbsent(occurrenceKey, key -> {
                        StudentAssessmentOccurrenceGroupView created = new StudentAssessmentOccurrenceGroupView(
                                occurrenceId,
                                classGroupView == null ? "Occurrence" : classGroupView.getOccurrenceLabel(),
                                classGroupView == null ? "-" : classGroupView.getOccurrenceDateRangeLabel()
                        );
                        subject.addOccurrenceGroup(created);
                        return created;
                    });
                    long classGroupId = classGroupView == null ? 0L : classGroupView.getId();
                    String groupKey = occurrenceKey + ":" + classGroupId;
                    StudentAssessmentClassGroupView group = groups.computeIfAbsent(groupKey, key -> {
                        StudentAssessmentClassGroupView created = new StudentAssessmentClassGroupView(
                                classGroupId,
                                classGroupView == null ? "Subject assessment" : classGroupView.getCode()
                        );
                        occurrence.addClassGroup(created);
                        return created;
                    });
                    group.addAssessment(assessment);
                });
        return new ArrayList<>(courses.values());
    }

    private record AssessmentPlacement(AssessmentView assessment, ClassGroupView classGroupView) { }

    public static final class StudentAssessmentCourseGroupView {
        private final long id; private final String name;
        private final List<StudentAssessmentSubjectGroupView> subjectGroups = new ArrayList<>();
        StudentAssessmentCourseGroupView(long id, String name) { this.id = id; this.name = name; }
        public long getId() { return id; } public String getName() { return name; }
        public List<StudentAssessmentSubjectGroupView> getSubjectGroups() { return subjectGroups; }
        public int getAssessmentCount() { return subjectGroups.stream().mapToInt(StudentAssessmentSubjectGroupView::getAssessmentCount).sum(); }
        void addSubjectGroup(StudentAssessmentSubjectGroupView group) { subjectGroups.add(group); }
    }

    public static final class StudentAssessmentSubjectGroupView {
        private final long id; private final String name;
        private final List<StudentAssessmentOccurrenceGroupView> occurrenceGroups = new ArrayList<>();
        StudentAssessmentSubjectGroupView(long id, String name) { this.id = id; this.name = name; }
        public long getId() { return id; } public String getName() { return name; }
        public List<StudentAssessmentOccurrenceGroupView> getOccurrenceGroups() { return occurrenceGroups; }
        public List<StudentAssessmentClassGroupView> getClassGroups() {
            return occurrenceGroups.stream()
                    .flatMap(group -> group.getClassGroups().stream())
                    .toList();
        }
        public int getAssessmentCount() {
            return occurrenceGroups.stream().mapToInt(StudentAssessmentOccurrenceGroupView::getAssessmentCount).sum();
        }
        void addOccurrenceGroup(StudentAssessmentOccurrenceGroupView group) { occurrenceGroups.add(group); }
    }

    public static final class StudentAssessmentOccurrenceGroupView {
        private final long id;
        private final String label;
        private final String dateRangeLabel;
        private final List<StudentAssessmentClassGroupView> classGroups = new ArrayList<>();

        StudentAssessmentOccurrenceGroupView(long id, String label, String dateRangeLabel) {
            this.id = id;
            this.label = label;
            this.dateRangeLabel = dateRangeLabel;
        }

        public long getId() { return id; }
        public String getLabel() { return label; }
        public String getDateRangeLabel() { return dateRangeLabel; }
        public List<StudentAssessmentClassGroupView> getClassGroups() { return classGroups; }
        public List<AssessmentView> getAssessments() {
            return classGroups.stream().flatMap(group -> group.getAssessments().stream()).toList();
        }
        public int getAssessmentCount() {
            return classGroups.stream().mapToInt(StudentAssessmentClassGroupView::getAssessmentCount).sum();
        }
        void addClassGroup(StudentAssessmentClassGroupView group) { classGroups.add(group); }
    }

    public static final class StudentAssessmentClassGroupView {
        private final long id; private final String code;
        private final List<AssessmentView> assessments = new ArrayList<>();
        StudentAssessmentClassGroupView(long id, String code) { this.id = id; this.code = code; }
        public long getId() { return id; } public String getCode() { return code; }
        public List<AssessmentView> getAssessments() { return assessments; }
        public int getAssessmentCount() { return assessments.size(); }
        void addAssessment(AssessmentView assessment) { assessments.add(assessment); }
    }
}
