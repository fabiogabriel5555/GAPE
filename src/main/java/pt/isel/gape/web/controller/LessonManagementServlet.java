package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.PhysicalRoomDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.LessonUpdateCommand;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.ScheduleEventCreateCommand;
import pt.isel.gape.learning.model.ScheduleEventState;
import pt.isel.gape.learning.model.ScheduleEventType;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.ScheduleEventService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.LessonFormData;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.ScheduleEventView;
import pt.isel.gape.web.view.SelectOptionView;

@WebServlet(name = "lessonManagementServlet", urlPatterns = {
        "/learning/lessons",
        "/learning/lessons/*",
        "/learning/calendar"
})
public final class LessonManagementServlet extends DashboardServletSupport {

    private static final String LESSON_LIST_JSP = "/WEB-INF/views/learning/lesson-list.jsp";
    private static final String LESSON_DETAIL_JSP = "/WEB-INF/views/learning/lesson-detail.jsp";
    private static final String LESSON_FORM_JSP = "/WEB-INF/views/learning/lesson-form.jsp";

    private final LessonService lessonService;
    private final ClassGroupService classGroupService;
    private final ClassGroupDAO classGroupDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final PhysicalRoomDAO physicalRoomDAO;
    private final ScheduleEventService scheduleEventService;
    private final LearningViewFactory viewFactory;
    private final ScheduleAttendanceViewFactory scheduleViewFactory;

