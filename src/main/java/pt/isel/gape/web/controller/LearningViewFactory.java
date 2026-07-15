package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.isel.gape.access.model.User;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceContext;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupTeacherView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.CourseOccurrenceView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentManagementView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

final class LearningViewFactory {

    private final ApplicationReadService.Organizations organizationDAO;
    private final ApplicationReadService.OrganicUnits organicUnitDAO;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.CourseOccurrences courseOccurrenceDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.Enrollments enrollmentDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO;
    private final ApplicationReadService.ContentBlocks contentBlockDAO;
    private final ApplicationReadService.Users userDAO;
    private final ApplicationReadService.TeachClassGroups teachClassGroupDAO;

    LearningViewFactory(
            ApplicationReadService.Organizations organizationDAO,
            ApplicationReadService.OrganicUnits organicUnitDAO,
            ApplicationReadService.Subjects subjectDAO,
            ApplicationReadService.CourseSubjects courseSubjectDAO,
            ApplicationReadService.Enrollments enrollmentDAO
    ) {
        this(organizationDAO, organicUnitDAO, null, null, subjectDAO, courseSubjectDAO, enrollmentDAO,
                null, null, null, null, null);
    }

    LearningViewFactory(
            ApplicationReadService.Organizations organizationDAO,
            ApplicationReadService.OrganicUnits organicUnitDAO,
            ApplicationReadService.Courses courseDAO,
            ApplicationReadService.CourseOccurrences courseOccurrenceDAO,
            ApplicationReadService.Subjects subjectDAO,
            ApplicationReadService.CourseSubjects courseSubjectDAO,
            ApplicationReadService.Enrollments enrollmentDAO,
            ApplicationReadService.ClassGroups classGroupDAO,
            ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO,
            ApplicationReadService.ContentBlocks contentBlockDAO,
            ApplicationReadService.Users userDAO,
            ApplicationReadService.TeachClassGroups teachClassGroupDAO
    ) {
        this.organizationDAO = organizationDAO;
        this.organicUnitDAO = organicUnitDAO;
        this.courseDAO = courseDAO;
        this.courseOccurrenceDAO = courseOccurrenceDAO;
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

    List<CourseSubjectView> courseSubjects(long courseId, Long studentUserId) {
        try {
            List<CourseSubjectAssociation> associations = courseSubjectDAO.findByCourse(courseId);
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
            return CourseSubjectView.from(association, subjectView(subject));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course-subject view", exception);
        }
    }

    List<CourseOccurrenceView> courseOccurrenceViews(long courseId) {
        requireCourseOccurrenceSupport();
        try {
            return courseOccurrenceDAO.findByCourse(courseId).stream()
                    .map(occurrence -> {
                        try {
                            return CourseOccurrenceView.from(
                                    occurrence,
                                    courseOccurrenceDAO.findPeriodsByOccurrence(occurrence.id())
                            );
                        } catch (SQLException exception) {
                            throw new IllegalStateException("Failed to load course occurrence periods", exception);
                        }
                    })
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course occurrence views", exception);
        }
    }

    List<CourseOccurrence> courseOccurrenceSummary(long courseId) {
        requireCourseOccurrenceSupport();
        try {
            return courseOccurrenceDAO.findByCourse(courseId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course occurrence summary", exception);
        }
    }

    ClassGroupView classGroupView(ClassGroup classGroup) {
        return classGroupViews(List.of(classGroup)).get(0);
    }

    List<ClassGroupView> classGroupViews(List<ClassGroup> classGroups) {
        requireClassGroupSupport();
        if (classGroups.isEmpty()) {
            return List.of();
        }
        try {
            List<Long> classGroupIds = classGroups.stream().map(ClassGroup::id).toList();
            Map<Long, Integer> activeEnrollmentCounts =
                    classGroupDAO.countActiveEnrollmentsByClassGroupIds(classGroupIds);
            Map<Long, Integer> blockCounts = contentBlockDAO.countByClassGroupIds(classGroupIds);
            Map<Long, Integer> teacherCounts = teachClassGroupDAO.countActiveAssignmentsByClassGroupIds(classGroupIds);
            Map<Long, CourseView> courseViews = new HashMap<>();
            Map<Long, SubjectView> subjectViews = new HashMap<>();
            Map<Long, CourseOccurrenceContext> occurrenceContexts = new HashMap<>();
            return classGroups.stream()
                    .map(classGroup -> {
                        long occurrencePeriodId = classGroup.courseOccurrencePeriodId();
                        CourseOccurrenceContext context = occurrenceContexts.get(occurrencePeriodId);
                        if (context == null && !occurrenceContexts.containsKey(occurrencePeriodId)) {
                            context = occurrenceContext(classGroup);
                            occurrenceContexts.put(occurrencePeriodId, context);
                        }
                        return ClassGroupView.from(
                                classGroup,
                                courseViews.computeIfAbsent(classGroup.courseId(), courseId -> courseView(loadCourse(courseId))),
                                subjectViews.computeIfAbsent(classGroup.subjectId(), subjectId -> subjectView(loadSubject(subjectId))),
                                activeEnrollmentCounts.getOrDefault(classGroup.id(), 0),
                                blockCounts.getOrDefault(classGroup.id(), 0),
                                teacherCounts.getOrDefault(classGroup.id(), 0),
                                occurrenceLabel(classGroup, context),
                                occurrenceDateRangeLabel(context),
                                occurrenceStateLabel(context),
                                occurrenceStateBadgeClass(context),
                                occurrencePeriodLabel(context),
                                occurrencePeriodDateRangeLabel(context),
                                occurrencePeriodStateLabel(context),
                                occurrencePeriodStateBadgeClass(context)
                        );
                    })
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build class group views", exception);
        }
    }

    private Course loadCourse(long courseId) {
        try {
            return courseDAO.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course", exception);
        }
    }

    private Subject loadSubject(long subjectId) {
        try {
            return subjectDAO.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject", exception);
        }
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
                            CourseOccurrence occurrence = courseOccurrenceDAO
                                    .findById(enrollment.courseOccurrenceId())
                                    .orElse(null);
                            return EnrollmentManagementView.course(
                                    effectiveCourseEnrollmentState(enrollment, occurrence),
                                    user == null ? "Unknown student" : user.name(),
                                    user == null ? "" : user.email(),
                                    occurrence == null ? "" : occurrence.code()
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

    private static CourseEnrollment effectiveCourseEnrollmentState(
            CourseEnrollment enrollment,
            CourseOccurrence occurrence
    ) {
        if (occurrence == null
                || occurrence.state() != CourseOccurrenceState.COMPLETED
                || enrollment.state() == EnrollmentState.WITHDRAWN) {
            return enrollment;
        }
        return new CourseEnrollment(
                enrollment.studentUserId(),
                enrollment.courseId(),
                enrollment.courseOccurrenceId(),
                EnrollmentState.COMPLETED,
                occurrence.startsAt(),
                occurrence.endsAt()
        );
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
            return userDAO.findActiveStudentsWithCurricularSubjectAccess(courseId, subjectId).stream()
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
                || courseOccurrenceDAO == null
                || classGroupDAO == null
                || classGroupEnrollmentDAO == null
                || contentBlockDAO == null
                || userDAO == null
                || teachClassGroupDAO == null) {
            throw new IllegalStateException("LearningViewFactory was not configured for class group views");
        }
    }

    private void requireCourseOccurrenceSupport() {
        if (courseOccurrenceDAO == null) {
            throw new IllegalStateException("LearningViewFactory was not configured for course occurrence views");
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

    private CourseOccurrenceContext occurrenceContext(ClassGroup classGroup) {
        try {
            return courseOccurrenceDAO.findContextByPeriodId(classGroup.courseOccurrencePeriodId()).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course occurrence context", exception);
        }
    }

    private static String occurrenceLabel(ClassGroup classGroup, CourseOccurrenceContext context) {
        return context == null ? "Occurrence " + classGroup.courseOccurrenceId() : context.occurrence().label();
    }

    private static String occurrenceDateRangeLabel(CourseOccurrenceContext context) {
        if (context == null) {
            return "-";
        }
        return (context.occurrence().startsAt() == null
                ? "-"
                : ApplicationDateTimeFormat.date(context.occurrence().startsAt()))
                + " to "
                + (context.occurrence().endsAt() == null
                ? "-"
                : ApplicationDateTimeFormat.date(context.occurrence().endsAt()));
    }

    private static String occurrenceStateLabel(CourseOccurrenceContext context) {
        if (context == null) {
            return "-";
        }
        return switch (context.occurrence().state()) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String occurrenceStateBadgeClass(CourseOccurrenceContext context) {
        if (context == null) {
            return "bg-neutral-30 text-neutral-600";
        }
        return switch (context.occurrence().state()) {
            case DRAFT -> "bg-warning-50 text-warning-600";
            case SCHEDULED -> "bg-warning-50 text-warning-700";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-neutral-20 text-neutral-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    private static String occurrencePeriodLabel(CourseOccurrenceContext context) {
        if (context == null) {
            return "-";
        }
        return context.period().term().labelForCourseYear(context.period().curricularYear());
    }

    private static String occurrencePeriodDateRangeLabel(CourseOccurrenceContext context) {
        if (context == null) {
            return "-";
        }
        return (context.period().startsAt() == null
                ? "-"
                : ApplicationDateTimeFormat.date(context.period().startsAt()))
                + " to "
                + (context.period().endsAt() == null
                ? "-"
                : ApplicationDateTimeFormat.date(context.period().endsAt()));
    }

    private static String occurrencePeriodStateLabel(CourseOccurrenceContext context) {
        return context == null ? "-" : occurrenceStateLabel(context.period().state());
    }

    private static String occurrencePeriodStateBadgeClass(CourseOccurrenceContext context) {
        return context == null
                ? "bg-neutral-30 text-neutral-600"
                : occurrenceStateBadgeClass(context.period().state());
    }

    private static String occurrenceStateLabel(CourseOccurrenceState state) {
        return switch (state) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String occurrenceStateBadgeClass(CourseOccurrenceState state) {
        return switch (state) {
            case DRAFT -> "bg-warning-50 text-warning-600";
            case SCHEDULED -> "bg-warning-50 text-warning-700";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-neutral-20 text-neutral-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    private static String ordinalSuffix(int value) {
        int rem100 = value % 100;
        if (rem100 >= 11 && rem100 <= 13) {
            return "th";
        }
        return switch (value % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
