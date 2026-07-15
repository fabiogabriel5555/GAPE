package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.ScheduleEventService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.ScheduleEventView;

@WebServlet(name = "studentLessonServlet", urlPatterns = {
        "/student/lessons",
        "/student/lessons/*",
        "/student/calendar",
        "/student/events",
        "/student/events/*"
})
public final class StudentLessonServlet extends DashboardServletSupport {

    private static final String STUDENT_LESSON_LIST_JSP = "/student/student/lesson/student-lessons.jsp";
    private static final String STUDENT_CALENDAR_JSP = "/student/student/calendar/student-calendar.jsp";

    private final LessonService lessonService;
    private final ScheduleEventService scheduleEventService;
    private final ApplicationReadService.LearningEvents learningEvents;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.ContentBlocks contentBlockDAO;
    private final LearningViewFactory viewFactory;
    private final ScheduleAttendanceViewFactory scheduleViewFactory;

    public StudentLessonServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private StudentLessonServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new LessonService(connectionProvider, clock),
                new ScheduleEventService(connectionProvider, clock)
        );
    }

    StudentLessonServlet(
            ApplicationReadService readService,
            LessonService lessonService,
            ScheduleEventService scheduleEventService
    ) {
        this.lessonService = lessonService;
        this.scheduleEventService = scheduleEventService;
        this.learningEvents = readService.learningEvents();
        this.classGroupDAO = readService.classGroups();
        this.contentBlockDAO = readService.contentBlocks();
        this.viewFactory = new LearningViewFactory(
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
        this.scheduleViewFactory = new ScheduleAttendanceViewFactory(
                readService.classGroups(),
                readService.lessons(),
                readService.users(),
                viewFactory
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (isCalendarRequest(request)) {
                if (segments.length == 0) {
                    showList(request, response, true);
                    return;
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (segments.length == 0) {
                showList(request, response, false);
                return;
            }
            if (segments.length == 2 && "access".equals(segments[1])) {
                openMeetingAccess(request, response, Long.parseLong(segments[0]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (isCalendarRequest(request) && segments.length == 1 && "read-by-href".equals(segments[0])) {
            markEventReadByHref(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void markEventReadByHref(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        String href = text(request, "href");
        if (!isSafeReturnPath(href)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            learningEvents.markReadByHref(
                    actor.userId(),
                    href,
                    false,
                    LocalDateTime.now(ApplicationClock.system())
            );
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":true}");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark student learning event as read", exception);
        }
    }

    private void openMeetingAccess(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        Lesson lesson = lessonService.getLesson(
                actor.userId(),
                currentSessionId(request),
                AccessProfileType.STUDENT,
                lessonId,
                request.getRemoteAddr()
        );
        if (lesson.accessUrl() == null || lesson.accessUrl().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.sendRedirect(lesson.accessUrl());
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, boolean calendarMode)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        List<LessonView> lessons = lessonService.listPersonalCalendarLessons(
                        actor.userId(),
                        currentSessionId(request),
                        AccessProfileType.STUDENT,
                        request.getRemoteAddr()
                )
                .stream()
                .sorted(Comparator.comparing(Lesson::startsAt).thenComparingLong(Lesson::id))
                .map(viewFactory::lessonView)
                .toList();
        List<ScheduleEventView> scheduleEvents = calendarMode
                ? scheduleEventService.listVisibleEvents(
                                actor.userId(),
                                currentSessionId(request),
                                AccessProfileType.STUDENT,
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(scheduleViewFactory::scheduleEventView)
                        .filter(event -> event.isLessonEvent() || event.isAssessmentEvent())
                        .toList()
                : List.of();
        Map<Long, ClassGroupView> classGroupById = classGroupMap(lessons);
        Map<Long, ContentBlockView> contentBlockById = contentBlockMap(lessons);
        List<StudentCalendarItemView> calendarItems = calendarMode
                ? calendarItems(lessons, scheduleEvents)
                : List.of();
        request.setAttribute("lessons", lessons);
        request.setAttribute("scheduleEvents", scheduleEvents);
        request.setAttribute("calendarItems", calendarItems);
        request.setAttribute("scheduleEventCount", scheduleEvents.size());
        request.setAttribute("calendarItemCount", lessons.size() + scheduleEvents.size());
        request.setAttribute("classGroupById", classGroupById);
        request.setAttribute("contentBlockById", contentBlockById);
        request.setAttribute("lessonCourseGroups", lessonCourseGroups(lessons, classGroupById));
        request.setAttribute("calendarMode", calendarMode);
        prepareDashboard(request, calendarMode ? "calendar" : "lessons", calendarMode ? "Events" : "Lessons");
        forward(request, response, calendarMode ? STUDENT_CALENDAR_JSP : STUDENT_LESSON_LIST_JSP);
    }

    private static boolean isCalendarRequest(HttpServletRequest request) {
        return "/student/calendar".equals(request.getServletPath())
                || "/student/events".equals(request.getServletPath());
    }

    private static List<StudentCalendarItemView> calendarItems(
            List<LessonView> lessons,
            List<ScheduleEventView> scheduleEvents
    ) {
        List<StudentCalendarItemView> items = new ArrayList<>();
        for (ScheduleEventView event : scheduleEvents) {
            items.add(StudentCalendarItemView.event(event));
        }
        for (LessonView lesson : lessons) {
            items.add(StudentCalendarItemView.lesson(lesson));
        }
        return items.stream()
                .sorted(Comparator
                        .comparing(StudentCalendarItemView::getStartsAtRaw)
                        .thenComparing(StudentCalendarItemView::getSortOrder)
                        .thenComparingLong(StudentCalendarItemView::getId))
                .toList();
    }

    private Map<Long, ClassGroupView> classGroupMap(List<LessonView> lessons) {
        Map<Long, ClassGroupView> result = new HashMap<>();
        for (LessonView lesson : lessons) {
            result.computeIfAbsent(lesson.getClassGroupId(), this::classGroupView);
        }
        return result;
    }

    private Map<Long, ContentBlockView> contentBlockMap(List<LessonView> lessons) {
        Map<Long, ContentBlockView> result = new HashMap<>();
        for (LessonView lesson : lessons) {
            result.computeIfAbsent(lesson.getContentBlockId(), this::contentBlockView);
        }
        return result;
    }

    private List<StudentLessonCourseGroupView> lessonCourseGroups(
            List<LessonView> lessons,
            Map<Long, ClassGroupView> classGroupById
    ) {
        Map<Long, StudentLessonCourseGroupView> courseGroups = new LinkedHashMap<>();
        Map<String, StudentLessonSubjectGroupView> subjectGroups = new LinkedHashMap<>();
        lessons.stream()
                .sorted(Comparator
                        .comparing((LessonView lesson) -> courseName(lesson, classGroupById), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(lesson -> subjectName(lesson, classGroupById), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(LessonView::getStartsAtRaw)
                        .thenComparingLong(LessonView::getId))
                .forEach(lesson -> {
                    ClassGroupView classGroup = classGroupById.get(lesson.getClassGroupId());
                    long courseId = classGroup == null ? 0L : classGroup.getCourseId();
                    long subjectId = classGroup == null ? 0L : classGroup.getSubjectId();
                    StudentLessonCourseGroupView courseGroup = courseGroups.computeIfAbsent(
                            courseId,
                            id -> new StudentLessonCourseGroupView(id, courseName(lesson, classGroupById))
                    );
                    String subjectKey = courseId + ":" + subjectId;
                    StudentLessonSubjectGroupView subjectGroup = subjectGroups.computeIfAbsent(subjectKey, key -> {
                        StudentLessonSubjectGroupView created = new StudentLessonSubjectGroupView(
                                subjectId,
                                subjectName(lesson, classGroupById),
                                classGroup == null ? "" : classGroup.getContextGroupHtml(),
                                classGroup == null ? "" : classGroup.getContextGroupTitle()
                        );
                        courseGroup.addSubjectGroup(created);
                        return created;
                    });
                    subjectGroup.addLesson(lesson);
                });
        return new ArrayList<>(courseGroups.values());
    }

    private static String courseName(LessonView lesson, Map<Long, ClassGroupView> classGroupById) {
        ClassGroupView classGroup = classGroupById.get(lesson.getClassGroupId());
        return classGroup == null ? "Other lessons" : classGroup.getCourseName();
    }

    private static String subjectName(LessonView lesson, Map<Long, ClassGroupView> classGroupById) {
        ClassGroupView classGroup = classGroupById.get(lesson.getClassGroupId());
        return classGroup == null ? "Unassigned subject" : classGroup.getSubjectName();
    }

    private ClassGroupView classGroupView(long classGroupId) {
        try {
            return classGroupDAO.findById(classGroupId)
                    .map(viewFactory::classGroupView)
                    .orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group", exception);
        }
    }

    private ContentBlockView contentBlockView(long contentBlockId) {
        try {
            ContentBlock block = contentBlockDAO.findById(contentBlockId).orElse(null);
            return block == null ? null : viewFactory.contentBlockView(block);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load content block", exception);
        }
    }

    public static final class StudentLessonCourseGroupView {

        private final long courseId;
        private final String courseName;
        private final List<StudentLessonSubjectGroupView> subjectGroups = new ArrayList<>();

        StudentLessonCourseGroupView(long courseId, String courseName) {
            this.courseId = courseId;
            this.courseName = courseName;
        }

        public long getCourseId() {
            return courseId;
        }

        public String getCourseName() {
            return courseName;
        }

        public List<StudentLessonSubjectGroupView> getSubjectGroups() {
            return subjectGroups;
        }

        public int getLessonCount() {
            return subjectGroups.stream()
                    .mapToInt(StudentLessonSubjectGroupView::getLessonCount)
                    .sum();
        }

        void addSubjectGroup(StudentLessonSubjectGroupView subjectGroup) {
            subjectGroups.add(subjectGroup);
        }
    }

    public static final class StudentCalendarItemView {

        private final ScheduleEventView event;
        private final LessonView lesson;

        private StudentCalendarItemView(ScheduleEventView event, LessonView lesson) {
            this.event = event;
            this.lesson = lesson;
        }

        static StudentCalendarItemView event(ScheduleEventView event) {
            return new StudentCalendarItemView(event, null);
        }

        static StudentCalendarItemView lesson(LessonView lesson) {
            return new StudentCalendarItemView(null, lesson);
        }

        public boolean isEventItem() {
            return event != null;
        }

        public boolean isLessonItem() {
            return lesson != null;
        }

        public ScheduleEventView getEvent() {
            return event;
        }

        public LessonView getLesson() {
            return lesson;
        }

        public java.time.LocalDateTime getStartsAtRaw() {
            return event == null ? lesson.getStartsAtRaw() : event.getStartsAtRaw();
        }

        public int getSortOrder() {
            return event == null ? 1 : 0;
        }

        public long getId() {
            return event == null ? lesson.getId() : event.getId();
        }
    }

    public static final class StudentLessonSubjectGroupView {

        private final long subjectId;
        private final String subjectName;
        private final String contextHtml;
        private final String contextTitle;
        private final List<LessonView> lessons = new ArrayList<>();

        StudentLessonSubjectGroupView(long subjectId, String subjectName, String contextHtml, String contextTitle) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.contextHtml = contextHtml;
            this.contextTitle = contextTitle;
        }

        public long getSubjectId() {
            return subjectId;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getContextHtml() {
            return contextHtml;
        }

        public String getContextTitle() {
            return contextTitle;
        }

        public List<LessonView> getLessons() {
            return lessons;
        }

        public int getLessonCount() {
            return lessons.size();
        }

        void addLesson(LessonView lesson) {
            lessons.add(lesson);
        }
    }
}
