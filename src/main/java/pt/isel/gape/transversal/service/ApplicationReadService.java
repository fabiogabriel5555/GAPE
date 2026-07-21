package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.AttendanceRecordDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.ContentItemDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseOccurrenceDAO;
import pt.isel.gape.learning.dao.CoursePeriodTemplateDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentApprovalPolicyDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.dao.LearningEventDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.PhysicalRoomDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceContext;
import pt.isel.gape.learning.model.CourseOccurrencePeriod;
import pt.isel.gape.learning.model.CoursePeriodTemplate;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.LearningEvent;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.ClassGroupContext;
import pt.isel.gape.structure.model.CoordinateSubjectAssignment;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.TeacherClassGroupAssignment;

/**
 * Read-only application boundary used by the web layer.
 *
 * <p>Servlets depend on these query ports rather than concrete DAO classes.
 * The DAO implementations remain private to this service composition root.</p>
 */
public final class ApplicationReadService {

    private final ConnectionProvider connectionProvider;
    private final Assessments assessments;
    private final AssessmentEnrollments assessmentEnrollments;
    private final Attempts attempts;
    private final AttendanceRecords attendanceRecords;
    private final Certificates certificates;
    private final ClassGroups classGroups;
    private final ClassGroupEnrollments classGroupEnrollments;
    private final ContentBlocks contentBlocks;
    private final ContentItems contentItems;
    private final Courses courses;
    private final CourseOccurrences courseOccurrences;
    private final CoursePeriodTemplates coursePeriodTemplates;
    private final CourseSubjects courseSubjects;
    private final Enrollments enrollments;
    private final GradeSheets gradeSheets;
    private final LearningEvents learningEvents;
    private final Lessons lessons;
    private final PhysicalRooms physicalRooms;
    private final Questions questions;
    private final QuestionOptions questionOptions;
    private final Responses responses;
    private final Subjects subjects;
    private final EnrollmentApprovalPolicies enrollmentApprovalPolicies;
    private final Organizations organizations;
    private final OrganicUnits organicUnits;
    private final CoordinateSubjects coordinateSubjects;
    private final TeachClassGroups teachClassGroups;
    private final ManageOrganizations manageOrganizations;
    private final Users users;
    private final Permissions permissions;

