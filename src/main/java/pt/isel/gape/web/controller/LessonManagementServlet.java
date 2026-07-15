package pt.isel.gape.web.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.LessonUpdateCommand;
import pt.isel.gape.learning.model.LearningEvent;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EventCalendarDayView;
import pt.isel.gape.web.view.EventCalendarView;
import pt.isel.gape.web.view.LearningNotificationView;
import pt.isel.gape.web.view.LessonFormData;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.SelectOptionView;

@WebServlet(name = "lessonManagementServlet", urlPatterns = {
        "/learning/lessons",
        "/learning/lessons/*",
        "/learning/calendar",
        "/learning/events",
        "/learning/events/*"
})
public final class LessonManagementServlet extends DashboardServletSupport {

    private static final String LESSON_LIST_JSP = "/WEB-INF/views/learning/lesson-list.jsp";
    private static final String LESSON_DETAIL_JSP = "/WEB-INF/views/learning/lesson-detail.jsp";
    private static final String LESSON_FORM_JSP = "/WEB-INF/views/learning/lesson-form.jsp";
    private static final String LEARNING_MANAGEMENT_ROWS_JSP =
            "/WEB-INF/fragments/learning-management-rows.jsp";
    private static final int EVENT_PAGE_SIZE = 20;
    private static final int LEARNING_MANAGEMENT_PAGE_SIZE = 10;
    private static final DateTimeFormatter EVENT_MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", ApplicationDateTimeFormat.LOCALE);

    private final ApplicationReadService readService;
    private final LessonService lessonService;
    private final ClassGroupService classGroupService;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.ContentBlocks contentBlockDAO;
    private final ApplicationReadService.PhysicalRooms physicalRoomDAO;
    private final ApplicationReadService.LearningEvents learningEventDAO;
    private final AssessmentService assessmentService;
    private final ApplicationReadService.Assessments assessmentDAO;
    private final ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO;
    private final ApplicationReadService.GradeSheets gradeSheetDAO;
    private final ApplicationReadService.Certificates certificateDAO;
    private final ApplicationReadService.OrganicUnits organicUnitDAO;
    private final LearningViewFactory viewFactory;
    private final AssessmentViewFactory assessmentViewFactory;
    private final AttendanceManagementServlet attendanceManagementServlet;

