package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.AttendanceRecordDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.transversal.dao.ManagementViewContextAccessDAO;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ReportAggregation;

/**
 * Produces contextual dashboard/report indicators exclusively through the
 * existing domain DAOs.  It deliberately contains no JDBC/SQL so the
 * application remains Servlet -> Service -> DAO -> JDBC.
 */
public final class ReportAggregationService {

    private final ConnectionProvider connectionProvider;
    private final ManagementViewAccessService accessService;
    private final ManagementViewContextAccessDAO contextAccessDAO;
    private final Clock clock;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final LessonDAO lessonDAO;
    private final AssessmentDAO assessmentDAO;
    private final AttemptDAO attemptDAO;
    private final AttendanceRecordDAO attendanceRecordDAO;
    private final CertificateDAO certificateDAO;

    public ReportAggregationService(ConnectionProvider connectionProvider) {
        this(connectionProvider, ApplicationClock.system());
    }

    public ReportAggregationService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ManagementViewAccessService(connectionProvider, clock),
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new ClassGroupEnrollmentDAO(connectionProvider),
                new LessonDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new AttendanceRecordDAO(connectionProvider),
                new CertificateDAO(connectionProvider),
                new ManagementViewContextAccessDAO(connectionProvider),
                clock
        );
    }

    public ReportAggregationService(
            ConnectionProvider connectionProvider,
            ManagementViewAccessService accessService,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            ClassGroupDAO classGroupDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            LessonDAO lessonDAO,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            AttendanceRecordDAO attendanceRecordDAO,
            CertificateDAO certificateDAO
    ) {
        this(
                connectionProvider,
                accessService,
                courseDAO,
                subjectDAO,
                courseSubjectDAO,
                classGroupDAO,
                classGroupEnrollmentDAO,
                lessonDAO,
                assessmentDAO,
                attemptDAO,
                attendanceRecordDAO,
                certificateDAO,
                new ManagementViewContextAccessDAO(connectionProvider),
                ApplicationClock.system()
        );
    }

    public ReportAggregationService(
            ConnectionProvider connectionProvider,
            ManagementViewAccessService accessService,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            ClassGroupDAO classGroupDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            LessonDAO lessonDAO,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            AttendanceRecordDAO attendanceRecordDAO,
            CertificateDAO certificateDAO,
            ManagementViewContextAccessDAO contextAccessDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.accessService = Objects.requireNonNull(accessService, "accessService is required");
        this.contextAccessDAO = Objects.requireNonNull(contextAccessDAO, "contextAccessDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.classGroupEnrollmentDAO = Objects.requireNonNull(
                classGroupEnrollmentDAO,
                "classGroupEnrollmentDAO is required"
        );
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.attemptDAO = Objects.requireNonNull(attemptDAO, "attemptDAO is required");
        this.attendanceRecordDAO = Objects.requireNonNull(attendanceRecordDAO, "attendanceRecordDAO is required");
        this.certificateDAO = Objects.requireNonNull(certificateDAO, "certificateDAO is required");
    }

    /**
     * Opens the requested view through the scope authorization service and
     * then computes its reusable indicator snapshot.
     */
    public ReportAggregation aggregate(AccessContext actor, long managementViewId) {
        ManagementView view = accessService.requireAccess(actor, managementViewId);
        try {
            return aggregateAuthorizedView(actor, view);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to aggregate report for management view " + managementViewId, exception);
        }
    }

    /** Semantic alias for controllers that explicitly render a report. */
    public ReportAggregation aggregateReport(AccessContext actor, long managementViewId) {
        return aggregate(actor, managementViewId);
    }

    private ReportAggregation aggregateAuthorizedView(AccessContext actor, ManagementView view) throws SQLException {
        validateViewContext(view);
        ScopeData scopeData = resolveScope(actor, view);
        IndicatorData indicators = loadIndicators(scopeData);
        return new ReportAggregation(
                view.visibilityScope(),
                view.scopeContextId(),
                view.ownerUserId(),
                scopeData.courseIds().size(),
                scopeData.subjectIds().size(),
                scopeData.classGroups().size(),
                indicators.activeEnrollmentCount(),
                indicators.lessonCount(),
                indicators.assessmentCount(),
                indicators.submittedAttemptCount(),
                indicators.correctedAttemptCount(),
                indicators.submittedAttemptCount(),
                indicators.attendanceRecordCount(),
                indicators.issuedCertificateCount()
        );
    }

    private ScopeData resolveScope(AccessContext actor, ManagementView view) throws SQLException {
        Long contextId = view.scopeContextId();
        Long studentUserId = actor.profileType() == pt.isel.gape.access.model.AccessProfileType.STUDENT
                ? actor.userId()
                : null;
        Set<Long> currentStudentClassGroupIds = studentUserId == null
                ? Set.of()
                : contextAccessDAO.findCurrentStudentClassGroupIds(studentUserId, LocalDate.now(clock));
        return switch (view.visibilityScope()) {
            case GLOBAL -> scopeData(courseDAO.findAll(), subjectDAO.findAll(), classGroupDAO.findAll(), null);
            case ORGANIZATION -> resolveOrganizationScope(requiredContextId(contextId));
            case COURSE -> resolveCourseScope(requiredContextId(contextId), studentUserId, currentStudentClassGroupIds);
            case SUBJECT -> resolveSubjectScope(requiredContextId(contextId), studentUserId, currentStudentClassGroupIds);
            case CLASS_GROUP -> resolveClassGroupScope(requiredContextId(contextId), studentUserId, currentStudentClassGroupIds);
            case PERSONAL -> resolvePersonalScope(requiredContextId(contextId), currentStudentClassGroupIds);
        };
    }

    private ScopeData resolveOrganizationScope(long organizationId) throws SQLException {
        List<Course> courses = courseDAO.findByOrganization(organizationId);
        List<Subject> subjects = subjectDAO.findByOrganization(organizationId);
        Set<Long> courseIds = idsOfCourses(courses);
        List<ClassGroup> classGroups = classGroupDAO.findAll().stream()
                .filter(classGroup -> courseIds.contains(classGroup.courseId()))
                .toList();
        return scopeData(courses, subjects, classGroups, null);
    }

    private ScopeData resolveCourseScope(long courseId, Long studentUserId, Set<Long> currentStudentClassGroupIds)
            throws SQLException {
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course scope target not found: " + courseId));
        List<ClassGroup> classGroups = filterForStudent(
                classGroupDAO.findByCourse(courseId),
                studentUserId,
                currentStudentClassGroupIds
        );
        if (studentUserId != null) {
            List<Subject> subjects = subjectsForClassGroups(classGroups);
            return scopeData(List.of(course), subjects, classGroups, studentUserId);
        }
        List<CourseSubjectAssociation> associations = courseSubjectDAO.findByCourse(courseId);
        Set<Long> subjectIds = associations.stream()
                .map(CourseSubjectAssociation::subjectId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<Subject> subjects = new ArrayList<>();
        for (Long subjectId : subjectIds) {
            subjectDAO.findById(subjectId).ifPresent(subjects::add);
        }
        return scopeData(List.of(course), subjects, classGroups, null);
    }

    private ScopeData resolveSubjectScope(long subjectId, Long studentUserId, Set<Long> currentStudentClassGroupIds)
            throws SQLException {
        Subject subject = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject scope target not found: " + subjectId));
        List<ClassGroup> classGroups = filterForStudent(
                classGroupDAO.findBySubject(subjectId),
                studentUserId,
                currentStudentClassGroupIds
        );
        if (studentUserId != null) {
            List<Course> courses = courseDAO.findByIds(classGroups.stream().map(ClassGroup::courseId).toList());
            return scopeData(courses, List.of(subject), classGroups, studentUserId);
        }
        List<CourseSubjectAssociation> associations = courseSubjectDAO.findBySubject(subjectId);
        List<Course> courses = courseDAO.findByIds(
                associations.stream().map(CourseSubjectAssociation::courseId).toList()
        );
        return scopeData(courses, List.of(subject), classGroups, null);
    }

    private ScopeData resolveClassGroupScope(
            long classGroupId,
            Long studentUserId,
            Set<Long> currentStudentClassGroupIds
    ) throws SQLException {
        if (studentUserId != null && !currentStudentClassGroupIds.contains(classGroupId)) {
            throw new SecurityException("Student no longer has a current class-group report context");
        }
        ClassGroup classGroup = classGroupDAO.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class-group scope target not found: " + classGroupId));
        Course course = courseDAO.findById(classGroup.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Class group has no course: " + classGroupId));
        Subject subject = subjectDAO.findById(classGroup.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("Class group has no subject: " + classGroupId));
        return scopeData(List.of(course), List.of(subject), List.of(classGroup), studentUserId);
    }

    private ScopeData resolvePersonalScope(long studentUserId, Set<Long> currentStudentClassGroupIds) throws SQLException {
        Map<Long, ClassGroup> groups = new LinkedHashMap<>();
        for (Long classGroupId : currentStudentClassGroupIds) {
            classGroupDAO.findById(classGroupId)
                    .ifPresent(classGroup -> groups.put(classGroup.id(), classGroup));
        }
        List<ClassGroup> classGroups = List.copyOf(groups.values());
        List<Course> courses = courseDAO.findByIds(classGroups.stream().map(ClassGroup::courseId).toList());
        Map<Long, Subject> subjects = new LinkedHashMap<>();
        for (ClassGroup classGroup : classGroups) {
            subjectDAO.findById(classGroup.subjectId())
                    .ifPresent(subject -> subjects.put(subject.id(), subject));
        }
        return scopeData(courses, List.copyOf(subjects.values()), classGroups, studentUserId);
    }

    /**
     * Student-visible reports only retain class groups that are active in the
     * actor's present enrollment context.  This is intentionally an
     * intersection rather than a general scope lookup: a valid course or
     * subject report must never turn into a cohort-wide roll-up for a student.
     */
    private static List<ClassGroup> filterForStudent(
            Collection<ClassGroup> classGroups,
            Long studentUserId,
            Set<Long> currentStudentClassGroupIds
    ) {
        if (studentUserId == null) {
            return List.copyOf(classGroups);
        }
        return classGroups.stream()
                .filter(classGroup -> currentStudentClassGroupIds.contains(classGroup.id()))
                .toList();
    }

    private List<Subject> subjectsForClassGroups(Collection<ClassGroup> classGroups) throws SQLException {
        Map<Long, Subject> subjects = new LinkedHashMap<>();
        for (ClassGroup classGroup : classGroups) {
            subjectDAO.findById(classGroup.subjectId())
                    .ifPresent(subject -> subjects.put(subject.id(), subject));
        }
        return List.copyOf(subjects.values());
    }

    private IndicatorData loadIndicators(ScopeData scopeData) throws SQLException {
        List<Long> classGroupIds = scopeData.classGroups().stream().map(ClassGroup::id).toList();
        int activeEnrollmentCount = countActiveEnrollments(classGroupIds, scopeData.studentUserId());

        Map<Long, Lesson> lessons = new LinkedHashMap<>();
        Map<Long, Assessment> assessments = new LinkedHashMap<>();
        for (Long classGroupId : classGroupIds) {
            for (Lesson lesson : lessonDAO.findByClassGroup(classGroupId)) {
                lessons.put(lesson.id(), lesson);
            }
            for (Assessment assessment : assessmentDAO.findByClassGroup(classGroupId)) {
                assessments.put(assessment.id(), assessment);
            }
        }

        List<Long> assessmentIds = List.copyOf(assessments.keySet());
        int submittedAttemptCount = scopeData.studentUserId() == null
                ? sum(attemptDAO.countByAssessmentIdsAndState(assessmentIds, AttemptState.SUBMITTED))
                : countStudentAttempts(assessmentIds, scopeData.studentUserId(), AttemptState.SUBMITTED);
        int correctedAttemptCount = scopeData.studentUserId() == null
                ? sum(attemptDAO.countByAssessmentIdsAndState(assessmentIds, AttemptState.CORRECTED))
                : countStudentAttempts(assessmentIds, scopeData.studentUserId(), AttemptState.CORRECTED);
        int attendanceRecordCount = countRelevantAttendance(lessons.keySet(), scopeData.studentUserId());
        int issuedCertificateCount = countIssuedCertificates(
                scopeData.courseIds(),
                idsOfCourseOccurrences(scopeData.classGroups()),
                scopeData.studentUserId()
        );

        return new IndicatorData(
                activeEnrollmentCount,
                lessons.size(),
                assessments.size(),
                submittedAttemptCount,
                correctedAttemptCount,
                attendanceRecordCount,
                issuedCertificateCount
        );
    }

    private int countActiveEnrollments(Collection<Long> classGroupIds, Long studentUserId) throws SQLException {
        return Math.toIntExact(classGroupEnrollmentDAO.findByClassGroups(classGroupIds).stream()
                .filter(enrollment -> enrollment.state() == EnrollmentState.ACTIVE)
                .filter(enrollment -> studentUserId == null || enrollment.studentUserId() == studentUserId)
                .count());
    }

    private int countStudentAttempts(
            Collection<Long> assessmentIds,
            long studentUserId,
            AttemptState state
    ) throws SQLException {
        int count = 0;
        for (Long assessmentId : assessmentIds) {
            count += Math.toIntExact(attemptDAO.findByStudentAndAssessment(studentUserId, assessmentId).stream()
                    .filter(attempt -> attempt.state() == state)
                    .count());
        }
        return count;
    }

    private int countRelevantAttendance(Collection<Long> lessonIds, Long studentUserId) throws SQLException {
        if (lessonIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        try (Connection connection = connectionProvider.getConnection()) {
            for (Long lessonId : lessonIds) {
                if (studentUserId == null) {
                    count += Math.toIntExact(attendanceRecordDAO.findByLesson(connection, lessonId).stream()
                            .filter(record -> record.state() != AttendanceState.CANCELLED)
                            .count());
                } else if (attendanceRecordDAO.findByLessonAndStudent(connection, lessonId, studentUserId)
                        .filter(record -> record.state() != AttendanceState.CANCELLED)
                        .isPresent()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countIssuedCertificates(
            Set<Long> courseIds,
            Set<Long> courseOccurrenceIds,
            Long studentUserId
    ) throws SQLException {
        if (courseIds.isEmpty()) {
            return 0;
        }
        List<Certificate> certificates;
        if (studentUserId != null) {
            try (Connection connection = connectionProvider.getConnection()) {
                certificates = certificateDAO.findByStudent(connection, studentUserId);
            }
        } else {
            certificates = certificateDAO.findAll();
        }
        return Math.toIntExact(certificates.stream()
                .filter(certificate -> certificate.state() == CertificateState.ISSUED)
                .filter(certificate -> courseIds.contains(certificate.courseId()))
                // A student's context is occurrence-based.  A certificate
                // from an earlier occurrence of the same course must not
                // inflate the current personal/report indicator.
                .filter(certificate -> studentUserId == null
                        || courseOccurrenceIds.contains(certificate.courseOccurrenceId()))
                .count());
    }

    private static ScopeData scopeData(
            Collection<Course> courses,
            Collection<Subject> subjects,
            Collection<ClassGroup> classGroups,
            Long studentUserId
    ) {
        return new ScopeData(
                idsOfCourses(courses),
                idsOfSubjects(subjects),
                orderedClassGroups(classGroups),
                studentUserId
        );
    }

    private static Set<Long> idsOfCourses(Collection<Course> courses) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Course course : courses) {
            ids.add(course.id());
        }
        return Set.copyOf(ids);
    }

    private static Set<Long> idsOfSubjects(Collection<Subject> subjects) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Subject subject : subjects) {
            ids.add(subject.id());
        }
        return Set.copyOf(ids);
    }

    private static Set<Long> idsOfCourseOccurrences(Collection<ClassGroup> classGroups) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ClassGroup classGroup : classGroups) {
            ids.add(classGroup.courseOccurrenceId());
        }
        return Set.copyOf(ids);
    }

    private static List<ClassGroup> orderedClassGroups(Collection<ClassGroup> classGroups) {
        Map<Long, ClassGroup> unique = new LinkedHashMap<>();
        for (ClassGroup classGroup : classGroups) {
            unique.put(classGroup.id(), classGroup);
        }
        return List.copyOf(unique.values());
    }

    private static int sum(Map<Long, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static long requiredContextId(Long contextId) {
        if (contextId == null || contextId <= 0) {
            throw new IllegalArgumentException("Management view scope context is required");
        }
        return contextId;
    }

    private static void validateViewContext(ManagementView view) {
        Objects.requireNonNull(view, "view is required");
        if (view.ownerUserId() == null || view.ownerUserId() <= 0) {
            throw new IllegalArgumentException("Management view has no owner");
        }
        if (view.visibilityScope() == ManagementViewScope.GLOBAL) {
            if (view.scopeTargetType() != null || view.scopeTargetId() != null) {
                throw new IllegalArgumentException("Global management view cannot have a scope target");
            }
            return;
        }
        if (view.scopeTargetType() != view.visibilityScope().targetType()
                || view.scopeTargetId() == null
                || view.scopeTargetId() <= 0) {
            throw new IllegalArgumentException("Management view has an inconsistent scope target");
        }
        if (view.visibilityScope() == ManagementViewScope.PERSONAL
                && !view.ownerUserId().equals(view.scopeTargetId())) {
            throw new IllegalArgumentException("Personal management view owner must match its target");
        }
    }

    private record ScopeData(
            Set<Long> courseIds,
            Set<Long> subjectIds,
            List<ClassGroup> classGroups,
            Long studentUserId
    ) {
    }

    private record IndicatorData(
            int activeEnrollmentCount,
            int lessonCount,
            int assessmentCount,
            int submittedAttemptCount,
            int correctedAttemptCount,
            int attendanceRecordCount,
            int issuedCertificateCount
    ) {
    }
}
