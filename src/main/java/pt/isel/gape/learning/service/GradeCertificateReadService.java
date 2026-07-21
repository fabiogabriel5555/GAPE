package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseOccurrenceDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.GradeAccessDAO;
import pt.isel.gape.learning.dao.GradeRecordDAO;
import pt.isel.gape.learning.dao.GradeSheetDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;

public final class GradeCertificateReadService {

    private final ConnectionProvider connectionProvider;
    private final GradeSheetDAO gradeSheetDAO;
    private final GradeRecordDAO gradeRecordDAO;
    private final CertificateDAO certificateDAO;
    private final SubjectDAO subjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final AssessmentDAO assessmentDAO;
    private final CourseDAO courseDAO;
    private final CourseOccurrenceDAO courseOccurrenceDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final UserDAO userDAO;
    private final GradeAccessDAO gradeAccessDAO;
    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;

    public GradeCertificateReadService(ConnectionProvider connectionProvider) {
        this(
                connectionProvider,
                new GradeSheetDAO(connectionProvider),
                new GradeRecordDAO(connectionProvider),
                new CertificateDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new CourseOccurrenceDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new UserDAO(connectionProvider),
                new GradeAccessDAO(),
                new OrganizationDAO(connectionProvider),
                new OrganicUnitDAO(connectionProvider)
        );
    }

    public GradeCertificateReadService(
            ConnectionProvider connectionProvider,
            GradeSheetDAO gradeSheetDAO,
            GradeRecordDAO gradeRecordDAO,
            CertificateDAO certificateDAO,
            SubjectDAO subjectDAO,
            ClassGroupDAO classGroupDAO,
            AssessmentDAO assessmentDAO,
            CourseDAO courseDAO,
            CourseOccurrenceDAO courseOccurrenceDAO,
            CourseSubjectDAO courseSubjectDAO,
            UserDAO userDAO,
            GradeAccessDAO gradeAccessDAO,
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO
    ) {
        this.connectionProvider = connectionProvider;
        this.gradeSheetDAO = gradeSheetDAO;
        this.gradeRecordDAO = gradeRecordDAO;
        this.certificateDAO = certificateDAO;
        this.subjectDAO = subjectDAO;
        this.classGroupDAO = classGroupDAO;
        this.assessmentDAO = assessmentDAO;
        this.courseDAO = courseDAO;
        this.courseOccurrenceDAO = courseOccurrenceDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.userDAO = userDAO;
        this.gradeAccessDAO = gradeAccessDAO;
        this.organizationDAO = organizationDAO;
        this.organicUnitDAO = organicUnitDAO;
    }

    public List<GradeSheet> findAllGradeSheets() throws SQLException {
        return gradeSheetDAO.findAll();
    }

    public List<GradeSheet> findGradeSheetsBySubject(long subjectId) throws SQLException {
        return gradeSheetDAO.findBySubject(subjectId);
    }

    public Optional<GradeSheet> findGradeSheetById(long gradeSheetId) throws SQLException {
        return gradeSheetDAO.findById(gradeSheetId);
    }

    public List<Long> findAssessmentIdsForSheetContext(GradeSheet gradeSheet) throws SQLException {
        return gradeSheetDAO.findAssessmentIdsForSheetContext(
                gradeSheet.subjectId(),
                gradeSheet.courseOccurrenceId(),
                gradeSheet.classGroupIds()
        );
    }

    public List<Long> findStudentUserIdsForSheetContext(GradeSheet gradeSheet) throws SQLException {
        return gradeSheetDAO.findStudentUserIdsForSheetContext(gradeSheet);
    }

    public Optional<BigDecimal> findLatestCorrectedAssessmentScore(long studentUserId, long assessmentId)
            throws SQLException {
        return gradeSheetDAO.findLatestCorrectedAssessmentScore(studentUserId, assessmentId);
    }

    public boolean classGroupsEndedBefore(GradeSheet gradeSheet, LocalDate date) throws SQLException {
        return gradeSheetDAO.classGroupsEndedBefore(gradeSheet.classGroupIds(), date);
    }

    public List<GradeRecord> findGradeRecordsBySheet(long gradeSheetId) throws SQLException {
        return gradeRecordDAO.findByGradeSheet(gradeSheetId);
    }

    public Optional<GradeRecord> findActiveGradeRecord(long gradeSheetId, long studentUserId) throws SQLException {
        return gradeRecordDAO.findActiveBySheetAndStudent(gradeSheetId, studentUserId);
    }

    public List<Certificate> findAllCertificates() throws SQLException {
        return certificateDAO.findAll();
    }

    public List<Subject> findAllSubjects() throws SQLException {
        return subjectDAO.findAll();
    }

    public List<CourseSubjectAssociation> findCourseSubjects(long courseId) throws SQLException {
        return courseSubjectDAO.findByCourse(courseId);
    }

    public List<Subject> findSubjectsByCoordinator(long coordinatorUserId) throws SQLException {
        return subjectDAO.findByCoordinator(coordinatorUserId);
    }

    public List<Subject> findSubjectsByTeacher(long teacherUserId) throws SQLException {
        return subjectDAO.findByTeacher(teacherUserId);
    }

    public List<ClassGroup> findAllClassGroups() throws SQLException {
        return classGroupDAO.findAll();
    }

    public List<Assessment> findAllAssessments() throws SQLException {
        return assessmentDAO.findAll();
    }

    public List<Course> findCatalogCourses() throws SQLException {
        return courseDAO.findCatalogCourses(null, null, null);
    }

    public List<CourseOccurrence> findAllCourseOccurrences() throws SQLException {
        return courseOccurrenceDAO.findAll();
    }

    public Optional<CourseOccurrence> findCourseOccurrenceById(long courseOccurrenceId) throws SQLException {
        return courseOccurrenceDAO.findById(courseOccurrenceId);
    }

    public Optional<Course> findCourseById(long courseId) throws SQLException {
        return courseDAO.findById(courseId);
    }

    public List<User> findAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    public List<User> findActiveStudents() throws SQLException {
        return userDAO.findActiveStudents();
    }

    public List<Long> findTeacherManagedClassGroupIds(long teacherUserId) throws SQLException {
        try (var connection = connectionProvider.getConnection()) {
            return gradeAccessDAO.teacherManagedClassGroupIds(connection, teacherUserId);
        }
    }

    public List<Long> findCoordinatorManagedClassGroupIds(long coordinatorUserId) throws SQLException {
        try (var connection = connectionProvider.getConnection()) {
            return gradeAccessDAO.coordinatorManagedClassGroupIds(connection, coordinatorUserId);
        }
    }

    public List<Long> findActiveStudentIdsInClassGroups(List<Long> classGroupIds) throws SQLException {
        try (var connection = connectionProvider.getConnection()) {
            return gradeAccessDAO.activeStudentIdsInClassGroups(connection, classGroupIds);
        }
    }

    public List<Long> findApplicableClassGroupIds(long assessmentId) throws SQLException {
        return assessmentDAO.findApplicableClassGroupIds(assessmentId);
    }

    public List<Organization> findActiveOrganizations() throws SQLException {
        return organizationDAO.findActive();
    }

    public List<OrganicUnit> findOrganicUnitsByOrganization(long organizationId) throws SQLException {
        return organicUnitDAO.findByOrganization(organizationId);
    }
}
