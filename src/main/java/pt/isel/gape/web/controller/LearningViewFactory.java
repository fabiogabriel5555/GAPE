package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.List;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.User;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupTeacherView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentManagementView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

final class LearningViewFactory {

    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final ClassGroupDAO classGroupDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final UserDAO userDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;

    LearningViewFactory(
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO
    ) {
        this(organizationDAO, organicUnitDAO, null, subjectDAO, courseSubjectDAO, enrollmentDAO,
                null, null, null, null, null);
    }

    LearningViewFactory(
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            ClassGroupDAO classGroupDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            ContentBlockDAO contentBlockDAO,
            UserDAO userDAO,
            TeachClassGroupDAO teachClassGroupDAO
    ) {
        this.organizationDAO = organizationDAO;
        this.organicUnitDAO = organicUnitDAO;
        this.courseDAO = courseDAO;
        this.subjectDAO = subjectDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.classGroupDAO = classGroupDAO;
        this.classGroupEnrollmentDAO = classGroupEnrollmentDAO;
        this.contentBlockDAO = contentBlockDAO;
        this.userDAO = userDAO;
        this.teachClassGroupDAO = teachClassGroupDAO;
    }

    CourseView courseView(Course course) {
        return courseView(course, null);
    }

    CourseView courseView(Course course, CourseEnrollment enrollment) {
        try {
            OrganizationContext organization = organizationDAO.findById(course.organizationId())
                    .map(LearningViewFactory::organizationContext)
                    .orElse(new OrganizationContext("Unknown organization", ""));
            OrganicUnitContext organicUnit = course.organicUnitId() == null
                    ? null
                    : organicUnitDAO.findById(course.organicUnitId())
                            .map(LearningViewFactory::organicUnitContext)
                            .orElse(new OrganicUnitContext("Unknown unit", ""));
            int subjectCount = courseSubjectDAO.findByCourse(course.id()).size();
            return CourseView.from(
                    course,
                    organization.name(),
                    organization.acronym(),
                    organicUnit == null ? null : organicUnit.name(),
                    organicUnit == null ? null : organicUnit.acronym(),
                    subjectCount,
                    enrollment
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course view", exception);
        }
    }

    SubjectView subjectView(Subject subject) {
        try {
            OrganizationContext organization = organizationDAO.findById(subject.organizationId())
                    .map(LearningViewFactory::organizationContext)
                    .orElse(new OrganizationContext("Unknown organization", ""));
            return SubjectView.from(subject, organization.name(), organization.acronym());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build subject view", exception);
        }
    }

    List<CourseSubjectView> courseSubjects(long courseId, boolean activeOnly, Long studentUserId) {
        try {
            List<CourseSubjectAssociation> associations = activeOnly
                    ? courseSubjectDAO.findActiveByCourse(courseId)
                    : courseSubjectDAO.findByCourse(courseId);
            return associations.stream()
                    .map(association -> courseSubjectView(association, studentUserId))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course-subject views", exception);
        }
    }

    CourseSubjectView courseSubjectView(CourseSubjectAssociation association, Long studentUserId) {
        try {
            Subject subject = subjectDAO.findById(association.subjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + association.subjectId()));
            SubjectEnrollment enrollment = null;
            if (studentUserId != null) {
                enrollment = enrollmentDAO
                        .findSubjectEnrollment(studentUserId, association.courseId(), association.subjectId())
                        .orElse(null);
            }
            return CourseSubjectView.from(association, subjectView(subject), enrollment);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course-subject view", exception);
        }
    }

