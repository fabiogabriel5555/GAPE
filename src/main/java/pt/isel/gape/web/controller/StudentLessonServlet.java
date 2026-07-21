package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LearningEvent;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationState;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.service.AttendanceRecordService;
import pt.isel.gape.learning.service.AbsenceJustificationService;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.ScheduleEventService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.AttendanceRecordView;
import pt.isel.gape.web.view.AbsenceJustificationView;
import pt.isel.gape.web.view.EventCalendarDayView;
import pt.isel.gape.web.view.EventCalendarView;
import pt.isel.gape.web.view.LearningNotificationView;
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
    private static final DateTimeFormatter EVENT_MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", ApplicationDateTimeFormat.LOCALE);

    private final LessonService lessonService;
    private final ScheduleEventService scheduleEventService;
    private final ApplicationReadService.LearningEvents learningEvents;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.ContentBlocks contentBlockDAO;
    private final LearningViewFactory viewFactory;
    private final ScheduleAttendanceViewFactory scheduleViewFactory;
    private final StudentAssessmentCatalogSupport assessmentCatalogSupport;
    private final AttendanceRecordService attendanceRecordService;
    private final AbsenceJustificationService absenceJustificationService;

    public StudentLessonServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private StudentLessonServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new LessonService(connectionProvider, clock),
                new ScheduleEventService(connectionProvider, clock),
                new AttendanceRecordService(connectionProvider, clock),
                new AbsenceJustificationService(connectionProvider, clock)
        );
    }

    StudentLessonServlet(
            ApplicationReadService readService,
            LessonService lessonService,
            ScheduleEventService scheduleEventService
    ) {
        this(readService, lessonService, scheduleEventService, null, null);
    }

    private StudentLessonServlet(
            ApplicationReadService readService,
            LessonService lessonService,
            ScheduleEventService scheduleEventService,
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService absenceJustificationService
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
        this.assessmentCatalogSupport = new StudentAssessmentCatalogSupport(readService);
        this.attendanceRecordService = attendanceRecordService;
        this.absenceJustificationService = absenceJustificationService;
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
        List<LearningNotificationView> eventCalendarNotifications = calendarMode
                ? visibleStudentEvents(actor.userId())
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
        request.setAttribute("eventCalendarNotifications", eventCalendarNotifications);
        request.setAttribute("eventCalendar", eventCalendar(request, eventCalendarNotifications));
        request.setAttribute("classGroupById", classGroupById);
        request.setAttribute("contentBlockById", contentBlockById);
        List<LessonView> activeLessons = lessons.stream()
                .filter(lesson -> !lesson.isCompleted())
                .toList();
        List<LessonView> completedLessons = lessons.stream()
                .filter(LessonView::isCompleted)
                .toList();
        request.setAttribute("lessonCourseGroups", lessonCourseGroups(activeLessons, classGroupById));
        request.setAttribute("lessonCompletedCourseGroups", lessonCourseGroups(completedLessons, classGroupById));
        request.setAttribute("activeLessonCount", activeLessons.size());
        request.setAttribute("completedLessonCount", completedLessons.size());
        List<AttendanceRecordView> attendanceRecords = calendarMode ? List.of() : studentAttendance(actor, request);
        if (!calendarMode) {
            attendanceRecords.stream()
                    .map(AttendanceRecordView::getLesson)
                    .filter(java.util.Objects::nonNull)
                    .map(LessonView::getClassGroupId)
                    .filter(id -> id > 0)
                    .forEach(id -> classGroupById.computeIfAbsent(id, this::classGroupView));
        }
        List<AttendanceRecordView> completedAttendanceRecords = attendanceRecords.stream()
                .filter(StudentLessonServlet::isCompletedAttendanceStatus)
                .toList();
        List<AttendanceRecordView> activeAttendanceRecords = attendanceRecords.stream()
                .filter(record -> !isCompletedAttendanceStatus(record))
                .toList();
        request.setAttribute("attendanceRecords", activeAttendanceRecords);
        request.setAttribute("attendanceCourseGroups", attendanceCourseGroups(activeAttendanceRecords, classGroupById));
        request.setAttribute("attendanceCompletedCourseGroups", attendanceCourseGroups(completedAttendanceRecords, classGroupById));
        request.setAttribute("completedAttendanceCount", completedAttendanceRecords.size());
        request.setAttribute("calendarMode", calendarMode);
        if (!calendarMode) {
            try {
                assessmentCatalogSupport.prepare(request, actor.userId());
            } catch (SQLException exception) {
                throw new ServletException("Failed to load student assessments", exception);
            }
        }
        prepareDashboard(
                request,
                calendarMode ? "calendar" : "learning",
                calendarMode ? "Events" : "Lessons & Assessments"
        );
        forward(request, response, calendarMode ? STUDENT_CALENDAR_JSP : STUDENT_LESSON_LIST_JSP);
    }

    private List<AttendanceRecordView> studentAttendance(SessionUser actor, HttpServletRequest request) {
        if (attendanceRecordService == null) {
            return List.of();
        }
        List<AttendanceRecord> records = attendanceRecordService.listOwnAttendance(
                actor.userId(),
                currentSessionId(request),
                AccessProfileType.STUDENT,
                request.getRemoteAddr()
        );
        List<AbsenceJustification> ownJustifications = absenceJustificationService == null
                ? List.of()
                : absenceJustificationService.listOwnJustifications(
                                actor.userId(),
                                currentSessionId(request),
                                AccessProfileType.STUDENT,
                                request.getRemoteAddr()
                        );
        Map<Long, AbsenceJustificationState> justificationStates = ownJustifications.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                AbsenceJustification::attendanceRecordId,
                                AbsenceJustification::state,
                                (first, second) -> first
                        ));
        List<AttendanceRecordView> views = scheduleViewFactory.attendanceRecordViews(records, justificationStates);
        Map<Long, AttendanceRecordView> viewByRecordId = views.stream()
                .collect(java.util.stream.Collectors.toMap(
                        AttendanceRecordView::getId,
                        java.util.function.Function.identity(),
                        (first, second) -> first
                ));
        Map<Long, AbsenceJustificationView> justificationByRecordId = ownJustifications.stream()
                .map(justification -> AbsenceJustificationView.from(
                        justification,
                        viewByRecordId.get(justification.attendanceRecordId())
                ))
                .collect(java.util.stream.Collectors.toMap(
                        AbsenceJustificationView::getAttendanceRecordId,
                        java.util.function.Function.identity(),
                        (first, second) -> first
                ));
        return views.stream()
                .map(view -> view.withJustification(justificationByRecordId.get(view.getId())))
                .toList();
    }

    private static boolean isCompletedAttendanceStatus(AttendanceRecordView record) {
        String status = record.getStatusValue();
        return "present".equalsIgnoreCase(status) || "justified".equalsIgnoreCase(status);
    }

    private static boolean isCalendarRequest(HttpServletRequest request) {
        return "/student/calendar".equals(request.getServletPath())
                || "/student/events".equals(request.getServletPath());
    }

    /**
     * The student calendar is a read-only view over the complete visible
     * learning-event projection.  Deliberately do not filter by read state:
     * read and unread events belong in the same calendar.
     */
    private List<LearningNotificationView> visibleStudentEvents(long studentUserId) {
        try {
            LocalDateTime now = LocalDateTime.now(ApplicationClock.system());
            int total = learningEvents.countVisible(
                            List.of(),
                            List.of(),
                            List.of(),
                            studentUserId,
                            true,
                            false,
                            null,
                            null,
                            null,
                            null,
                            now
                    );
            if (total <= 0) {
                return List.of();
            }
            return learningEvents.findVisible(
                            List.of(),
                            List.of(),
                            List.of(),
                            studentUserId,
                            true,
                            false,
                            null,
                            null,
                            null,
                            null,
                            now,
                            studentUserId,
                            total,
                            0
                    )
                    .stream()
                    .map(StudentLessonServlet::learningNotificationView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load student calendar events", exception);
        }
    }

    private static LearningNotificationView learningNotificationView(LearningEvent event) {
        return new LearningNotificationView(
                event.id(),
                event.eventType(),
                event.categoryLabel(),
                event.title(),
                event.description(),
                event.contextLabel(),
                event.contextTitle(),
                LearningNotificationView.dateTime(event.occurredAt()),
                event.occurredAt(),
                event.classGroupId(),
                event.detailHref(),
                event.iconClass(),
                event.badgeClass(),
                event.stateLabel(),
                event.stateBadgeClass(),
                event.read()
        );
    }

    private static EventCalendarView eventCalendar(
            HttpServletRequest request,
            List<LearningNotificationView> events
    ) {
        YearMonth month = selectedEventMonth(request, events);
        LocalDate today = LocalDate.now(ApplicationClock.system());
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        LocalDate cursor = firstDay.minusDays(firstDay.getDayOfWeek().getValue() - 1L);
        LocalDate gridEnd = lastDay.plusDays(7L - lastDay.getDayOfWeek().getValue());
        List<EventCalendarDayView> days = new ArrayList<>();
        int visibleMonthEventCount = 0;
        while (!cursor.isAfter(gridEnd) || days.size() < 35) {
            LocalDate day = cursor;
            List<LearningNotificationView> dayEvents = events.stream()
                    .filter(event -> event.getOccurredAt() != null)
                    .filter(event -> event.getOccurredAt().toLocalDate().equals(day))
                    .toList();
            if (YearMonth.from(day).equals(month)) {
                visibleMonthEventCount += dayEvents.size();
            }
            days.add(new EventCalendarDayView(
                    day,
                    YearMonth.from(day).equals(month),
                    day.equals(today),
                    dayEvents
            ));
            cursor = cursor.plusDays(1);
        }
        return new EventCalendarView(
                month,
                month.format(EVENT_MONTH_LABEL),
                days,
                visibleMonthEventCount,
                events.size()
        );
    }

    private static YearMonth selectedEventMonth(
            HttpServletRequest request,
            List<LearningNotificationView> events
    ) {
        String monthValue = text(request, "month");
        if (monthValue != null) {
            try {
                return YearMonth.parse(monthValue);
            } catch (RuntimeException ignored) {
                // Fall through to the latest event month.
            }
        }
        return events.stream()
                .map(LearningNotificationView::getOccurredAt)
                .filter(occurredAt -> occurredAt != null)
                .findFirst()
                .map(YearMonth::from)
                .orElseGet(() -> YearMonth.now(ApplicationClock.system()));
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
        Map<String, StudentLessonOccurrenceGroupView> occurrenceGroups = new LinkedHashMap<>();
        Map<String, StudentLessonClassGroupView> classGroups = new LinkedHashMap<>();
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
                    long occurrenceId = classGroup == null ? 0L : classGroup.getCourseOccurrenceId();
                    String occurrenceKey = subjectKey + ":" + occurrenceId;
                    StudentLessonOccurrenceGroupView occurrenceGroup = occurrenceGroups.computeIfAbsent(occurrenceKey, key -> {
                        StudentLessonOccurrenceGroupView created = new StudentLessonOccurrenceGroupView(
                                occurrenceId,
                                classGroup == null ? "Occurrence" : classGroup.getOccurrenceLabel(),
                                classGroup == null ? "" : classGroup.getOccurrenceDateRangeLabel()
                        );
                        subjectGroup.addOccurrenceGroup(created);
                        return created;
                    });
                    String classGroupKey = occurrenceKey + ":" + lesson.getClassGroupId();
                    StudentLessonClassGroupView classGroupView = classGroups.computeIfAbsent(classGroupKey, key -> {
                        StudentLessonClassGroupView created = new StudentLessonClassGroupView(
                                lesson.getClassGroupId(),
                                classGroup == null ? "Class group" : classGroup.getCode(),
                                classGroup == null ? "" : classGroup.getContextGroupTitle()
                        );
                        occurrenceGroup.addClassGroup(created);
                        return created;
                    });
                    classGroupView.addLesson(lesson);
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

    private List<StudentAttendanceCourseGroupView> attendanceCourseGroups(
            List<AttendanceRecordView> records,
            Map<Long, ClassGroupView> classGroupById
    ) {
        Map<Long, StudentAttendanceCourseGroupView> courses = new LinkedHashMap<>();
        Map<String, StudentAttendanceSubjectGroupView> subjects = new LinkedHashMap<>();
        Map<String, StudentAttendanceOccurrenceGroupView> occurrences = new LinkedHashMap<>();
        Map<String, StudentAttendanceClassGroupView> groups = new LinkedHashMap<>();
        records.stream()
                .sorted(Comparator
                        .comparing((AttendanceRecordView record) -> record.getLesson() == null
                                ? "Other attendance" : courseName(record.getLesson(), classGroupById),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> record.getLesson() == null
                                ? "Unassigned subject" : subjectName(record.getLesson(), classGroupById),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> {
                            LessonView lesson = record.getLesson();
                            ClassGroupView group = lesson == null ? null : classGroupById.get(lesson.getClassGroupId());
                            return group == null ? "Occurrence" : group.getOccurrenceLabel();
                        }, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> {
                            LessonView lesson = record.getLesson();
                            ClassGroupView group = lesson == null ? null : classGroupById.get(lesson.getClassGroupId());
                            return group == null ? "Class group" : group.getCode();
                        }, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> record.getLesson() == null
                                ? null : record.getLesson().getStartsAtRaw(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingLong(AttendanceRecordView::getId))
                .forEach(record -> {
                    LessonView lesson = record.getLesson();
                    long classGroupId = lesson == null ? 0L : lesson.getClassGroupId();
                    ClassGroupView classGroup = classGroupById.get(classGroupId);
                    long courseId = classGroup == null ? 0L : classGroup.getCourseId();
                    long subjectId = classGroup == null ? 0L : classGroup.getSubjectId();
                    String courseName = classGroup == null ? "Other attendance" : classGroup.getCourseName();
                    String subjectName = classGroup == null ? "Unassigned subject" : classGroup.getSubjectName();
                    StudentAttendanceCourseGroupView course = courses.computeIfAbsent(courseId,
                            id -> new StudentAttendanceCourseGroupView(id, courseName));
                    String subjectKey = courseId + ":" + subjectId;
                    StudentAttendanceSubjectGroupView subject = subjects.computeIfAbsent(subjectKey, key -> {
                        StudentAttendanceSubjectGroupView created = new StudentAttendanceSubjectGroupView(subjectId, subjectName);
                        course.addSubjectGroup(created);
                        return created;
                    });
                    long occurrenceId = classGroup == null ? 0L : classGroup.getCourseOccurrenceId();
                    String occurrenceKey = subjectKey + ":" + occurrenceId;
                    StudentAttendanceOccurrenceGroupView occurrence = occurrences.computeIfAbsent(occurrenceKey, key -> {
                        StudentAttendanceOccurrenceGroupView created = new StudentAttendanceOccurrenceGroupView(
                                occurrenceId,
                                classGroup == null ? "Occurrence" : classGroup.getOccurrenceLabel(),
                                classGroup == null ? "" : classGroup.getOccurrenceDateRangeLabel()
                        );
                        subject.addOccurrenceGroup(created);
                        return created;
                    });
                    String groupKey = occurrenceKey + ":" + classGroupId;
                    StudentAttendanceClassGroupView group = groups.computeIfAbsent(groupKey, key -> {
                        StudentAttendanceClassGroupView created = new StudentAttendanceClassGroupView(
                                classGroupId,
                                classGroup == null ? "Class group" : classGroup.getCode(),
                                classGroup == null ? "" : classGroup.getContextGroupTitle()
                        );
                        occurrence.addClassGroup(created);
                        return created;
                    });
                    group.addRecord(record);
                });
        return new ArrayList<>(courses.values());
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
        private final List<StudentLessonOccurrenceGroupView> occurrenceGroups = new ArrayList<>();

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
            return occurrenceGroups.stream().flatMap(group -> group.getLessons().stream()).toList();
        }

        public int getLessonCount() {
            return occurrenceGroups.stream().mapToInt(StudentLessonOccurrenceGroupView::getLessonCount).sum();
        }

        public List<StudentLessonOccurrenceGroupView> getOccurrenceGroups() {
            return occurrenceGroups;
        }

        /**
         * Compatibility accessor for the legacy hidden lesson markup.  The
         * visible catalogue now uses occurrenceGroups, but JSP compilation
         * still evaluates the legacy branch, so expose the flattened class
         * groups without changing the new hierarchy.
         */
        public List<StudentLessonClassGroupView> getClassGroups() {
            return occurrenceGroups.stream().flatMap(group -> group.getClassGroups().stream()).toList();
        }

        void addOccurrenceGroup(StudentLessonOccurrenceGroupView occurrenceGroup) {
            occurrenceGroups.add(occurrenceGroup);
        }
    }

    public static final class StudentLessonOccurrenceGroupView {
        private final long id;
        private final String label;
        private final String dateRangeLabel;
        private final List<StudentLessonClassGroupView> classGroups = new ArrayList<>();

        StudentLessonOccurrenceGroupView(long id, String label, String dateRangeLabel) {
            this.id = id;
            this.label = label;
            this.dateRangeLabel = dateRangeLabel;
        }

        public long getId() { return id; }
        public String getLabel() { return label; }
        public String getDateRangeLabel() { return dateRangeLabel; }
        public List<StudentLessonClassGroupView> getClassGroups() { return classGroups; }
        public List<LessonView> getLessons() {
            return classGroups.stream().flatMap(group -> group.getLessons().stream()).toList();
        }
        public int getLessonCount() {
            return classGroups.stream().mapToInt(StudentLessonClassGroupView::getLessonCount).sum();
        }
        void addClassGroup(StudentLessonClassGroupView classGroup) { classGroups.add(classGroup); }
    }

    public static final class StudentLessonClassGroupView {
        private final long id;
        private final String code;
        private final String contextTitle;
        private final List<LessonView> lessons = new ArrayList<>();

        StudentLessonClassGroupView(long id, String code, String contextTitle) {
            this.id = id;
            this.code = code;
            this.contextTitle = contextTitle;
        }

        public long getId() { return id; }
        public String getCode() { return code; }
        public String getContextTitle() { return contextTitle; }
        public List<LessonView> getLessons() { return lessons; }
        public int getLessonCount() { return lessons.size(); }
        void addLesson(LessonView lesson) { lessons.add(lesson); }
    }

    public static final class StudentAttendanceCourseGroupView {
        private final long id;
        private final String name;
        private final List<StudentAttendanceSubjectGroupView> subjectGroups = new ArrayList<>();
        StudentAttendanceCourseGroupView(long id, String name) { this.id = id; this.name = name; }
        public long getId() { return id; }
        public String getName() { return name; }
        public List<StudentAttendanceSubjectGroupView> getSubjectGroups() { return subjectGroups; }
        public int getRecordCount() { return subjectGroups.stream().mapToInt(StudentAttendanceSubjectGroupView::getRecordCount).sum(); }
        void addSubjectGroup(StudentAttendanceSubjectGroupView group) { subjectGroups.add(group); }
    }

    public static final class StudentAttendanceSubjectGroupView {
        private final long id;
        private final String name;
        private final List<StudentAttendanceOccurrenceGroupView> occurrenceGroups = new ArrayList<>();
        StudentAttendanceSubjectGroupView(long id, String name) { this.id = id; this.name = name; }
        public long getId() { return id; }
        public String getName() { return name; }
        public List<StudentAttendanceOccurrenceGroupView> getOccurrenceGroups() { return occurrenceGroups; }
        public List<StudentAttendanceClassGroupView> getClassGroups() {
            return occurrenceGroups.stream()
                    .flatMap(group -> group.getClassGroups().stream())
                    .toList();
        }
        public int getRecordCount() { return occurrenceGroups.stream().mapToInt(StudentAttendanceOccurrenceGroupView::getRecordCount).sum(); }
        void addOccurrenceGroup(StudentAttendanceOccurrenceGroupView group) { occurrenceGroups.add(group); }
    }

    public static final class StudentAttendanceOccurrenceGroupView {
        private final long id;
        private final String label;
        private final String dateRangeLabel;
        private final List<StudentAttendanceClassGroupView> classGroups = new ArrayList<>();

        StudentAttendanceOccurrenceGroupView(long id, String label, String dateRangeLabel) {
            this.id = id;
            this.label = label;
            this.dateRangeLabel = dateRangeLabel;
        }

        public long getId() { return id; }
        public String getLabel() { return label; }
        public String getDateRangeLabel() { return dateRangeLabel; }
        public List<StudentAttendanceClassGroupView> getClassGroups() { return classGroups; }
        public int getRecordCount() { return classGroups.stream().mapToInt(StudentAttendanceClassGroupView::getRecordCount).sum(); }
        void addClassGroup(StudentAttendanceClassGroupView group) { classGroups.add(group); }
    }

    public static final class StudentAttendanceClassGroupView {
        private final long id;
        private final String code;
        private final String contextTitle;
        private final List<AttendanceRecordView> records = new ArrayList<>();
        StudentAttendanceClassGroupView(long id, String code, String contextTitle) {
            this.id = id; this.code = code; this.contextTitle = contextTitle;
        }
        public long getId() { return id; }
        public String getCode() { return code; }
        public String getContextTitle() { return contextTitle; }
        public List<AttendanceRecordView> getRecords() { return records; }
        public int getRecordCount() { return records.size(); }
        void addRecord(AttendanceRecordView record) { records.add(record); }
    }
}