    public LessonManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private LessonManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new LessonService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new ClassGroupDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new PhysicalRoomDAO(connectionProvider),
                new ScheduleEventService(connectionProvider, clock),
                new LearningViewFactory(
                        new OrganizationDAO(connectionProvider),
                        new OrganicUnitDAO(connectionProvider),
                        new CourseDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new CourseSubjectDAO(connectionProvider),
                        new EnrollmentDAO(connectionProvider),
                        new ClassGroupDAO(connectionProvider),
                        new ClassGroupEnrollmentDAO(connectionProvider),
                        new ContentBlockDAO(connectionProvider),
                        new UserDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new ScheduleAttendanceViewFactory(
                        new ClassGroupDAO(connectionProvider),
                        new LessonDAO(connectionProvider),
                        new UserDAO(connectionProvider),
                        new LearningViewFactory(
                                new OrganizationDAO(connectionProvider),
                                new OrganicUnitDAO(connectionProvider),
                                new CourseDAO(connectionProvider),
                                new SubjectDAO(connectionProvider),
                                new CourseSubjectDAO(connectionProvider),
                                new EnrollmentDAO(connectionProvider),
                                new ClassGroupDAO(connectionProvider),
                                new ClassGroupEnrollmentDAO(connectionProvider),
                                new ContentBlockDAO(connectionProvider),
                                new UserDAO(connectionProvider),
                                new TeachClassGroupDAO(connectionProvider)
                        )
                )
        );
    }

    LessonManagementServlet(
            LessonService lessonService,
            ClassGroupService classGroupService,
            ClassGroupDAO classGroupDAO,
            ContentBlockDAO contentBlockDAO,
            PhysicalRoomDAO physicalRoomDAO,
            ScheduleEventService scheduleEventService,
            LearningViewFactory viewFactory,
            ScheduleAttendanceViewFactory scheduleViewFactory
    ) {
        this.lessonService = lessonService;
        this.classGroupService = classGroupService;
        this.classGroupDAO = classGroupDAO;
        this.contentBlockDAO = contentBlockDAO;
        this.physicalRoomDAO = physicalRoomDAO;
        this.scheduleEventService = scheduleEventService;
        this.viewFactory = viewFactory;
        this.scheduleViewFactory = scheduleViewFactory;
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
            if (segments.length == 1 && "new".equals(segments[0])) {
                showCreateForm(request, response, null);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "edit".equals(segments[1])) {
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
                if (segments.length == 0) {
                    createScheduleEvent(request, response);
                    return;
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
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
        List<LessonView> lessons = calendarMode
                ? personalCalendarLessons(request)
                : managedLessons(request);
        List<ScheduleEventView> scheduleEvents = calendarMode ? visibleScheduleEvents(request) : List.of();
        List<ClassGroupView> classGroupOptions = calendarMode
                ? classGroupOptionsFromCalendar(lessons, scheduleEvents)
                : viewFactory.classGroupViews(visibleClassGroups(request));
        if (calendarMode && !calendarFilterShowsLessons(selectedCalendarFilter)) {
            lessons = List.of();
        }
        if (calendarMode && !calendarFilterShowsEvents(selectedCalendarFilter)) {
            scheduleEvents = List.of();
        }
        Map<Long, ClassGroupView> classGroupById = classGroupMap(classGroupOptions);

        if (classGroupFilter != null) {
            lessons = lessons.stream()
                    .filter(lesson -> lesson.getClassGroupId() == classGroupFilter)
                    .toList();
            scheduleEvents = scheduleEvents.stream()
                    .filter(event -> event.getClassGroupIds().contains(classGroupFilter))
                    .toList();
        }

        lessons = lessons.stream()
                .sorted(Comparator.comparing(LessonView::getStartsAtRaw).thenComparingLong(LessonView::getId))
                .toList();
        scheduleEvents = scheduleEvents.stream()
                .sorted(Comparator.comparing(ScheduleEventView::getStartsAtRaw).thenComparingLong(ScheduleEventView::getId))
                .toList();

        request.setAttribute("lessons", lessons);
        request.setAttribute("scheduleEvents", scheduleEvents);
        request.setAttribute("classGroupOptions", classGroupOptions);
        request.setAttribute("manageableClassGroupOptions", viewFactory.classGroupViews(manageableClassGroups(request)));
        request.setAttribute("classGroupById", classGroupById);
        request.setAttribute("canManageClassGroupById", canManageClassGroupMap(request, classGroupOptions));
        request.setAttribute("selectedClassGroupId", classGroupFilter);
        request.setAttribute("selectedCalendarFilter", selectedCalendarFilter);
        request.setAttribute("calendarFilterOptions", calendarFilterOptions(selectedCalendarFilter));
        request.setAttribute("calendarMode", calendarMode);
        request.setAttribute("lessonCount", lessons.size());
        request.setAttribute("scheduleEventCount", scheduleEvents.size());
        request.setAttribute("calendarItemCount", lessons.size() + scheduleEvents.size());
        request.setAttribute("activeLessonCount", lessons.stream().filter(LessonView::isActive).count());
        request.setAttribute("scheduledLessonCount", lessons.stream().filter(LessonView::isScheduled).count());
        prepareDashboard(
                request,
                calendarMode ? "calendar" : "lessons",
                calendarMode ? "Calendar" : "Lessons",
                calendarMode || manageableClassGroups(request).isEmpty() ? null : "/learning/lessons/new",
                calendarMode || manageableClassGroups(request).isEmpty() ? null : "New Lesson"
        );
        forward(request, response, LESSON_LIST_JSP);
    }

    private List<ScheduleEventView> visibleScheduleEvents(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        return scheduleEventService.listVisibleEvents(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        request.getRemoteAddr()
                )
                .stream()
                .map(scheduleViewFactory::scheduleEventView)
                .toList();
    }

    private List<LessonView> managedLessons(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        List<LessonView> lessons = new ArrayList<>();
        for (ClassGroup classGroup : visibleClassGroups(request)) {
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

    private List<ClassGroupView> classGroupOptionsFromCalendar(
            List<LessonView> lessons,
            List<ScheduleEventView> scheduleEvents
    ) {
        Map<Long, ClassGroupView> classGroupsById = new LinkedHashMap<>();
        for (LessonView lesson : lessons) {
            classGroupsById.computeIfAbsent(lesson.getClassGroupId(), this::calendarClassGroupView);
        }
        for (ScheduleEventView event : scheduleEvents) {
            for (Long classGroupId : event.getClassGroupIds()) {
                classGroupsById.computeIfAbsent(classGroupId, this::calendarClassGroupView);
            }
        }
        return classGroupsById.values().stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ClassGroupView calendarClassGroupView(long classGroupId) {
        try {
            return classGroupDAO.findById(classGroupId)
                    .map(viewFactory::classGroupView)
                    .orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load calendar class group", exception);
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
        request.setAttribute("lessonBackHref", backHref(request, "/learning/lessons"));
        request.setAttribute("lessonEditHref", request.getContextPath()
                + appendReturnTo("/learning/lessons/" + lesson.id() + "/edit", currentRequestPath(request)));
        prepareLessonContext(request, lesson.id(), "detail", "/learning/lessons");
        prepareDashboard(request, "lessons", "Lesson Detail");
        forward(request, response, LESSON_DETAIL_JSP);
    }

    private static boolean isCalendarRequest(HttpServletRequest request) {
        return "/learning/calendar".equals(request.getServletPath());
    }

    private void createScheduleEvent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            scheduleEventService.createEvent(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    createScheduleCommand(request),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Schedule event created.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/calendar");
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
        request.setAttribute("contentBlocksByClassGroup", blocksByClassGroup);
        request.setAttribute("physicalRoomOptions", roomOptions);
        request.setAttribute("lessonTypes", lessonTypeOptions(form));
        request.setAttribute("providerOptions", providerOptions(form));
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
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
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

    private ScheduleEventCreateCommand createScheduleCommand(HttpServletRequest request) {
        boolean reminderEnabled = request.getParameter("reminderEnabled") != null;
        return new ScheduleEventCreateCommand(
                optionalLong(request, "lessonId"),
                optionalLong(request, "assessmentId"),
                text(request, "title"),
                nullIfBlank(text(request, "description")),
                ScheduleEventType.parse(defaultText(request, "type", "other")),
                dateTimeParameter(request, "startsAt"),
                dateTimeParameter(request, "endsAt"),
                request.getParameter("allDay") != null,
                reminderEnabled,
                reminderEnabled ? optionalInteger(request, "reminderMinutesBefore") : null,
                ScheduleEventState.parse(defaultText(request, "state", "active")),
                longList(request.getParameterValues("classGroupIds"))
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
                                .filter(block -> block.state() != ContentBlockState.INACTIVE)
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
            case "events" -> "events";
            default -> "all";
        };
    }

    private static boolean calendarFilterShowsLessons(String filter) {
        return "all".equals(filter) || "lessons".equals(filter);
    }

    private static boolean calendarFilterShowsEvents(String filter) {
        return "all".equals(filter) || "events".equals(filter);
    }

    private static List<SelectOptionView> calendarFilterOptions(String selected) {
        return List.of(
                new SelectOptionView("all", "All", "all".equals(selected)),
                new SelectOptionView("lessons", "Lessons", "lessons".equals(selected)),
                new SelectOptionView("events", "Events", "events".equals(selected))
        );
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

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static LocalDateTime dateTimeParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return LocalDateTime.parse(value);
    }

    private static List<Long> longList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }

    private static String defaultText(HttpServletRequest request, String name, String fallback) {
        String value = text(request, name);
        return value == null ? fallback : value;
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

    private enum LessonAction {
        CANCEL,
        COMPLETE
    }
}