    public ApplicationReadService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.assessments = new AssessmentDAO(connectionProvider);
        this.assessmentEnrollments = new AssessmentEnrollmentDAO(connectionProvider);
        this.attempts = new AttemptDAO(connectionProvider);
        this.attendanceRecords = new AttendanceRecordDAO(connectionProvider);
        this.certificates = new CertificateDAO(connectionProvider);
        this.classGroups = new ClassGroupDAO(connectionProvider);
        this.classGroupEnrollments = new ClassGroupEnrollmentDAO(connectionProvider);
        this.contentBlocks = new ContentBlockDAO(connectionProvider);
        this.contentItems = new ContentItemDAO(connectionProvider);
        this.courses = new CourseDAO(connectionProvider);
        this.courseOccurrences = new CourseOccurrenceDAO(connectionProvider);
        this.coursePeriodTemplates = new CoursePeriodTemplateDAO(connectionProvider);
        this.courseSubjects = new CourseSubjectDAO(connectionProvider);
        this.enrollments = new EnrollmentDAO(connectionProvider);
        this.gradeSheets = new GradeSheetDAO(connectionProvider);
        this.learningEvents = new LearningEventDAO(connectionProvider);
        this.lessons = new LessonDAO(connectionProvider);
        this.physicalRooms = new PhysicalRoomDAO(connectionProvider);
        this.questions = new QuestionDAO(connectionProvider);
        this.questionOptions = new QuestionOptionDAO(connectionProvider);
        this.responses = new ResponseDAO(connectionProvider);
        this.subjects = new SubjectDAO(connectionProvider);
        this.enrollmentApprovalPolicies = new EnrollmentApprovalPolicyDAO(connectionProvider);
        this.organizations = new OrganizationDAO(connectionProvider);
        this.organicUnits = new OrganicUnitDAO(connectionProvider);
        this.coordinateSubjects = new CoordinateSubjectDAO(connectionProvider);
        this.teachClassGroups = new TeachClassGroupDAO(connectionProvider);
        this.manageOrganizations = new ManageOrganizationDAO(connectionProvider);
        this.users = new UserDAO(connectionProvider);
        this.permissions = new PermissionDAO(connectionProvider);
    }

    public Assessments assessments() { return assessments; }
    public AssessmentEnrollments assessmentEnrollments() { return assessmentEnrollments; }
    public Attempts attempts() { return attempts; }
    public Certificates certificates() { return certificates; }
    public ClassGroups classGroups() { return classGroups; }
    public ClassGroupEnrollments classGroupEnrollments() { return classGroupEnrollments; }
    public ContentBlocks contentBlocks() { return contentBlocks; }
    public ContentItems contentItems() { return contentItems; }
    public Courses courses() { return courses; }
    public CourseOccurrences courseOccurrences() { return courseOccurrences; }
    public CoursePeriodTemplates coursePeriodTemplates() { return coursePeriodTemplates; }
    public CourseSubjects courseSubjects() { return courseSubjects; }
    public Enrollments enrollments() { return enrollments; }
    public GradeSheets gradeSheets() { return gradeSheets; }
    public LearningEvents learningEvents() { return learningEvents; }
    public Lessons lessons() { return lessons; }
    public PhysicalRooms physicalRooms() { return physicalRooms; }
    public Questions questions() { return questions; }
    public QuestionOptions questionOptions() { return questionOptions; }
    public Responses responses() { return responses; }
    public Subjects subjects() { return subjects; }
    public EnrollmentApprovalPolicies enrollmentApprovalPolicies() { return enrollmentApprovalPolicies; }
    public Organizations organizations() { return organizations; }
    public OrganicUnits organicUnits() { return organicUnits; }
    public CoordinateSubjects coordinateSubjects() { return coordinateSubjects; }
    public TeachClassGroups teachClassGroups() { return teachClassGroups; }
    public ManageOrganizations manageOrganizations() { return manageOrganizations; }
    public Users users() { return users; }
    public Permissions permissions() { return permissions; }

    public List<AttendanceRecord> findVisibleAttendanceRecords(AccessProfileType profile, long actorUserId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return switch (profile) {
                case ADMINISTRATOR -> attendanceRecords.findVisibleForAdministrator(connection, actorUserId);
                case COORDINATOR -> attendanceRecords.findVisibleForCoordinator(connection, actorUserId);
                case TEACHER -> attendanceRecords.findVisibleForTeacher(connection, actorUserId);
                default -> List.of();
            };
        }
    }

    public List<Lesson> findLessonsByIds(Collection<Long> lessonIds) throws SQLException {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return List.of();
        }
        try (Connection connection = connectionProvider.getConnection()) {
            return lessons.findByIds(connection, lessonIds);
        }
    }

    public List<Certificate> findCertificatesByStudent(long studentUserId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return certificates.findByStudent(connection, studentUserId);
        }
    }

    public interface Assessments {
        Optional<Assessment> findById(long assessmentId) throws SQLException;
        List<Assessment> findAll() throws SQLException;
        List<Assessment> findByContentBlock(long contentBlockId) throws SQLException;
        List<Assessment> findByClassGroup(long classGroupId) throws SQLException;
        Map<Long, Integer> countByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
        Map<Long, Integer> countSubmittedAttemptsByClassGroupIds(Collection<Long> classGroupIds)
                throws SQLException;
        int countDistinctSubmittedAttemptsByClassGroupIds(Collection<Long> classGroupIds)
                throws SQLException;
        List<Assessment> findActiveAccessibleByStudent(long studentUserId) throws SQLException;
        boolean hasAnyAttempts(long assessmentId) throws SQLException;
        List<Long> findApplicableClassGroupIds(long assessmentId) throws SQLException;
        Map<Long, List<Long>> findApplicableClassGroupIdsByAssessmentIds(Collection<Long> assessmentIds)
                throws SQLException;
        boolean hasCurrentStudentAccess(long studentUserId, long assessmentId) throws SQLException;
        AssessmentDAO.ClassGroupAssessmentWeightSummary summarizeAssessmentWeightsForClassGroup(long classGroupId)
                throws SQLException;
    }

    public interface AssessmentEnrollments {
        int countByAssessmentAndState(long assessmentId, EnrollmentState state) throws SQLException;
        Map<Long, Integer> countByAssessmentIdsAndState(Collection<Long> assessmentIds, EnrollmentState state)
                throws SQLException;
        List<AssessmentEnrollment> findByAssessment(long assessmentId) throws SQLException;
        List<Long> findEligibleStudentIds(long assessmentId) throws SQLException;
        Optional<AssessmentEnrollment> findEnrollment(long studentUserId, long assessmentId) throws SQLException;
        int syncAutomaticEnrollments(long assessmentId) throws SQLException;
        int syncAllAutomaticEnrollments() throws SQLException;
    }

    public interface Attempts {
        Optional<Attempt> findById(long attemptId) throws SQLException;
        List<Attempt> findByAssessment(long assessmentId) throws SQLException;
        List<Attempt> findByStudent(long studentUserId) throws SQLException;
        Optional<Attempt> findLatestInProgress(long studentUserId, long assessmentId) throws SQLException;
        int countByAssessment(long assessmentId) throws SQLException;
        int countByAssessmentAndState(long assessmentId, AttemptState state) throws SQLException;
        Map<Long, Integer> countByAssessmentIdsAndState(Collection<Long> assessmentIds, AttemptState state)
                throws SQLException;
    }

    public interface AttendanceRecords {
        List<AttendanceRecord> findVisibleForTeacher(Connection connection, long teacherUserId) throws SQLException;
        List<AttendanceRecord> findVisibleForCoordinator(Connection connection, long coordinatorUserId) throws SQLException;
        List<AttendanceRecord> findVisibleForAdministrator(Connection connection, long administratorUserId)
                throws SQLException;
    }

    public interface Certificates {
        List<Certificate> findAll() throws SQLException;
        List<Certificate> findByStudent(Connection connection, long studentUserId) throws SQLException;
    }

    public interface ClassGroups {
        Optional<ClassGroup> findById(long classGroupId) throws SQLException;
        List<ClassGroup> findAll() throws SQLException;
        List<ClassGroup> findByCourse(long courseId) throws SQLException;
        List<ClassGroup> findBySubject(long subjectId) throws SQLException;
        List<ClassGroup> findByCourseAndSubject(long courseId, long subjectId) throws SQLException;
        Map<Long, Integer> countActiveEnrollmentsByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
    }

    public interface ClassGroupEnrollments {
        Optional<ClassGroupEnrollment> findEnrollment(long studentUserId, long classGroupId) throws SQLException;
        List<ClassGroupEnrollment> findByStudent(long studentUserId) throws SQLException;
        List<ClassGroupEnrollment> findByClassGroup(long classGroupId) throws SQLException;
        List<ClassGroupEnrollment> findByClassGroups(Collection<Long> classGroupIds) throws SQLException;
        Map<Long, Integer> countPendingByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
    }

    public interface ContentBlocks {
        Optional<ContentBlock> findById(long contentBlockId) throws SQLException;
        List<ContentBlock> findByClassGroup(long classGroupId) throws SQLException;
        Map<Long, Integer> countByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
    }

    public interface ContentItems {
        boolean hasActiveAssessmentRepositoryReference(long assessmentId) throws SQLException;
    }

    public interface Courses {
        Optional<Course> findById(long courseId) throws SQLException;
        List<Course> findAll() throws SQLException;
        List<Course> findByIds(Collection<Long> courseIds) throws SQLException;
        List<Course> findByOrganization(long organizationId) throws SQLException;
        List<Course> findCatalogCourses(Long organizationId, CourseType type, String query) throws SQLException;
        Optional<Course> findActiveById(long courseId) throws SQLException;
    }

    public interface CourseOccurrences {
        Optional<CourseOccurrence> findById(long courseOccurrenceId) throws SQLException;
        List<CourseOccurrence> findAll() throws SQLException;
        List<CourseOccurrence> findByCourse(long courseId) throws SQLException;
        List<CourseOccurrencePeriod> findPeriodsByOccurrence(long courseOccurrenceId) throws SQLException;
        Optional<CourseOccurrenceContext> findContextByPeriodId(long courseOccurrencePeriodId) throws SQLException;
    }

    public interface CoursePeriodTemplates {
        List<CoursePeriodTemplate> findByCourse(long courseId) throws SQLException;
    }

    public interface CourseSubjects {
        Optional<CourseSubjectAssociation> findByCourseAndSubject(long courseId, long subjectId) throws SQLException;
        List<CourseSubjectAssociation> findByCourse(long courseId) throws SQLException;
        List<CourseSubjectAssociation> findBySubject(long subjectId) throws SQLException;
    }

    public interface Enrollments {
        Optional<pt.isel.gape.learning.model.CourseEnrollment> findCourseEnrollment(long studentUserId, long courseId)
                throws SQLException;
        List<pt.isel.gape.learning.model.CourseEnrollment> findCourseEnrollmentsByCourse(long courseId)
                throws SQLException;
        List<pt.isel.gape.learning.model.CourseEnrollment> findCourseEnrollmentsByStudent(long studentUserId)
                throws SQLException;
        long countActiveCourseEnrollments(long courseId) throws SQLException;
        Set<Long> findActiveCourseIdsByStudent(long studentUserId) throws SQLException;
    }

    public interface GradeSheets {
        List<GradeSheet> findAll() throws SQLException;
    }

    public interface LearningEvents {
        int countVisible(
                Collection<Long> visibleClassGroupIds,
                Collection<Long> visibleCourseIds,
                Collection<Long> visibleSubjectIds,
                Long studentUserId,
                boolean studentProfile,
                boolean administratorProfile,
                String category,
                Long classGroupFilter,
                Long courseFilter,
                Long subjectFilter,
                LocalDateTime now
        ) throws SQLException;
        int countUnreadVisible(
                Collection<Long> visibleClassGroupIds,
                Collection<Long> visibleCourseIds,
                Collection<Long> visibleSubjectIds,
                Long studentUserId,
                boolean studentProfile,
                boolean administratorProfile,
                String category,
                Long classGroupFilter,
                Long courseFilter,
                Long subjectFilter,
                LocalDateTime now,
                long viewerUserId
        ) throws SQLException;
        List<LearningEvent> findVisible(
                Collection<Long> visibleClassGroupIds,
                Collection<Long> visibleCourseIds,
                Collection<Long> visibleSubjectIds,
                Long studentUserId,
                boolean studentProfile,
                boolean administratorProfile,
                String category,
                Long classGroupFilter,
                Long courseFilter,
                Long subjectFilter,
                LocalDateTime now,
                long viewerUserId,
                int limit,
                int offset
        ) throws SQLException;
        Optional<LearningEvent> findVisibleById(
                Collection<Long> visibleClassGroupIds,
                Collection<Long> visibleCourseIds,
                Collection<Long> visibleSubjectIds,
                Long studentUserId,
                boolean studentProfile,
                boolean administratorProfile,
                LocalDateTime now,
                long viewerUserId,
                long eventId
        ) throws SQLException;
        void markReadById(long viewerUserId, long eventId, LocalDateTime readAt) throws SQLException;
        void markReadByHref(long viewerUserId, String href, boolean includeAnchoredChildren, LocalDateTime readAt)
                throws SQLException;
        void rebuildFromCurrentRecords() throws SQLException;
    }

    public interface Lessons {
        Optional<Lesson> findById(long lessonId) throws SQLException;
        List<Lesson> findByIds(Connection connection, Collection<Long> lessonIds) throws SQLException;
        Map<Long, Integer> countByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
        List<Lesson> findByRoom(String physicalRoomCode) throws SQLException;
    }

    public interface PhysicalRooms {
        Optional<PhysicalRoom> findByCode(String code) throws SQLException;
        List<PhysicalRoom> findByOrganization(long organizationId) throws SQLException;
    }

    public interface Questions {
        Optional<Question> findById(long questionId) throws SQLException;
        List<Question> findByAssessment(long assessmentId) throws SQLException;
        int countActiveByAssessment(long assessmentId) throws SQLException;
    }

    public interface QuestionOptions {
        Optional<QuestionOption> findById(long optionId) throws SQLException;
        List<QuestionOption> findByQuestion(long questionId) throws SQLException;
        List<QuestionOption> findActiveByQuestion(long questionId) throws SQLException;
        List<QuestionOption> findSelectedOptions(long responseId) throws SQLException;
    }

    public interface Responses {
        Optional<Response> findById(long responseId) throws SQLException;
        List<Response> findByAttempt(long attemptId) throws SQLException;
    }

    public interface Subjects {
        Optional<Subject> findById(long subjectId) throws SQLException;
        List<Subject> findAll() throws SQLException;
        List<Subject> findByTeacher(long teacherUserId) throws SQLException;
    }

    public interface EnrollmentApprovalPolicies {
        EnrollmentApprovalMode classGroupMode(long classGroupId) throws SQLException;
    }

    public interface Organizations {
        Optional<Organization> findById(long organizationId) throws SQLException;
        List<Organization> findActive() throws SQLException;
        List<Organization> findByAdministrator(long administratorUserId) throws SQLException;
    }

    public interface OrganicUnits {
        Optional<OrganicUnit> findById(long organicUnitId) throws SQLException;
        List<OrganicUnit> findByOrganization(long organizationId) throws SQLException;
    }

    public interface CoordinateSubjects {
        List<CoordinateSubjectAssignment> findBySubject(long subjectId) throws SQLException;
        Set<Long> findActiveSubjectIdsByCoordinator(long coordinatorUserId) throws SQLException;
    }

    public interface TeachClassGroups {
        Map<Long, Integer> countActiveAssignmentsByClassGroupIds(Collection<Long> classGroupIds) throws SQLException;
        List<TeacherClassGroupAssignment> findByClassGroup(long classGroupId) throws SQLException;
        Set<Long> findActiveClassGroupIdsByTeacher(long teacherUserId) throws SQLException;
        List<ClassGroupContext> findActiveByOrganizations(Collection<Long> organizationIds) throws SQLException;
        ClassGroupContext requireContext(long classGroupId) throws SQLException;
    }

    public interface ManageOrganizations {
        Set<Long> findActiveOrganizationIdsByAdministrator(long administratorUserId) throws SQLException;
    }

    public interface Users {
        Optional<User> findById(long userId) throws SQLException;
        List<User> findAll() throws SQLException;
        List<User> findActiveTeachers() throws SQLException;
        List<User> findActiveStudents() throws SQLException;
        List<User> findActiveStudentsEnrolledInCourse(long courseId) throws SQLException;
        List<User> findActiveStudentsWithCurricularSubjectAccess(long courseId, long subjectId) throws SQLException;
    }

    public interface Permissions {
        Set<String> findActivePermissionCodes(long userId, AccessProfileType profileType) throws SQLException;
        Set<AdministratorPermissionAssignment> findActiveAdministratorAssignments(long administratorUserId)
                throws SQLException;
    }
}