    ClassGroupView classGroupView(ClassGroup classGroup) {
        requireClassGroupSupport();
        try {
            Course course = courseDAO.findById(classGroup.courseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + classGroup.courseId()));
            Subject subject = subjectDAO.findById(classGroup.subjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + classGroup.subjectId()));
            int activeEnrollmentCount = Math.toIntExact(classGroupDAO.countActiveEnrollments(classGroup.id()));
            int blockCount = contentBlockDAO.findByClassGroup(classGroup.id()).size();
            int teacherCount = Math.toIntExact(teachClassGroupDAO.countActiveAssignments(classGroup.id()));
            return ClassGroupView.from(
                    classGroup,
                    courseView(course),
                    subjectView(subject),
                    activeEnrollmentCount,
                    blockCount,
                    teacherCount
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build class group view", exception);
        }
    }

    List<ClassGroupView> classGroupViews(List<ClassGroup> classGroups) {
        return classGroups.stream()
                .map(this::classGroupView)
                .toList();
    }

    List<ContentBlockView> contentBlockViews(long classGroupId) {
        requireClassGroupSupport();
        try {
            return contentBlockDAO.findByClassGroup(classGroupId).stream()
                    .map(ContentBlockView::from)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build content block views", exception);
        }
    }

    ContentBlockView contentBlockView(ContentBlock contentBlock) {
        return ContentBlockView.from(contentBlock);
    }

    LessonView lessonView(Lesson lesson) {
        return LessonView.from(lesson);
    }

    List<LessonView> lessonViews(List<Lesson> lessons) {
        return lessons.stream()
                .map(this::lessonView)
                .toList();
    }

    PhysicalRoomView physicalRoomView(PhysicalRoom room) {
        try {
            Organization organization = organizationDAO.findById(room.organizationId()).orElse(null);
            OrganicUnit organicUnit = room.organicUnitId() == null
                    ? null
                    : organicUnitDAO.findById(room.organicUnitId()).orElse(null);
            return PhysicalRoomView.from(room, organization, organicUnit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build physical room view", exception);
        }
    }

    List<PhysicalRoomView> physicalRoomViews(List<PhysicalRoom> rooms) {
        return rooms.stream()
                .map(this::physicalRoomView)
                .toList();
    }

    List<ClassGroupEnrollmentView> classGroupEnrollmentViews(long classGroupId) {
        requireClassGroupSupport();
        try {
            return classGroupEnrollmentDAO.findByClassGroup(classGroupId).stream()
                    .map(this::classGroupEnrollmentView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build class group enrollment views", exception);
        }
    }

    List<EnrollmentManagementView> courseEnrollmentViews(long courseId) {
        requireClassGroupSupport();
        try {
            return enrollmentDAO.findCourseEnrollmentsByCourse(courseId).stream()
                    .map(enrollment -> {
                        try {
                            User user = userDAO.findById(enrollment.studentUserId()).orElse(null);
                            return EnrollmentManagementView.course(
                                    enrollment,
                                    user == null ? "Unknown student" : user.name(),
                                    user == null ? "" : user.email()
                            );
                        } catch (SQLException exception) {
                            throw new IllegalStateException("Failed to load course enrollment user", exception);
                        }
                    })
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course enrollment views", exception);
        }
    }

    List<EnrollmentManagementView> subjectEnrollmentViews(long subjectId) {
        requireClassGroupSupport();
        try {
            return enrollmentDAO.findSubjectEnrollmentsBySubject(subjectId).stream()
                    .map(enrollment -> {
                        try {
                            User user = userDAO.findById(enrollment.studentUserId()).orElse(null);
                            Course course = courseDAO.findById(enrollment.courseId()).orElse(null);
                            return EnrollmentManagementView.subject(
                                    enrollment,
                                    user == null ? "Unknown student" : user.name(),
                                    user == null ? "" : user.email(),
                                    course == null ? "" : course.name()
                            );
                        } catch (SQLException exception) {
                            throw new IllegalStateException("Failed to load subject enrollment context", exception);
                        }
                    })
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build subject enrollment views", exception);
        }
    }

    List<ClassGroupTeacherView> classGroupTeacherViews(long classGroupId) {
        requireClassGroupSupport();
        try {
            return teachClassGroupDAO.findByClassGroup(classGroupId).stream()
                    .map(ClassGroupTeacherView::from)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build class group teacher views", exception);
        }
    }

    List<UserOptionView> activeTeacherOptions(Long selectedId) {
        requireClassGroupSupport();
        try {
            return userDAO.findActiveTeachers().stream()
                    .map(user -> UserOptionView.from(user, selectedId != null && selectedId == user.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load teacher options", exception);
        }
    }

    List<UserOptionView> activeStudentOptions(Long selectedId) {
        requireClassGroupSupport();
        try {
            return userDAO.findActiveStudents().stream()
                    .map(user -> UserOptionView.from(user, selectedId != null && selectedId == user.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load student options", exception);
        }
    }

    List<UserOptionView> activeStudentsEnrolledInCourseOptions(long courseId, Long selectedId) {
        requireClassGroupSupport();
        try {
            return userDAO.findActiveStudentsEnrolledInCourse(courseId).stream()
                    .map(user -> UserOptionView.from(user, selectedId != null && selectedId == user.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course student options", exception);
        }
    }

    List<UserOptionView> eligibleStudentOptions(long courseId, long subjectId) {
        requireClassGroupSupport();
        try {
            return userDAO.findActiveStudentsEnrolledInSubject(courseId, subjectId).stream()
                    .map(user -> UserOptionView.from(user, false))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load student options", exception);
        }
    }

    ClassGroupEnrollmentView classGroupEnrollmentView(ClassGroupEnrollment enrollment) {
        try {
            User user = userDAO.findById(enrollment.studentUserId()).orElse(null);
            return ClassGroupEnrollmentView.from(
                    enrollment,
                    user == null ? "Unknown student" : user.name(),
                    user == null ? "" : user.email()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group enrollment user", exception);
        }
    }

    private void requireClassGroupSupport() {
        if (courseDAO == null
                || classGroupDAO == null
                || classGroupEnrollmentDAO == null
                || contentBlockDAO == null
                || userDAO == null
                || teachClassGroupDAO == null) {
            throw new IllegalStateException("LearningViewFactory was not configured for class group views");
        }
    }

    private static OrganizationContext organizationContext(Organization organization) {
        return new OrganizationContext(organization.name(), organization.acronym());
    }

    private static OrganicUnitContext organicUnitContext(OrganicUnit unit) {
        return new OrganicUnitContext(unit.name(), unit.acronym());
    }

    private record OrganizationContext(String name, String acronym) {
    }

    private record OrganicUnitContext(String name, String acronym) {
    }
}