    public LessonManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private LessonManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new LessonService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new AssessmentService(connectionProvider, clock)
        );
    }

    LessonManagementServlet(
            ApplicationReadService readService,
            LessonService lessonService,
            ClassGroupService classGroupService,
            AssessmentService assessmentService
    ) {
        this.readService = readService;
        this.lessonService = lessonService;
        this.classGroupService = classGroupService;
        this.classGroupDAO = readService.classGroups();
        this.contentBlockDAO = readService.contentBlocks();
        this.physicalRoomDAO = readService.physicalRooms();
        this.learningEventDAO = readService.learningEvents();
        this.assessmentService = assessmentService;
        this.assessmentDAO = readService.assessments();
        this.classGroupEnrollmentDAO = readService.classGroupEnrollments();
        this.gradeSheetDAO = readService.gradeSheets();
        this.certificateDAO = readService.certificates();
        this.organicUnitDAO = readService.organicUnits();
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
        this.assessmentViewFactory = new AssessmentViewFactory(
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
        this.attendanceManagementServlet = new AttendanceManagementServlet();
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
                if (segments.length == 2 && "detail".equals(segments[1])) {
                    showEventDetail(request, response, Long.parseLong(segments[0]));
                    return;
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (segments.length == 0) {
                showList(request, response, false);
                return;
            }
            if (segments.length == 1 && "new".equals(segments[0])) {
                if (!isLessonModalRequest(request)) {
                    redirectToLessonModalHost(request, response, "new");
                    return;
                }
                showCreateForm(request, response, null);
                return;
            }
            if (segments.length == 1) {
                if (!isLessonModalRequest(request)) {
                    redirectToLessonModalHost(request, response, "detail:" + Long.parseLong(segments[0]));
                    return;
                }
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "edit".equals(segments[1])) {
                if (!isLessonModalRequest(request)) {
                    redirectToLessonModalHost(request, response, "edit:" + Long.parseLong(segments[0]));
                    return;
                }
                showEditForm(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (isCalendarRequest(request)) {
                if (segments.length == 1 && "read-by-href".equals(segments[0])) {
                    markEventReadByHref(request, response);
                    return;
                }
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            if (segments.length == 0) {
                createLesson(request, response);
                return;
            }
            if (segments.length == 1) {
                updateLesson(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2) {
                long lessonId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "cancel" -> cancelLesson(request, response, lessonId);
                    case "complete" -> completeLesson(request, response, lessonId);
                    case "delete" -> deleteLesson(request, response, lessonId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, boolean calendarMode)
            throws ServletException, IOException {
        Long classGroupFilter = optionalLong(request, "classGroupId");
        String selectedCalendarFilter = calendarMode ? calendarFilter(request) : "all";
        List<ClassGroup> visibleClassGroups = visibleClassGroups(request);
        List<LessonView> lessons = calendarMode
                ? List.of()
                : managedLessons(request, visibleClassGroups);
        List<ClassGroupView> classGroupOptions = viewFactory.classGroupViews(visibleClassGroups);
        Map<Long, ClassGroupView> classGroupById = classGroupMap(classGroupOptions);
        List<AssessmentView> assessments = calendarMode ? List.of() : managedAssessments(request);

        if (classGroupFilter != null) {
            lessons = lessons.stream()
                    .filter(lesson -> lesson.getClassGroupId() == classGroupFilter)
                    .toList();
            assessments = assessments.stream()
                    .filter(assessment -> assessment.getClassGroupId() != null
                            && assessment.getClassGroupId() == classGroupFilter)
                    .toList();
        }

        lessons = lessons.stream()
                .sorted(Comparator.comparing(
                                LessonView::getStartsAtRaw,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingLong(LessonView::getId).reversed()))
                .toList();
        assessments = assessments.stream()
                .sorted(Comparator.comparing(
                                AssessmentView::getAvailableFromRaw,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingLong(AssessmentView::getId).reversed()))
                .toList();
        List<LessonView> managedLessonRows = lessons;
        List<AssessmentView> managedAssessmentRows = assessments;
        Map<Long, Boolean> canManageClassGroupById = calendarMode
                ? Map.of()
                : canManageClassGroupMap(request, classGroupOptions);
        Map<Long, Integer> pendingAssessmentEnrollmentCountById = calendarMode
                ? Map.of()
                : pendingAssessmentEnrollmentCounts(managedAssessmentRows);
        Map<Long, Integer> pendingAssessmentCorrectionCountById = calendarMode
                ? Map.of()
                : pendingAssessmentCorrectionCounts(managedAssessmentRows);

        if (!calendarMode && isLearningManagementRowsFragmentRequest(request)) {
            prepareLearningManagementRows(
                    request,
                    learningManagementKind(request),
                    managedLessonRows,
                    managedAssessmentRows,
                    classGroupById,
                    canManageClassGroupById,
                    pendingAssessmentEnrollmentCountById,
                    pendingAssessmentCorrectionCountById,
                    requestedLearningManagementOffset(request),
                    requestedLearningManagementLoadAll(request)
            );
            forward(request, response, LEARNING_MANAGEMENT_ROWS_JSP);
            return;
        }

        LearningManagementPage<LessonView> lessonPage = calendarMode
                ? LearningManagementPage.all(managedLessonRows)
                : pageLearningManagementItems(managedLessonRows, LessonView::isCompleted, 0, false);
        LearningManagementPage<AssessmentView> assessmentPage = calendarMode
                ? LearningManagementPage.all(managedAssessmentRows)
                : pageLearningManagementItems(managedAssessmentRows, AssessmentView::isCompleted, 0, false);
        List<LessonView> lessonPageRows = lessonPage.rows();
        List<AssessmentView> assessmentPageRows = assessmentPage.rows();
        List<LearningNotificationView> learningNotifications = List.of();
        List<LearningNotificationView> eventCalendarNotifications = List.of();
        int notificationCount = 0;
        int unreadEventCount = 0;
        if (calendarMode) {
            int requestedPage = positiveInteger(request, "page", 1);
            notificationCount = countPersistedEvents(
                    request,
                    selectedCalendarFilter,
                    classGroupFilter,
                    classGroupById
            );
            unreadEventCount = countUnreadPersistedEvents(
                    request,
                    selectedCalendarFilter,
                    classGroupFilter,
                    classGroupById
            );
            int pageCount = pageCount(notificationCount, EVENT_PAGE_SIZE);
            int currentPage = Math.min(requestedPage, pageCount);
            learningNotifications = persistedEvents(
                    request,
                    selectedCalendarFilter,
                    classGroupFilter,
                    classGroupById,
                    currentPage
            );
            eventCalendarNotifications = persistedEvents(
                    request,
                    selectedCalendarFilter,
                    classGroupFilter,
                    classGroupById,
                    notificationCount,
                    0
            );
            request.setAttribute("eventCurrentPage", currentPage);
            request.setAttribute("eventPageCount", pageCount);
            request.setAttribute("eventPageSize", EVENT_PAGE_SIZE);
            request.setAttribute("eventTotalCount", notificationCount);
            request.setAttribute("eventPageStart", pageStart(notificationCount, currentPage, EVENT_PAGE_SIZE));
            request.setAttribute("eventPageEnd", pageEnd(notificationCount, currentPage, EVENT_PAGE_SIZE));
            request.setAttribute("eventHasPreviousPage", currentPage > 1);
            request.setAttribute("eventHasNextPage", currentPage < pageCount);
            request.setAttribute("eventPreviousPage", Math.max(1, currentPage - 1));
            request.setAttribute("eventNextPage", Math.min(pageCount, currentPage + 1));
            request.setAttribute("eventTypeCount", 16);
            request.setAttribute("eventCalendar", eventCalendar(request, eventCalendarNotifications, notificationCount));
        }
        List<ClassGroupView> manageableClassGroupOptions = calendarMode
                ? List.of()
                : viewFactory.classGroupViews(manageableClassGroups(request));

        request.setAttribute("lessons", lessonPageRows);
        request.setAttribute("assessments", assessmentPageRows);
        request.setAttribute("learningNotifications", learningNotifications);
        request.setAttribute("eventCalendarNotifications", eventCalendarNotifications);
        request.setAttribute("classGroupOptions", classGroupOptions);
        request.setAttribute("classGroupById", classGroupById);
        request.setAttribute("classGroupOptionCount", classGroupOptions.size());
        request.setAttribute("manageableClassGroupOptions", manageableClassGroupOptions);
        request.setAttribute("canManageClassGroupById", canManageClassGroupById);
        request.setAttribute("pendingAssessmentEnrollmentCountById", pendingAssessmentEnrollmentCountById);
        request.setAttribute("pendingAssessmentCorrectionCountById", pendingAssessmentCorrectionCountById);
        request.setAttribute("selectedClassGroupId", classGroupFilter);
        request.setAttribute("selectedCalendarFilter", selectedCalendarFilter);
        request.setAttribute("calendarFilterOptions", calendarFilterOptions(selectedCalendarFilter));
        request.setAttribute("calendarMode", calendarMode);
        request.setAttribute("lessonCount", managedLessonRows.size());
        request.setAttribute("assessmentCount", managedAssessmentRows.size());
        request.setAttribute("formCount", managedAssessmentRows.stream().filter(AssessmentView::isForm).count());
        request.setAttribute("testCount", managedAssessmentRows.stream().filter(AssessmentView::isTest).count());
        request.setAttribute("examCount", managedAssessmentRows.stream().filter(AssessmentView::isExam).count());
        request.setAttribute("pendingCorrectionCount",
                pendingAssessmentCorrectionCountById.values().stream().mapToInt(Integer::intValue).sum());
        request.setAttribute("pendingAssessmentEnrollmentCount",
                pendingAssessmentEnrollmentCountById.values().stream().mapToInt(Integer::intValue).sum());
        request.setAttribute("notificationCount", notificationCount);
        // This is the unread count for the filtered Events panel only.  The
        // sidebar's eventUnreadCount is prepared globally by the dashboard
        // filter/support layer and must not be replaced when this page opens.
        request.setAttribute("eventListUnreadCount", unreadEventCount);
        request.setAttribute("activeLessonCount", managedLessonRows.stream().filter(LessonView::isActive).count());
        request.setAttribute("scheduledLessonCount", managedLessonRows.stream().filter(LessonView::isScheduled).count());
        if (!calendarMode) {
            request.setAttribute("lessonManagementOffset", 0);
            request.setAttribute("lessonManagementCurrentPage", 1);
            request.setAttribute("lessonManagementLoadAll", false);
            request.setAttribute("lessonManagementPageTotal", lessonPage.totalCount());
            request.setAttribute("lessonManagementCompletedTotal", lessonPage.completedCount());
            request.setAttribute("lessonManagementHasMore", lessonPage.hasMore());
            request.setAttribute("lessonManagementNextOffset", lessonPage.nextOffset());
            request.setAttribute("assessmentManagementOffset", 0);
            request.setAttribute("assessmentManagementCurrentPage", 1);
            request.setAttribute("assessmentManagementLoadAll", false);
            request.setAttribute("assessmentManagementPageTotal", assessmentPage.totalCount());
            request.setAttribute("assessmentManagementCompletedTotal", assessmentPage.completedCount());
            request.setAttribute("assessmentManagementHasMore", assessmentPage.hasMore());
            request.setAttribute("assessmentManagementNextOffset", assessmentPage.nextOffset());
        }
        if (!calendarMode) {
            SessionUser actor = requireCurrentUser(request);
            attendanceManagementServlet.populateLearningAttendanceAttributes(
                    request,
                    actor,
                    primaryProfile(actor),
                    "/learning/lessons",
                    "/learning/lessons/attendance",
                    "/learning/lessons#attendance"
            );
            request.setAttribute("initialLessonAssessmentPanel",
                    text(request, "attendanceState") == null && text(request, "attendancePage") == null
                            ? "lessons"
                            : "attendance");
        }
        prepareDashboard(
                request,
                calendarMode ? "calendar" : "lessons",
                calendarMode ? "Events" : "Lessons & Assessments",
                null,
                null
        );
        forward(request, response, LESSON_LIST_JSP);
    }

    private void prepareLearningManagementRows(
            HttpServletRequest request,
            String kind,
            List<LessonView> lessons,
            List<AssessmentView> assessments,
            Map<Long, ClassGroupView> classGroupById,
            Map<Long, Boolean> canManageClassGroupById,
            Map<Long, Integer> pendingAssessmentEnrollmentCountById,
            Map<Long, Integer> pendingAssessmentCorrectionCountById,
            int requestedOffset,
            boolean loadAll
    ) {
        boolean lessonKind = "lessons".equals(kind);
        LearningManagementPage<LessonView> lessonPage = lessonKind
                ? pageLearningManagementItems(lessons, LessonView::isCompleted, requestedOffset, loadAll)
                : LearningManagementPage.all(List.of());
        LearningManagementPage<AssessmentView> assessmentPage = lessonKind
                ? LearningManagementPage.all(List.of())
                : pageLearningManagementItems(assessments, AssessmentView::isCompleted, requestedOffset, loadAll);
        LearningManagementPage<?> page = lessonKind ? lessonPage : assessmentPage;

        request.setAttribute("learningManagementKind", lessonKind ? "lessons" : "assessments");
        request.setAttribute("lessons", lessonPage.rows());
        request.setAttribute("assessments", assessmentPage.rows());
        request.setAttribute("classGroupById", classGroupById);
        request.setAttribute("canManageClassGroupById", canManageClassGroupById);
        request.setAttribute("pendingAssessmentEnrollmentCountById", pendingAssessmentEnrollmentCountById);
        request.setAttribute("pendingAssessmentCorrectionCountById", pendingAssessmentCorrectionCountById);
        request.setAttribute("currentReturnToParam", "/learning/lessons");
        request.setAttribute("learningManagementOffset", page.offset());
        request.setAttribute("learningManagementCurrentPage", page.offset() / LEARNING_MANAGEMENT_PAGE_SIZE + 1);
        request.setAttribute("learningManagementLoadAll", loadAll);
        request.setAttribute("learningManagementTotal", page.totalCount());
        request.setAttribute("learningManagementCompletedCount", page.completedCount());
        request.setAttribute("learningManagementBucket", "all");
        request.setAttribute("learningManagementHasMore", page.hasMore());
        request.setAttribute("learningManagementNextOffset", page.nextOffset());
    }

    private static boolean isLearningManagementRowsFragmentRequest(HttpServletRequest request) {
        return "learning-management-rows".equals(text(request, "fragment"));
    }

    private Map<Long, Integer> pendingAssessmentEnrollmentCounts(List<AssessmentView> assessments) {
        try {
            return readService.assessmentEnrollments().countByAssessmentIdsAndState(
                    assessments.stream().map(AssessmentView::getId).toList(),
                    EnrollmentState.PENDING
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load pending assessment enrollment counts", exception);
        }
    }

    private Map<Long, Integer> pendingAssessmentCorrectionCounts(List<AssessmentView> assessments) {
        try {
            return readService.attempts().countByAssessmentIdsAndState(
                    assessments.stream().map(AssessmentView::getId).toList(),
                    AttemptState.SUBMITTED
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load pending assessment correction counts", exception);
        }
    }

    private static String learningManagementKind(HttpServletRequest request) {
        return "assessments".equals(text(request, "kind")) ? "assessments" : "lessons";
    }

    private static int requestedLearningManagementOffset(HttpServletRequest request) {
        String value = text(request, "offset");
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean requestedLearningManagementLoadAll(HttpServletRequest request) {
        return Boolean.parseBoolean(text(request, "loadAll"));
    }

    private static <T> LearningManagementPage<T> pageLearningManagementItems(
            List<T> items,
            Predicate<T> completed,
            int requestedOffset,
            boolean loadAll
    ) {
        List<T> activeItems = items.stream().filter(item -> !completed.test(item)).toList();
        List<T> completedItems = items.stream().filter(completed).toList();
        // Class Group Management pages a single, bounded collection and only
        // then separates its completed rows into the archive at render time.
        // Keep active rows first so the archive is always the last block of a
        // page, while allowing the first page to expose the same paginator and
        // Load All control when completed rows make the total exceed ten.
        List<T> orderedItems = new ArrayList<>(activeItems.size() + completedItems.size());
        orderedItems.addAll(activeItems);
        orderedItems.addAll(completedItems);
        int fromIndex = Math.min(Math.max(0, requestedOffset), orderedItems.size());
        int toIndex = loadAll
                ? orderedItems.size()
                : Math.min(fromIndex + LEARNING_MANAGEMENT_PAGE_SIZE, orderedItems.size());
        List<T> rows = new ArrayList<>(orderedItems.subList(fromIndex, toIndex));
        return new LearningManagementPage<>(
                List.copyOf(rows),
                fromIndex,
                orderedItems.size(),
                completedItems.size(),
                toIndex,
                !loadAll && toIndex < orderedItems.size()
        );
    }

    private record LearningManagementPage<T>(
            List<T> rows,
            int offset,
            int totalCount,
            int completedCount,
            int nextOffset,
            boolean hasMore
    ) {
        private static <T> LearningManagementPage<T> all(List<T> rows) {
            return new LearningManagementPage<>(List.copyOf(rows), 0, rows.size(), 0, rows.size(), false);
        }
    }

    private int countPersistedEvents(
            HttpServletRequest request,
            String selectedCalendarFilter,
            Long classGroupFilter,
            Map<Long, ClassGroupView> classGroupById
    ) {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        ClassGroupView selectedClassGroup = classGroupFilter == null ? null : classGroupById.get(classGroupFilter);
        try {
            return learningEventDAO.countVisible(
                    classGroupById.values().stream().map(ClassGroupView::getId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getCourseId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getSubjectId).distinct().toList(),
                    profile == AccessProfileType.STUDENT ? actor.userId() : null,
                    profile == AccessProfileType.STUDENT,
                    profile == AccessProfileType.ADMINISTRATOR,
                    selectedCalendarFilter,
                    selectedClassGroup == null ? classGroupFilter : Long.valueOf(selectedClassGroup.getId()),
                    selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getCourseId()),
                    selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getSubjectId()),
                    LocalDateTime.now(ApplicationClock.system())
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count persisted learning events", exception);
        }
    }

    private List<LearningNotificationView> persistedEvents(
            HttpServletRequest request,
            String selectedCalendarFilter,
            Long classGroupFilter,
            Map<Long, ClassGroupView> classGroupById,
            int currentPage
    ) {
        return persistedEvents(
                request,
                selectedCalendarFilter,
                classGroupFilter,
                classGroupById,
                EVENT_PAGE_SIZE,
                Math.max(0, (currentPage - 1) * EVENT_PAGE_SIZE)
        );
    }

    private List<LearningNotificationView> persistedEvents(
            HttpServletRequest request,
            String selectedCalendarFilter,
            Long classGroupFilter,
            Map<Long, ClassGroupView> classGroupById,
            int limit,
            int offset
    ) {
        if (limit <= 0) {
            return List.of();
        }
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        ClassGroupView selectedClassGroup = classGroupFilter == null ? null : classGroupById.get(classGroupFilter);
        try {
            return learningEventDAO.findVisible(
                            classGroupById.values().stream().map(ClassGroupView::getId).distinct().toList(),
                            classGroupById.values().stream().map(ClassGroupView::getCourseId).distinct().toList(),
                            classGroupById.values().stream().map(ClassGroupView::getSubjectId).distinct().toList(),
                            profile == AccessProfileType.STUDENT ? actor.userId() : null,
                            profile == AccessProfileType.STUDENT,
                            profile == AccessProfileType.ADMINISTRATOR,
                            selectedCalendarFilter,
                            selectedClassGroup == null ? classGroupFilter : Long.valueOf(selectedClassGroup.getId()),
                            selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getCourseId()),
                            selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getSubjectId()),
                            LocalDateTime.now(ApplicationClock.system()),
                            actor.userId(),
                            limit,
                            offset
                    )
                    .stream()
                    .map(LessonManagementServlet::eventView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load persisted learning events", exception);
        }
    }

    private static EventCalendarView eventCalendar(
            HttpServletRequest request,
            List<LearningNotificationView> events,
            int totalEventCount
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
                totalEventCount
        );
    }

    private static YearMonth selectedEventMonth(HttpServletRequest request, List<LearningNotificationView> events) {
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

    private int countUnreadPersistedEvents(
            HttpServletRequest request,
            String selectedCalendarFilter,
            Long classGroupFilter,
            Map<Long, ClassGroupView> classGroupById
    ) {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        ClassGroupView selectedClassGroup = classGroupFilter == null ? null : classGroupById.get(classGroupFilter);
        try {
            return learningEventDAO.countUnreadVisible(
                    classGroupById.values().stream().map(ClassGroupView::getId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getCourseId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getSubjectId).distinct().toList(),
                    profile == AccessProfileType.STUDENT ? actor.userId() : null,
                    profile == AccessProfileType.STUDENT,
                    profile == AccessProfileType.ADMINISTRATOR,
                    selectedCalendarFilter,
                    selectedClassGroup == null ? classGroupFilter : Long.valueOf(selectedClassGroup.getId()),
                    selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getCourseId()),
                    selectedClassGroup == null ? null : Long.valueOf(selectedClassGroup.getSubjectId()),
                    LocalDateTime.now(ApplicationClock.system()),
                    actor.userId()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count unread learning events", exception);
        }
    }

    private static LearningNotificationView eventView(LearningEvent event) {
        return new LearningNotificationView(
                event.id(),
                event.category(),
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

    private void showEventDetail(HttpServletRequest request, HttpServletResponse response, long eventId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        List<ClassGroupView> classGroupOptions = viewFactory.classGroupViews(visibleClassGroups(request));
        Map<Long, ClassGroupView> classGroupById = classGroupMap(classGroupOptions);
        LocalDateTime now = LocalDateTime.now(ApplicationClock.system());
        try {
            Optional<LearningEvent> event = learningEventDAO.findVisibleById(
                    classGroupById.values().stream().map(ClassGroupView::getId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getCourseId).distinct().toList(),
                    classGroupById.values().stream().map(ClassGroupView::getSubjectId).distinct().toList(),
                    profile == AccessProfileType.STUDENT ? actor.userId() : null,
                    profile == AccessProfileType.STUDENT,
                    profile == AccessProfileType.ADMINISTRATOR,
                    now,
                    actor.userId(),
                    eventId
            );
            if (event.isEmpty() || !isSafeReturnPath(event.get().detailHref())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            learningEventDAO.markReadById(actor.userId(), eventId, now);
            response.sendRedirect(request.getContextPath() + eventRedirectHref(event.get().detailHref()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to open learning event detail", exception);
        }
    }

    private static String eventRedirectHref(String href) {
        int anchorIndex = href.indexOf('#');
        String pathAndQuery = anchorIndex >= 0 ? href.substring(0, anchorIndex) : href;
        String anchor = anchorIndex >= 0 ? href.substring(anchorIndex) : "";
        return pathAndQuery
                + (pathAndQuery.contains("?") ? "&" : "?")
                + "eventReadRedirect=1"
                + anchor;
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
            learningEventDAO.markReadByHref(
                    actor.userId(),
                    href,
                    false,
                    LocalDateTime.now(ApplicationClock.system())
            );
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":true}");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark learning event as read", exception);
        }
    }

    private List<AssessmentView> managedAssessments(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        try {
            assessmentService.synchronizeTemporalStates();
            List<Assessment> assessments = assessmentDAO.findAll().stream()
                    .filter(assessment -> assessmentService.canManageAssessment(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            assessment.id(),
                            request.getRemoteAddr()
                    ))
                    .toList();
            return assessmentViewFactory.assessmentViews(assessments);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load assessments", exception);
        }
    }

    private List<LessonView> managedLessons(HttpServletRequest request, List<ClassGroup> visibleClassGroups) {
        SessionUser actor = requireCurrentUser(request);
        List<LessonView> lessons = new ArrayList<>();
        for (ClassGroup classGroup : visibleClassGroups) {
            try {
                lessons.addAll(lessonService.listLessonsByClassGroup(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                classGroup.id(),
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(viewFactory::lessonView)
                        .toList());
            } catch (RuntimeException ignored) {
                // Class group visibility is checked above; ignore rows that become inaccessible mid-request.
            }
        }
        return lessons;
    }

    private List<LessonView> personalCalendarLessons(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        return lessonService.listPersonalCalendarLessons(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        request.getRemoteAddr()
                )
                .stream()
                .map(viewFactory::lessonView)
                .toList();
    }

    private List<LearningNotificationView> learningNotifications(
            HttpServletRequest request,
            List<LessonView> lessons,
            String selectedFilter,
            Long classGroupFilter,
            Map<Long, ClassGroupView> visibleClassGroups
    ) {
        List<LearningNotificationView> notifications = new ArrayList<>();
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        LocalDateTime now = LocalDateTime.now(ApplicationClock.system());

        if (categoryAllowed(selectedFilter, "lessons")) {
            for (LessonView lesson : lessons) {
                if ("draft".equals(lesson.getStateValue())) {
                    continue;
                }
                ClassGroupView classGroup = visibleClassGroups.get(lesson.getClassGroupId());
                notifications.add(new LearningNotificationView(
                        "lessons",
                        "Lesson",
                        lesson.getTitle(),
                        lesson.getTypeLabel() + " lesson",
                        classGroup == null ? "Class group " + lesson.getClassGroupId() : classGroup.getCode(),
                        lesson.getDateRangeLabel(),
                        lesson.getStartsAtRaw(),
                        lesson.getClassGroupId(),
                        "/learning/lessons/" + lesson.getId(),
                        lesson.getTypeIconClass(),
                        lesson.getTypeBadgeClass(),
                        lesson.getStateLabel(),
                        lesson.getStateBadgeClass()
                ));
            }
        }

        if (categoryAllowed(selectedFilter, "assessments")) {
            notifications.addAll(assessmentNotifications(request, classGroupFilter));
        }
        if (categoryAllowed(selectedFilter, "enrollments")) {
            notifications.addAll(enrollmentNotifications(request, visibleClassGroups, classGroupFilter));
        }
        if (categoryAllowed(selectedFilter, "attendance")) {
            notifications.addAll(attendanceNotifications(request, visibleClassGroups, classGroupFilter));
        }
        if (categoryAllowed(selectedFilter, "grades")) {
            notifications.addAll(gradeSheetNotifications(visibleClassGroups, classGroupFilter, profile));
        }
        if (categoryAllowed(selectedFilter, "certificates")) {
            notifications.addAll(certificateNotifications(request, visibleClassGroups, classGroupFilter, profile));
        }

        return notifications.stream()
                .filter(notification -> classGroupFilter == null
                        || notification.getClassGroupId() == null
                        || notification.getClassGroupId().equals(classGroupFilter))
                .filter(notification -> isEffectiveNotification(notification, now))
                .sorted(Comparator.comparing(
                        LearningNotificationView::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private static boolean isEffectiveNotification(LearningNotificationView notification, LocalDateTime now) {
        LocalDateTime occurredAt = notification.getOccurredAt();
        return occurredAt == null || !occurredAt.isAfter(now);
    }

    private List<LearningNotificationView> assessmentNotifications(HttpServletRequest request, Long classGroupFilter) {
        List<LearningNotificationView> notifications = new ArrayList<>();
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        try {
            assessmentService.synchronizeTemporalStates();
            List<Assessment> assessments = assessmentDAO.findAll();
            Map<Long, List<Long>> applicableClassGroupIdsByAssessment =
                    assessmentDAO.findApplicableClassGroupIdsByAssessmentIds(
                            assessments.stream().map(Assessment::id).toList()
                    );
            for (Assessment assessment : assessments) {
                if ("draft".equals(assessment.state().toDatabaseValue())) {
                    continue;
                }
                List<Long> classGroupIds = applicableClassGroupIdsByAssessment
                        .getOrDefault(assessment.id(), List.of());
                Long contextClassGroupId = classGroupIds.isEmpty() ? null : classGroupIds.get(0);
                if (classGroupFilter != null && !classGroupIds.contains(classGroupFilter)) {
                    continue;
                }
                if (!assessmentService.canManageAssessment(
                        actor.userId(),
                        currentSessionId(request),
                        profile,
                        assessment.id(),
                        request.getRemoteAddr())) {
                    continue;
                }
                AssessmentView view = assessmentViewFactory.assessmentView(assessment);
                notifications.add(new LearningNotificationView(
                        "assessments",
                        "Assessment",
                        view.getTitle(),
                        view.getTypeLabel() + " | " + view.getModeLabel(),
                        view.getContextLabel(),
                        view.getAvailabilityLabel(),
                        view.getAvailableFromRaw(),
                        contextClassGroupId,
                        "/learning/assessments/" + view.getId(),
                        view.getIconClass(),
                        view.getSoftClass(),
                        view.getStateLabel(),
                        view.getStateBadgeClass()
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load assessment event notifications", exception);
        }
        return notifications;
    }

    private List<LearningNotificationView> enrollmentNotifications(
            HttpServletRequest request,
            Map<Long, ClassGroupView> visibleClassGroups,
            Long classGroupFilter
    ) {
        List<LearningNotificationView> notifications = new ArrayList<>();
        List<Long> classGroupIds = visibleClassGroups.values().stream()
                .filter(classGroup -> classGroupFilter == null || classGroup.getId() == classGroupFilter)
                .map(ClassGroupView::getId)
                .toList();
        try {
            for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByClassGroups(classGroupIds)) {
                ClassGroupView classGroup = visibleClassGroups.get(enrollment.classGroupId());
                if (classGroup == null) {
                    continue;
                }
                notifications.add(new LearningNotificationView(
                        "enrollments",
                        "Enrollment",
                        "Student " + enrollment.studentUserId() + " in " + classGroup.getCode(),
                        "Class group enrollment",
                        classGroup.getCode(),
                        LearningNotificationView.date(enrollment.startDate()),
                        enrollment.startDate() == null ? null : enrollment.startDate().atStartOfDay(),
                        classGroup.getId(),
                        "/learning/attendance#enrollment"
                                + "classgroup"
                                + classGroup.getId()
                                + "_"
                                + classGroup.getSubjectId()
                                + "_"
                                + classGroup.getId()
                                + "_"
                                + enrollment.studentUserId()
                                + "Detail",
                        "ph ph-user-circle-plus",
                        "bg-main-50 text-main-600",
                        enrollmentStateLabel(enrollment.state()),
                        enrollmentStateBadge(enrollment.state())
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load enrollment event notifications", exception);
        }
        return notifications;
    }

    private List<LearningNotificationView> attendanceNotifications(
            HttpServletRequest request,
            Map<Long, ClassGroupView> visibleClassGroups,
            Long classGroupFilter
    ) {
        SessionUser actor = requireCurrentUser(request);
        try {
            List<AttendanceRecord> records = readService.findVisibleAttendanceRecords(
                    primaryProfile(actor),
                    actor.userId()
            );
            List<LearningNotificationView> notifications = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now(ApplicationClock.system());
            Map<Long, Lesson> lessonsById = new HashMap<>();
            for (Lesson lesson : readService.findLessonsByIds(
                    records.stream().map(AttendanceRecord::lessonId).distinct().toList()
            )) {
                lessonsById.put(lesson.id(), lesson);
            }
            for (AttendanceRecord record : records) {
                Lesson lesson = lessonsById.get(record.lessonId());
                if (lesson == null) {
                    continue;
                }
                if (classGroupFilter != null && lesson.classGroupId() != classGroupFilter) {
                    continue;
                }
                if (lesson.endsAt() != null && lesson.endsAt().isAfter(now)) {
                    continue;
                }
                ClassGroupView classGroup = visibleClassGroups.get(lesson.classGroupId());
                notifications.add(new LearningNotificationView(
                        "attendance",
                        "Attendance",
                        "Student " + record.studentUserId() + " | " + lesson.title(),
                        "Attendance record",
                        classGroup == null ? "Class group " + lesson.classGroupId() : classGroup.getCode(),
                        LearningNotificationView.dateTime(record.checkIn() == null ? lesson.startsAt() : record.checkIn()),
                        record.checkIn() == null ? lesson.startsAt() : record.checkIn(),
                        lesson.classGroupId(),
                        "/learning/lessons#attendanceDetail" + record.id(),
                        "ph ph-check-square-offset",
                        "bg-main-two-50 text-main-two-600",
                        attendanceStatusLabel(record),
                        attendanceStateBadge(record.state())
                ));
            }
            return notifications;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load attendance event notifications", exception);
        }
    }

    private List<LearningNotificationView> gradeSheetNotifications(
            Map<Long, ClassGroupView> visibleClassGroups,
            Long classGroupFilter,
            AccessProfileType profile
    ) {
        if (profile == AccessProfileType.STUDENT) {
            return List.of();
        }
        try {
            List<LearningNotificationView> notifications = new ArrayList<>();
            for (GradeSheet gradeSheet : gradeSheetDAO.findAll()) {
                if (gradeSheet.state() == GradeSheetState.DRAFT) {
                    continue;
                }
                Long contextClassGroupId = gradeSheet.classGroupIds().stream()
                        .filter(visibleClassGroups::containsKey)
                        .findFirst()
                        .orElse(null);
                if (classGroupFilter != null && !gradeSheet.classGroupIds().contains(classGroupFilter)) {
                    continue;
                }
                if (!gradeSheet.classGroupIds().isEmpty() && contextClassGroupId == null) {
                    continue;
                }
                notifications.add(new LearningNotificationView(
                        "grades",
                        "Grade Sheet",
                        gradeSheet.title(),
                        "Grade sheet " + gradeSheet.type().toDatabaseValue(),
                        contextClassGroupId == null ? "Subject " + gradeSheet.subjectId() : visibleClassGroups.get(contextClassGroupId).getCode(),
                        LearningNotificationView.dateTime(gradeSheet.releasedAt()),
                        gradeSheet.releasedAt(),
                        contextClassGroupId,
                        "/learning/attendance#gradeSheetDetail" + gradeSheet.id(),
                        "ph ph-table",
                        "bg-info-50 text-info-600",
                        gradeSheetStateLabel(gradeSheet.state()),
                        gradeSheetStateBadge(gradeSheet.state())
                ));
            }
            return notifications;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet event notifications", exception);
        }
    }

    private List<LearningNotificationView> certificateNotifications(
            HttpServletRequest request,
            Map<Long, ClassGroupView> visibleClassGroups,
            Long classGroupFilter,
            AccessProfileType profile
    ) {
        try {
            List<LearningNotificationView> notifications = new ArrayList<>();
            List<Certificate> certificates;
            certificates = profile == AccessProfileType.STUDENT
                    ? readService.findCertificatesByStudent(requireCurrentUser(request).userId())
                    : certificateDAO.findAll();
            for (Certificate certificate : certificates) {
                if (certificate.state() != CertificateState.ISSUED) {
                    continue;
                }
                Long contextClassGroupId = visibleClassGroups.values().stream()
                        .filter(classGroup -> classGroup.getCourse().getId() == certificate.courseId())
                        .map(ClassGroupView::getId)
                        .findFirst()
                        .orElse(null);
                if (classGroupFilter != null && !visibleClassGroups.containsKey(classGroupFilter)) {
                    continue;
                }
                if (classGroupFilter != null && contextClassGroupId != null && !contextClassGroupId.equals(classGroupFilter)) {
                    continue;
                }
                notifications.add(new LearningNotificationView(
                        "certificates",
                        "Certificate",
                        certificate.title(),
                        "Student " + certificate.studentUserId(),
                        "Course " + certificate.courseId(),
                        LearningNotificationView.dateTime(certificate.issuedAt()),
                        certificate.issuedAt(),
                        contextClassGroupId,
                        profile == AccessProfileType.STUDENT
                                ? "/student/attendance#grades-certificates"
                                : "/learning/attendance#certificateDetail" + certificate.id(),
                        "ph ph-certificate",
                        "bg-success-50 text-success-600",
                        "Issued",
                        "bg-success-50 text-success-600"
                ));
            }
            return notifications;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load certificate event notifications", exception);
        }
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Lesson lesson = lessonService.getLesson(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                lessonId,
                request.getRemoteAddr()
        );
        markLearningEventsReadForCurrentUser(request, "/learning/lessons/" + lesson.id(), false);
        ClassGroup classGroup = requireClassGroup(lesson.classGroupId());
        ContentBlock contentBlock = requireContentBlock(lesson.contentBlockId());
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        PhysicalRoomView roomView = lesson.physicalRoomCode() == null
                ? null
                : physicalRoomView(lesson.physicalRoomCode());

        request.setAttribute("lesson", viewFactory.lessonView(lesson));
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("contentBlock", viewFactory.contentBlockView(contentBlock));
        request.setAttribute("physicalRoom", roomView);
        boolean canManageLesson = canManageClassGroup(request, classGroup.id());
        request.setAttribute("canManageLesson", canManageLesson);
        boolean lessonModal = isLessonModalRequest(request);
        request.setAttribute("lessonModal", lessonModal);
        request.setAttribute("lessonBackHref", backHref(request, "/learning/lessons"));
        request.setAttribute("lessonEditHref", lessonModal
                ? request.getContextPath() + "/learning/lessons/" + lesson.id() + "/edit?modal=1"
                : request.getContextPath()
                        + appendReturnTo("/learning/lessons/" + lesson.id() + "/edit", currentRequestPath(request)));
        prepareLessonContext(request, lesson.id(), "detail", "/learning/lessons");
        prepareDashboard(request, "lessons", "Lesson Detail");
        forward(request, response, LESSON_DETAIL_JSP);
    }

    private static boolean isCalendarRequest(HttpServletRequest request) {
        return "/learning/calendar".equals(request.getServletPath())
                || "/learning/events".equals(request.getServletPath());
    }

    private static boolean isLessonModalRequest(HttpServletRequest request) {
        return "1".equals(text(request, "modal"));
    }

    private void redirectToLessonModalHost(
            HttpServletRequest request,
            HttpServletResponse response,
            String modalRequest
    ) throws IOException {
        StringBuilder target = new StringBuilder("/learning/lessons?lessonModal=")
                .append(URLEncoder.encode(modalRequest, StandardCharsets.UTF_8));
        String originalQuery = request.getQueryString();
        if (originalQuery != null && !originalQuery.isBlank()) {
            target.append('&').append(originalQuery);
        }
        redirect(request, response, target.toString());
    }

    private static void writeLessonModalSuccess(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("<!doctype html><html><body><script>"
                + "window.parent.postMessage({type:'gape:lesson:changed'},window.location.origin);"
                + "</script></body></html>");
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        LessonFormData form = LessonFormData.blank(
                optionalLong(request, "classGroupId"),
                optionalLong(request, "contentBlockId"),
                text(request, "type")
        );
        showLessonForm(request, response, form, true, error);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long lessonId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Lesson lesson = lessonService.getLesson(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                lessonId,
                request.getRemoteAddr()
        );
        showLessonForm(request, response, LessonFormData.from(lesson), false, error);
    }

    private void showLessonForm(
            HttpServletRequest request,
            HttpServletResponse response,
            LessonFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(manageableClassGroups(request));
        Map<Long, List<ContentBlockView>> blocksByClassGroup = contentBlockOptionsByClassGroup(classGroups);
        List<PhysicalRoomView> roomOptions = roomOptionsForClassGroups(classGroups);

        if (!creating && form.getId() != null) {
            request.setAttribute("canManageLesson", true);
            prepareLessonContext(request, form.getId(), "edit", "/learning/lessons");
        } else if (creating) {
            request.setAttribute("learningLessonActiveChild", "new");
        }

        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("formAction", creating
                ? request.getContextPath() + "/learning/lessons"
                : request.getContextPath() + "/learning/lessons/" + form.getId());
        request.setAttribute("formReturnTo", safeReturnPath(request));
        request.setAttribute("lessonBackHref", backHref(request, creating
                ? "/learning/lessons"
                : "/learning/lessons/" + form.getId()));
        request.setAttribute("classGroupOptions", classGroups);
        request.setAttribute("lessonContextOptions", lessonContextOptions(classGroups));
        request.setAttribute("contentBlocksByClassGroup", blocksByClassGroup);
        request.setAttribute("physicalRoomOptions", roomOptions);
        request.setAttribute("lessonTypes", lessonTypeOptions(form));
        request.setAttribute("providerOptions", providerOptions(form));
        request.setAttribute("lessonModal", isLessonModalRequest(request));
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "lessons", creating ? "Create Lesson" : "Edit Lesson");
        forward(request, response, LESSON_FORM_JSP);
    }

    private void createLesson(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        LessonFormData form = LessonFormData.from(request, null);
        try {
            Lesson lesson = lessonService.createLesson(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    createCommand(form),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Lesson created.");
            if (isLessonModalRequest(request)) {
                writeLessonModalSuccess(response);
                return;
            }
            redirectPreservingReturnTo(request, response, "/learning/lessons/" + lesson.id());
        } catch (RuntimeException exception) {
            showLessonForm(request, response, form, true, messageFor(exception));
        }
    }

    private void updateLesson(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        LessonFormData form = LessonFormData.from(request, lessonId);
        try {
            lessonService.updateLesson(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    lessonId,
                    updateCommand(form),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Lesson updated.");
            if (isLessonModalRequest(request)) {
                writeLessonModalSuccess(response);
                return;
            }
            redirectPreservingReturnTo(request, response, "/learning/lessons/" + lessonId);
        } catch (RuntimeException exception) {
            showLessonForm(request, response, form, false, messageFor(exception));
        }
    }

    private void cancelLesson(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws IOException {
        changeLessonState(request, response, lessonId, LessonAction.CANCEL);
    }

    private void completeLesson(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws IOException {
        changeLessonState(request, response, lessonId, LessonAction.COMPLETE);
    }

    private void changeLessonState(
            HttpServletRequest request,
            HttpServletResponse response,
            long lessonId,
            LessonAction action
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        boolean changed = false;
        try {
            if (action == LessonAction.CANCEL) {
                lessonService.cancelLesson(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        lessonId,
                        request.getRemoteAddr()
                );
                flashSuccess(request, "Lesson cancelled.");
            } else {
                lessonService.completeLesson(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        lessonId,
                        request.getRemoteAddr()
                );
                flashSuccess(request, "Lesson completed.");
            }
            changed = true;
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        if (changed && isLessonModalRequest(request)) {
            writeLessonModalSuccess(response);
            return;
        }
        redirect(request, response, "/learning/lessons/" + lessonId);
    }

    private void deleteLesson(HttpServletRequest request, HttpServletResponse response, long lessonId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            lessonService.deleteLesson(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    lessonId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Lesson deleted.");
            if (isLessonModalRequest(request)) {
                writeLessonModalSuccess(response);
                return;
            }
            redirectToReturnPath(request, response, "/learning/lessons");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/learning/lessons/" + lessonId);
        }
    }

    private LessonCreateCommand createCommand(LessonFormData form) {
        return new LessonCreateCommand(
                requiredLong(form.getClassGroupId(), "classGroupId"),
                requiredLong(form.getContentBlockId(), "contentBlockId"),
                nullIfBlank(form.getPhysicalRoomCode()),
                form.getTitle(),
                nullIfBlank(form.getDescription()),
                form.lessonType(),
                nullIfBlank(form.getProvider()),
                nullIfBlank(form.getAccessUrl()),
                form.isAttendanceRequired(),
                form.lessonState(),
                form.startsAtDateTime(),
                form.endsAtDateTime()
        );
    }

    private LessonUpdateCommand updateCommand(LessonFormData form) {
        return new LessonUpdateCommand(
                requiredLong(form.getClassGroupId(), "classGroupId"),
                requiredLong(form.getContentBlockId(), "contentBlockId"),
                nullIfBlank(form.getPhysicalRoomCode()),
                form.getTitle(),
                nullIfBlank(form.getDescription()),
                form.lessonType(),
                nullIfBlank(form.getProvider()),
                nullIfBlank(form.getAccessUrl()),
                form.isAttendanceRequired(),
                form.lessonState(),
                form.startsAtDateTime(),
                form.endsAtDateTime()
        );
    }

    private List<ClassGroup> visibleClassGroups(HttpServletRequest request) {
        try {
            return classGroupDAO.findAll().stream()
                    .sorted(Comparator.comparingLong(ClassGroup::id))
                    .filter(classGroup -> canReadClassGroup(request, classGroup.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
        }
    }

    private List<ClassGroup> manageableClassGroups(HttpServletRequest request) {
        try {
            return classGroupDAO.findAll().stream()
                    .sorted(Comparator.comparingLong(ClassGroup::id))
                    .filter(classGroup -> canManageClassGroup(request, classGroup.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load manageable class groups", exception);
        }
    }

    private Map<Long, List<ContentBlockView>> contentBlockOptionsByClassGroup(List<ClassGroupView> classGroups) {
        Map<Long, List<ContentBlockView>> result = new LinkedHashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            try {
                result.put(
                        classGroup.getId(),
                        contentBlockDAO.findByClassGroup(classGroup.getId()).stream()
                                .map(viewFactory::contentBlockView)
                                .toList()
                );
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load content block options", exception);
            }
        }
        return result;
    }

    private List<PhysicalRoomView> roomOptionsForClassGroups(List<ClassGroupView> classGroups) {
        List<Long> organizationIds = classGroups.stream()
                .map(classGroup -> classGroup.getCourse().getOrganizationId())
                .distinct()
                .toList();
        List<PhysicalRoom> rooms = new ArrayList<>();
        for (Long organizationId : organizationIds) {
            try {
                rooms.addAll(physicalRoomDAO.findByOrganization(organizationId).stream()
                        .filter(room -> room.state() == PhysicalRoomState.ACTIVE)
                        .toList());
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load physical room options", exception);
            }
        }
        return viewFactory.physicalRoomViews(rooms.stream()
                .sorted(Comparator.comparing(PhysicalRoom::code))
                .toList());
    }

    private List<LessonContextOptionView> lessonContextOptions(List<ClassGroupView> classGroups) {
        Map<Long, LessonContextOptionView> organizations = new LinkedHashMap<>();
        Map<Long, LessonContextOptionView> organicUnits = new LinkedHashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            CourseView course = classGroup.getCourse();
            organizations.putIfAbsent(course.getOrganizationId(), LessonContextOptionView.organization(course));
            if (course.getOrganicUnitId() != null) {
                organicUnits.putIfAbsent(course.getOrganicUnitId(), LessonContextOptionView.organicUnit(course));
            }
        }
        for (LessonContextOptionView organization : organizations.values()) {
            try {
                organicUnitDAO.findByOrganization(Long.parseLong(organization.getOrganizationId())).stream()
                        .filter(unit -> unit.state() == OrganicUnitState.ACTIVE)
                        .forEach(unit -> organicUnits.putIfAbsent(
                                unit.id(),
                                LessonContextOptionView.organicUnit(unit, organization)
                        ));
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load lesson organic unit context options", exception);
            }
        }
        List<LessonContextOptionView> result = new ArrayList<>();
        for (LessonContextOptionView organization : organizations.values()) {
            result.add(organization);
            organicUnits.values().stream()
                    .filter(unit -> unit.getOrganizationId().equals(organization.getOrganizationId()))
                    .sorted(Comparator.comparing(LessonContextOptionView::getSortLabel, String.CASE_INSENSITIVE_ORDER))
                    .forEach(result::add);
        }
        return result;
    }

    private ClassGroup requireClassGroup(long classGroupId) {
        try {
            return classGroupDAO.findById(classGroupId)
                    .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group", exception);
        }
    }

    private ContentBlock requireContentBlock(long contentBlockId) {
        try {
            return contentBlockDAO.findById(contentBlockId)
                    .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load content block", exception);
        }
    }

    private PhysicalRoomView physicalRoomView(String code) {
        try {
            return physicalRoomDAO.findByCode(code).map(viewFactory::physicalRoomView).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load physical room", exception);
        }
    }

    private boolean canReadClassGroup(HttpServletRequest request, long classGroupId) {
        SessionUser actor = requireCurrentUser(request);
        try {
            return classGroupService.canReadClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean canManageClassGroup(HttpServletRequest request, long classGroupId) {
        SessionUser actor = requireCurrentUser(request);
        try {
            return classGroupService.canModifyClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Map<Long, ClassGroupView> classGroupMap(List<ClassGroupView> classGroups) {
        Map<Long, ClassGroupView> result = new HashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            result.put(classGroup.getId(), classGroup);
        }
        return result;
    }

    private Map<Long, Boolean> canManageClassGroupMap(
            HttpServletRequest request,
            List<ClassGroupView> classGroups
    ) {
        Map<Long, Boolean> result = new HashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            result.put(classGroup.getId(), canManageClassGroup(request, classGroup.getId()));
        }
        return result;
    }

    private static List<SelectOptionView> lessonTypeOptions(LessonFormData form) {
        return Arrays.stream(LessonType.values())
                .map(type -> new SelectOptionView(type.name(), type.toDatabaseValue(), form.isTypeSelected(type.name())))
                .toList();
    }

    private static List<SelectOptionView> providerOptions(LessonFormData form) {
        return List.of(
                new SelectOptionView("Zoom", "Zoom", form.isProviderSelected("Zoom")),
                new SelectOptionView("Teams", "Teams", form.isProviderSelected("Teams")),
                new SelectOptionView("Meet", "Meet", form.isProviderSelected("Meet"))
        );
    }

    private static String calendarFilter(HttpServletRequest request) {
        String value = text(request, "contentType");
        if (value == null || value.isBlank()) {
            return "all";
        }
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "lessons" -> "lessons";
            case "class_group_enrollments" -> "class_group_enrollments";
            case "course_enrollments" -> "course_enrollments";
            case "attendance" -> "attendance";
            case "absence_justifications" -> "absence_justifications";
            case "assessments" -> "assessments";
            case "grade_sheets" -> "grade_sheets";
            case "certificates" -> "certificates";
            case "class_groups" -> "class_groups";
            case "attempts" -> "attempts";
            case "teacher_assignments" -> "teacher_assignments";
            case "subject_coordination" -> "subject_coordination";
            case "organization_management" -> "organization_management";
            case "deletion_requests" -> "deletion_requests";
            case "users" -> "users";
            default -> "all";
        };
    }

    private static boolean calendarFilterShowsLessons(String filter) {
        return "all".equals(filter) || "lessons".equals(filter);
    }

    private static List<SelectOptionView> calendarFilterOptions(String selected) {
        return List.of(
                new SelectOptionView("all", "All", "all".equals(selected)),
                new SelectOptionView("lessons", "Lessons", "lessons".equals(selected)),
                new SelectOptionView("assessments", "Assessments", "assessments".equals(selected)),
                new SelectOptionView("attendance", "Attendance", "attendance".equals(selected)),
                new SelectOptionView("absence_justifications", "Absence Justifications", "absence_justifications".equals(selected)),
                new SelectOptionView("grade_sheets", "Grade Sheets", "grade_sheets".equals(selected)),
                new SelectOptionView("certificates", "Certificates", "certificates".equals(selected)),
                new SelectOptionView("class_group_enrollments", "Class Group Enrollments", "class_group_enrollments".equals(selected)),
                new SelectOptionView("course_enrollments", "Course Enrollments", "course_enrollments".equals(selected)),
                new SelectOptionView("class_groups", "Class Groups", "class_groups".equals(selected)),
                new SelectOptionView("attempts", "Assessment Attempts", "attempts".equals(selected)),
                new SelectOptionView("teacher_assignments", "Teacher Assignments", "teacher_assignments".equals(selected)),
                new SelectOptionView("subject_coordination", "Subject Coordination", "subject_coordination".equals(selected)),
                new SelectOptionView("organization_management", "Organization Management", "organization_management".equals(selected)),
                new SelectOptionView("deletion_requests", "Deletion Requests", "deletion_requests".equals(selected)),
                new SelectOptionView("users", "Users", "users".equals(selected))
        );
    }

    private static boolean categoryAllowed(String selectedFilter, String category) {
        return "all".equals(selectedFilter) || category.equals(selectedFilter);
    }

    private static String enrollmentStateLabel(EnrollmentState state) {
        return switch (state) {
            case PENDING -> "Pending";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
            case WITHDRAWN -> "Withdrawn";
        };
    }

    private static String enrollmentStateBadge(EnrollmentState state) {
        return switch (state) {
            case PENDING -> "bg-warning-50 text-warning-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
            case WITHDRAWN -> "bg-neutral-30 text-neutral-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-info-50 text-info-600";
        };
    }

    private static String attendanceStatusLabel(AttendanceRecord record) {
        return switch (record.status()) {
            case PRESENT -> "Present";
            case ABSENT -> "Absent";
            case JUSTIFIED -> "Justified";
            case LATE -> "Late";
            case PARTIAL -> "Partial";
        };
    }

    private static String attendanceStateBadge(pt.isel.gape.learning.model.AttendanceState state) {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case CORRECTED -> "bg-info-50 text-info-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    private static String gradeSheetStateLabel(GradeSheetState state) {
        return switch (state) {
            case DRAFT -> "Draft";
            case PUBLISHED -> "Published";
            case CLOSED -> "Closed";
            case INACTIVE -> "Inactive";
        };
    }

    private static String gradeSheetStateBadge(GradeSheetState state) {
        return switch (state) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case PUBLISHED -> "bg-success-50 text-success-600";
            case CLOSED -> "bg-info-50 text-info-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
        };
    }

    private static void prepareLessonContext(
            HttpServletRequest request,
            long lessonId,
            String activeChild,
            String basePath
    ) {
        request.setAttribute("learningLessonContextId", lessonId);
        request.setAttribute("learningLessonContextBasePath", basePath);
        request.setAttribute("learningLessonActiveChild", activeChild);
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static int positiveInteger(HttpServletRequest request, String name, int defaultValue) {
        String value = text(request, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static int pageCount(int totalCount, int pageSize) {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }

    private static int pageStart(int totalCount, int currentPage, int pageSize) {
        if (totalCount == 0) {
            return 0;
        }
        return ((currentPage - 1) * pageSize) + 1;
    }

    private static int pageEnd(int totalCount, int currentPage, int pageSize) {
        if (totalCount == 0) {
            return 0;
        }
        return Math.min(totalCount, currentPage * pageSize);
    }

    private static <T> List<T> pageItems(List<T> items, int currentPage, int pageSize) {
        if (items.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.min(items.size(), (currentPage - 1) * pageSize);
        int toIndex = Math.min(items.size(), fromIndex + pageSize);
        return items.subList(fromIndex, toIndex);
    }

    private static long requiredLong(Long value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class LessonContextOptionView {
        private final String kind;
        private final String value;
        private final String organizationId;
        private final String organizationLabel;
        private final String organizationAcronym;
        private final String organicUnitId;
        private final String organicUnitLabel;
        private final String organicUnitAcronym;
        private final String label;
        private final String title;

        private LessonContextOptionView(
                String kind,
                String value,
                long organizationId,
                String organizationLabel,
                String organizationAcronym,
                String organicUnitId,
                String organicUnitLabel,
                String organicUnitAcronym,
                String label,
                String title
        ) {
            this.kind = kind;
            this.value = value;
            this.organizationId = Long.toString(organizationId);
            this.organizationLabel = organizationLabel == null ? "" : organizationLabel;
            this.organizationAcronym = organizationAcronym == null ? "" : organizationAcronym;
            this.organicUnitId = organicUnitId == null ? "" : organicUnitId;
            this.organicUnitLabel = organicUnitLabel == null ? "" : organicUnitLabel;
            this.organicUnitAcronym = organicUnitAcronym == null ? "" : organicUnitAcronym;
            this.label = label == null || label.isBlank() ? "-" : label;
            this.title = title == null || title.isBlank() ? this.label : title;
        }

        static LessonContextOptionView organization(CourseView course) {
            return new LessonContextOptionView(
                    "organization",
                    "context:organization:" + course.getOrganizationId(),
                    course.getOrganizationId(),
                    course.getOrganizationName(),
                    course.getOrganizationAcronym(),
                    "",
                    "",
                    "",
                    compactPart(course.getOrganizationAcronym(), course.getOrganizationName()),
                    course.getOrganizationName()
            );
        }

        static LessonContextOptionView organicUnit(CourseView course) {
            return new LessonContextOptionView(
                    "organicUnit",
                    "context:organicUnit:" + course.getOrganicUnitId(),
                    course.getOrganizationId(),
                    course.getOrganizationName(),
                    course.getOrganizationAcronym(),
                    Long.toString(course.getOrganicUnitId()),
                    course.getOrganicUnitLabel(),
                    course.getOrganicUnitAcronym(),
                    compactPart(course.getOrganicUnitAcronym(), course.getOrganicUnitLabel()),
                    course.getOrganicUnitLabel() + " | " + course.getOrganizationName()
            );
        }

        static LessonContextOptionView organicUnit(OrganicUnit unit, LessonContextOptionView organization) {
            return new LessonContextOptionView(
                    "organicUnit",
                    "context:organicUnit:" + unit.id(),
                    unit.organizationId(),
                    organization.getOrganizationLabel(),
                    organization.getOrganizationAcronym(),
                    Long.toString(unit.id()),
                    unit.name(),
                    unit.acronym(),
                    compactPart(unit.acronym(), unit.name()),
                    unit.name() + " | " + organization.getOrganizationLabel()
            );
        }

        public String getKind() {
            return kind;
        }

        public String getValue() {
            return value;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public String getOrganizationLabel() {
            return organizationLabel;
        }

        public String getOrganizationAcronym() {
            return organizationAcronym;
        }

        public String getOrganicUnitId() {
            return organicUnitId;
        }

        public String getOrganicUnitLabel() {
            return organicUnitLabel;
        }

        public String getOrganicUnitAcronym() {
            return organicUnitAcronym;
        }

        public String getLabel() {
            return label;
        }

        public String getTitle() {
            return title;
        }

        public String getSortLabel() {
            return title;
        }

        private static String compactPart(String acronym, String name) {
            if (acronym != null && !acronym.isBlank() && !"-".equals(acronym)) {
                return acronym;
            }
            return name == null || name.isBlank() ? "-" : name;
        }
    }

    private enum LessonAction {
        CANCEL,
        COMPLETE
    }
}
