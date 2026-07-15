package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupCreateCommand;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ClassGroupUpdateCommand;
import pt.isel.gape.learning.model.BlockContentPlacement;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.ContentDeletionResult;
import pt.isel.gape.learning.model.ContentRemovalResult;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.service.ClassGroupEnrollmentService;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.ContentBlockService;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.learning.service.CertificateService;
import pt.isel.gape.learning.service.GradeCertificateReadService;
import pt.isel.gape.learning.service.GradeRecordService;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.learning.service.PhysicalRoomService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.RoleAssignmentState;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.BlockActivityView;
import pt.isel.gape.web.view.BlockContentItemView;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupFormData;
import pt.isel.gape.web.view.ClassGroupOccurrenceGroupView;
import pt.isel.gape.web.view.ClassGroupTeacherView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockFormData;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.ContentRepositoryItemView;
import pt.isel.gape.web.view.CourseOccurrenceView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.GradeSheetView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.SubjectView;

@WebServlet(name = "classGroupManagementServlet", urlPatterns = {"/learning/class-groups", "/learning/class-groups/*"})
@MultipartConfig(maxFileSize = 1024L * 1024L, maxRequestSize = 2L * 1024L * 1024L)
public final class ClassGroupManagementServlet extends DashboardServletSupport {

    private static final String ADMIN_CLASS_GROUP_LIST_JSP = "/admin/admin/class-group/admin-class-groups.jsp";
    private static final String ADMIN_CLASS_GROUP_DETAIL_JSP = "/admin/admin/class-group/admin-class-group-detail.jsp";
    private static final String ADMIN_CLASS_GROUP_FORM_JSP = "/admin/admin/class-group/admin-class-group-form.jsp";
    private static final String ADMIN_CONTENT_BLOCK_FORM_JSP = "/admin/admin/class-group/admin-content-block-form.jsp";
    private static final String COORDINATOR_CLASS_GROUP_LIST_JSP =
            "/coordinator/coordinator/class-group/coordinator-class-groups.jsp";
    private static final String COORDINATOR_CLASS_GROUP_DETAIL_JSP =
            "/coordinator/coordinator/class-group/coordinator-class-group-detail.jsp";
    private static final String COORDINATOR_CLASS_GROUP_FORM_JSP =
            "/coordinator/coordinator/class-group/coordinator-class-group-form.jsp";
    private static final String COORDINATOR_CONTENT_BLOCK_FORM_JSP =
            "/coordinator/coordinator/class-group/coordinator-content-block-form.jsp";
    private static final String INSTRUCTOR_CLASS_GROUP_LIST_JSP =
            "/instructor/instructor/class-group/instructor-class-groups.jsp";
    private static final String INSTRUCTOR_CLASS_GROUP_DETAIL_JSP =
            "/instructor/instructor/class-group/instructor-class-group-detail.jsp";
    private static final String INSTRUCTOR_CLASS_GROUP_FORM_JSP =
            "/instructor/instructor/class-group/instructor-class-group-form.jsp";
    private static final String INSTRUCTOR_CONTENT_BLOCK_FORM_JSP =
            "/instructor/instructor/class-group/instructor-content-block-form.jsp";
    private static final String CLASS_GROUP_ENROLLMENTS_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-enrollments-panel.jsp";
    private static final String CLASS_GROUP_TEACHERS_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-teachers-panel.jsp";
    private static final String CLASS_GROUP_GRADE_SHEET_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-grade-sheet-panel.jsp";
    private static final String CLASS_GROUP_LIST_ROWS_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-list-rows.jsp";
    private static final String CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-activities-panel.jspf";
    private static final int LIST_PAGE_SIZE = 10;

    private final ClassGroupService classGroupService;
    private final ClassGroupEnrollmentService enrollmentService;
    private final ContentBlockService contentBlockService;
    private final ContentAssociationService contentAssociationService;
    private final ContentItemService contentItemService;
    private final LessonService lessonService;
    private final PhysicalRoomService roomService;
    private final PdfUploadService pdfUploadService;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.EnrollmentApprovalPolicies enrollmentApprovalPolicyDAO;
    private final ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO;
    private final ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO;
    private final ApplicationReadService.Attempts attemptDAO;
    private final ApplicationReadService.PhysicalRooms physicalRoomDAO;
    private final LearningViewFactory viewFactory;
    private final ApplicationReadService.Assessments assessmentDAO;
    private final AssessmentViewFactory assessmentViewFactory;
    private final GradeCertificateServlet gradeCertificateServlet;

    private record SubjectCreationContext(
            SubjectView subject,
            Map<Long, CourseSubjectView> associationsByCourseId,
            Map<Long, Boolean> selectableCourseById
    ) {
    }

    public ClassGroupManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private ClassGroupManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new ClassGroupService(connectionProvider, clock),
                new ClassGroupEnrollmentService(connectionProvider, clock),
                new ContentBlockService(connectionProvider, clock),
                new ContentAssociationService(connectionProvider, clock),
                new ContentItemService(connectionProvider, clock),
                new LessonService(connectionProvider, clock),
                new PhysicalRoomService(connectionProvider, clock),
                new PdfUploadService(),
                new GradeCertificateServlet(
                        connectionProvider,
                        new GradeSheetService(connectionProvider, clock),
                        new GradeRecordService(connectionProvider, clock),
                        new CertificateService(connectionProvider, clock),
                        new GradeCertificateReadService(connectionProvider),
                        clock
                )
        );
    }

    ClassGroupManagementServlet(
            ApplicationReadService readService,
            ClassGroupService classGroupService,
            ClassGroupEnrollmentService enrollmentService,
            ContentBlockService contentBlockService,
            ContentAssociationService contentAssociationService,
            ContentItemService contentItemService,
            LessonService lessonService,
            PhysicalRoomService roomService,
            PdfUploadService pdfUploadService,
            GradeCertificateServlet gradeCertificateServlet
    ) {
        this.classGroupService = classGroupService;
        this.enrollmentService = enrollmentService;
        this.contentBlockService = contentBlockService;
        this.contentAssociationService = contentAssociationService;
        this.contentItemService = contentItemService;
        this.lessonService = lessonService;
        this.roomService = roomService;
        this.pdfUploadService = pdfUploadService;
        this.classGroupDAO = readService.classGroups();
        this.courseDAO = readService.courses();
        this.courseSubjectDAO = readService.courseSubjects();
        this.enrollmentApprovalPolicyDAO = readService.enrollmentApprovalPolicies();
        this.classGroupEnrollmentDAO = readService.classGroupEnrollments();
        this.assessmentEnrollmentDAO = readService.assessmentEnrollments();
        this.attemptDAO = readService.attempts();
        this.physicalRoomDAO = readService.physicalRooms();
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
        this.assessmentDAO = readService.assessments();
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
        this.gradeCertificateServlet = gradeCertificateServlet;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 0) {
                showList(request, response);
                return;
            }
            if (segments.length == 1 && "new".equals(segments[0])) {
                showClassGroupForm(
                        request,
                        response,
                        ClassGroupFormData.blank(optionalLong(request, "courseId"), optionalLong(request, "subjectId")),
                        true,
                        null
                );
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "structure".equals(segments[1])) {
                showStructureFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "enrollments".equals(segments[1])) {
                showEnrollmentsFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "teachers".equals(segments[1])) {
                showTeachersFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "grade-sheet".equals(segments[1])) {
                showGradeSheetFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "activities".equals(segments[1])) {
                showActivitiesFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "edit".equals(segments[1])) {
                showEditForm(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            if (segments.length == 3 && "blocks".equals(segments[1]) && "new".equals(segments[2])) {
                redirect(request, response, "/learning/class-groups/" + Long.parseLong(segments[0]) + "#pedagogical-blocks");
                return;
            }
            if (segments.length == 4 && "blocks".equals(segments[1]) && "edit".equals(segments[3])) {
                redirect(request, response, "/learning/class-groups/" + Long.parseLong(segments[0]) + "#block-"
                        + Long.parseLong(segments[2]));
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
            if (segments.length == 0) {
                createClassGroup(request, response);
                return;
            }
            if (segments.length == 1) {
                updateClassGroup(request, response, Long.parseLong(segments[0]));
                return;
            }
            long classGroupId = Long.parseLong(segments[0]);
            if (!isClassGroupDeletionRequest(segments) && isCompletedClassGroup(request, classGroupId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (segments.length == 2) {
                switch (segments[1]) {
                    case "delete" -> deleteClassGroup(request, response, classGroupId);
                    case "teachers" -> assignTeacher(request, response, classGroupId);
                    case "enrollments" -> enrollStudent(request, response, classGroupId);
                    case "enrollment-policy" -> updateEnrollmentPolicy(request, response, classGroupId);
                    case "blocks" -> createContentBlock(request, response, classGroupId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1]) && "approve".equals(segments[3])) {
                approveStudentEnrollment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1]) && "reject".equals(segments[3])) {
                rejectStudentEnrollment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1]) && "withdraw".equals(segments[3])) {
                withdrawStudent(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1]) && "delete".equals(segments[3])) {
                deleteStudentEnrollment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "teachers".equals(segments[1]) && "remove".equals(segments[3])) {
                removeTeacher(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "teachers".equals(segments[1]) && "update".equals(segments[3])) {
                updateTeacherAssignment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 3 && "blocks".equals(segments[1]) && "reorder".equals(segments[2])) {
                reorderContentBlocks(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 4
                    && "blocks".equals(segments[1])
                    && "contents".equals(segments[2])
                    && "reorder".equals(segments[3])) {
                reorderBlockContents(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 6
                    && "blocks".equals(segments[1])
                    && "contents".equals(segments[3])
                    && ("delete".equals(segments[5]) || "remove".equals(segments[5]))) {
                deleteBlockContent(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2]),
                        Long.parseLong(segments[4])
                );
                return;
            }
            if (segments.length == 3 && "blocks".equals(segments[1])) {
                updateContentBlock(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "blocks".equals(segments[1])) {
                long contentBlockId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "lessons" -> createBlockLesson(request, response, classGroupId, contentBlockId);
                    case "delete" -> deleteContentBlock(request, response, classGroupId, contentBlockId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (isListRowsFragmentRequest(request)) {
            prepareClassGroupListRows(request, requestedListOffset(request), requestedListLoadAll(request));
            forward(request, response, CLASS_GROUP_LIST_ROWS_FRAGMENT_JSP);
            return;
        }
        prepareClassGroupListRows(request, 0, false);
        prepareDashboard(
                request,
                "class-groups",
                "Class Groups",
                canCreateClassGroups(request) ? "/learning/class-groups/new" : null,
                canCreateClassGroups(request) ? "New Class Group" : null
        );
        forward(request, response, classGroupListJsp(request));
    }

    /**
     * Supplies a small, independently refreshable page of rows.  Activities are deliberately
     * excluded here and are fetched only when a row is opened.
     */
    private void prepareClassGroupListRows(
            HttpServletRequest request,
            int requestedOffset,
            boolean loadAll
    ) {
        Long courseFilter = optionalLong(request, "courseId");
        Long subjectFilter = optionalLong(request, "subjectId");
        List<ClassGroup> visible = visibleClassGroups(request).stream()
                .filter(classGroup -> courseFilter == null || classGroup.courseId() == courseFilter)
                .filter(classGroup -> subjectFilter == null || classGroup.subjectId() == subjectFilter)
                .sorted(Comparator.comparingLong(ClassGroup::id).reversed())
                .toList();
        int total = visible.size();
        int fromIndex = Math.min(Math.max(0, requestedOffset), total);
        int toIndex = loadAll ? total : Math.min(fromIndex + LIST_PAGE_SIZE, total);
        List<ClassGroup> pageClassGroups = visible.subList(fromIndex, toIndex);
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(pageClassGroups);
        request.setAttribute("classGroups", classGroups);
        request.setAttribute("classGroupCount", total);
        request.setAttribute("activeClassGroups", visible.stream().filter(classGroup -> classGroup.state() == ClassGroupState.ACTIVE).count());
        request.setAttribute("completedClassGroups", visible.stream().filter(classGroup -> classGroup.state() == ClassGroupState.COMPLETED).count());
        request.setAttribute("canCreateClassGroups", canCreateClassGroups(request));
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(request, classGroups));
        request.setAttribute("canManageClassGroupStructureById", canManageClassGroupStructureById(request, classGroups));
        request.setAttribute(
                "classGroupEventCountById",
                manageableEventCountsByClassGroupId(request, classGroups)
        );
        request.setAttribute("classGroupListOffset", fromIndex);
        request.setAttribute("classGroupListCurrentPage", fromIndex / LIST_PAGE_SIZE + 1);
        request.setAttribute("classGroupListLoadAll", loadAll);
        request.setAttribute("classGroupListHasMore", !loadAll && toIndex < total);
        request.setAttribute("classGroupListNextOffset", toIndex);
    }

    private void showActivitiesFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = visibleClassGroup(request, actor, classGroupId);
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        List<ClassGroup> classGroups = List.of(classGroup);
        Map<Long, List<LessonView>> lessonsByClassGroup = classGroupLessonsByClassGroup(request, classGroups);
        Map<Long, List<AssessmentView>> assessmentsByClassGroup = classGroupAssessmentsByClassGroup(classGroups);
        Map<Long, List<PhysicalRoomView>> roomsByClassGroup =
                classGroupRoomsByClassGroup(lessonsByClassGroup, assessmentsByClassGroup);
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("classGroupLessonsByClassGroup", lessonsByClassGroup);
        request.setAttribute("classGroupAssessmentsByClassGroup", assessmentsByClassGroup);
        request.setAttribute("classGroupRoomsByClassGroup", roomsByClassGroup);
        request.setAttribute("canManagePhysicalRoomByCode", canManagePhysicalRoomByCode(request, roomsByClassGroup));
        request.setAttribute("canModifyClassGroup", canModifyClassGroup(request, classGroupView));
        request.setAttribute("currentReturnToParam", "/learning/class-groups");
        forward(request, response, CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws ServletException, IOException {
        // The learning plan is the default view.  Keep it in the first response just like
        // Subject Details does, so the user sees the already-rendered structure immediately.
        showDetail(request, response, classGroupId, true);
    }

    /**
     * The structure is available in the initial response.  The explicit endpoint remains for
     * live refreshes after a structure mutation.
     */
    private void showDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            boolean includeStructure
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = classGroupService.getClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        );
        markLearningEventsReadForCurrentUser(request, "/learning/class-groups/" + classGroup.id(), true);
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        boolean classGroupCompleted = classGroupView.isCompleted();
        boolean canManageClassGroup = canModifyClassGroup(request, classGroupView);
        boolean canDeleteClassGroup = canManageClassGroupStructure(request, classGroupView);
        boolean canManageClassGroupStructure = !classGroupCompleted && canDeleteClassGroup;
        boolean canManageClassGroupEnrollments = canManageClassGroupEnrollments(request, classGroupView);
        boolean canManageTeacherAssignments = canManageClassGroupStructure
                && primaryProfile(actor) != AccessProfileType.TEACHER;
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("classGroupDetailStructureLoaded", includeStructure);
        if (includeStructure) {
            prepareClassGroupStructureAttributes(
                    request,
                    actor,
                    classGroupView,
                    canManageClassGroup,
                    canManageClassGroupStructure
            );
        }
        request.setAttribute(
                "classGroupAssessmentWeightWarning",
                includeStructure ? classGroupAssessmentWeightWarning(classGroupId) : null
        );
        request.setAttribute("canManageClassGroup", canManageClassGroup);
        request.setAttribute("canManageClassGroupContent", !classGroupCompleted && canManageClassGroup);
        request.setAttribute("canManageLessons", !classGroupCompleted && canManageClassGroup);
        request.setAttribute("canManageClassGroupStructure", canManageClassGroupStructure);
        request.setAttribute("canDeleteClassGroup", canDeleteClassGroup);
        request.setAttribute("canManageClassGroupEnrollments", canManageClassGroupEnrollments);
        // This count belongs to the detail card only.  The dashboard attribute with a
        // similarly named purpose is calculated across every manageable class group by
        // DashboardServletSupport, so do not overwrite it here.
        request.setAttribute(
                "classGroupDetailPendingEnrollmentCount",
                canManageClassGroupEnrollments ? pendingEnrollmentCount(classGroupId) : 0
        );
        request.setAttribute("canManageTeacherAssignments", canManageTeacherAssignments);
        request.setAttribute("classGroupGradeSheetAvailable", Boolean.FALSE);
        request.setAttribute("classGroupBackHref", backHref(request, "/learning/class-groups"));
        prepareClassGroupContext(request, classGroupView, "detail");
        prepareDashboard(request, "class-groups", "Class Group Details");
        forward(request, response, classGroupDetailJsp(request));
    }

    private void showStructureFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId
    ) throws ServletException, IOException {
        showDetail(request, response, classGroupId, true);
    }

    private void prepareClassGroupStructureAttributes(
            HttpServletRequest request,
            SessionUser actor,
            ClassGroupView classGroup,
            boolean canManageClassGroup,
            boolean canManageClassGroupStructure
    ) {
        List<ContentBlockView> blocks = List.of();
        String blockContentLoadError = null;
        if (canManageClassGroup || canManageClassGroupStructure) {
            try {
                blocks = contentBlockService.listContentBlocksByClassGroup(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                classGroup.getId(),
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(viewFactory::contentBlockView)
                        .toList();
            } catch (RuntimeException exception) {
                blockContentLoadError = "Could not load pedagogical blocks for this class group.";
            }
        }
        Map<Long, List<BlockContentItemView>> blockContentsByBlock = new LinkedHashMap<>();
        Map<Long, List<LessonView>> blockLessonsByBlock = new LinkedHashMap<>();
        Map<Long, List<AssessmentView>> blockAssessmentsByBlock = new LinkedHashMap<>();
        if (blockContentLoadError == null && !blocks.isEmpty()) {
            try {
                blockContentsByBlock = blockContentsByBlock(actor, blocks, request);
                blockLessonsByBlock = blockLessonsByBlock(actor, blocks, request);
                blockAssessmentsByBlock = blockAssessmentsByBlock(blocks);
            } catch (RuntimeException exception) {
                blockContentLoadError = "Could not load pedagogical contents for this class group.";
            }
        }
        request.setAttribute("contentBlocks", blocks);
        request.setAttribute("blockContentsByBlock", blockContentsByBlock);
        request.setAttribute("blockLessonsByBlock", blockLessonsByBlock);
        request.setAttribute("blockAssessmentsByBlock", blockAssessmentsByBlock);
        Map<Long, Integer> pendingAssessmentEnrollmentCountById =
                pendingAssessmentEnrollmentCounts(blockAssessmentsByBlock);
        Map<Long, Integer> pendingAssessmentCorrectionCountById =
                pendingAssessmentCorrectionCounts(blockAssessmentsByBlock);
        request.setAttribute("classGroupAssessmentPendingEnrollmentCountById", pendingAssessmentEnrollmentCountById);
        request.setAttribute("classGroupAssessmentPendingCorrectionCountById", pendingAssessmentCorrectionCountById);
        request.setAttribute(
                "classGroupDetailPendingCorrectionCount",
                pendingAssessmentCorrectionCountById.values().stream().mapToInt(Integer::intValue).sum()
        );
        request.setAttribute("blockActivitiesByBlock", blockActivitiesByBlock(
                blocks,
                blockContentsByBlock,
                blockLessonsByBlock,
                blockAssessmentsByBlock
        ));
        request.setAttribute("blockContentLoadSuccess", blockContentLoadError == null);
        request.setAttribute("blockContentLoadError", blockContentLoadError);
        request.setAttribute("blockContentCount", blockContentsByBlock.values().stream().mapToInt(List::size).sum());
        request.setAttribute("lessonCount", blockLessonsByBlock.values().stream().mapToInt(List::size).sum());
        request.setAttribute("assessmentCount", blockAssessmentsByBlock.values().stream().mapToInt(List::size).sum());
        request.setAttribute(
                "pdfContentCount",
                blockContentsByBlock.values().stream().flatMap(List::stream).filter(BlockContentItemView::isPdf).count()
        );
        List<ContentRepositoryItemView> contentRepository = List.of();
        if (canManageClassGroupStructure) {
            try {
                contentRepository = contentItemService.listReusableFileBackedContent(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                ContentAssociationType.CLASS_GROUP,
                                classGroup.getId(),
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(ContentRepositoryItemView::from)
                        .toList();
            } catch (RuntimeException ignored) {
                contentRepository = List.of();
            }
        }
        request.setAttribute("contentRepository", contentRepository);
        request.setAttribute("physicalRoomOptions", physicalRoomOptions(classGroup));
    }

    private void showEnrollmentsFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = visibleClassGroup(request, actor, classGroupId);
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        List<ClassGroupEnrollmentView> enrollments = viewFactory.classGroupEnrollmentViews(classGroupId);

        request.setAttribute("classGroup", classGroupView);
        prepareClassGroupEnrollmentAttributes(request, classGroupView, enrollments);
        request.setAttribute("classGroupEnrollmentPolicy", classGroupEnrollmentPolicy(classGroupId));
        request.setAttribute("activeEnrollmentByStudent", activeEnrollmentByStudent(enrollments));
        request.setAttribute(
                "studentOptions",
                canManageClassGroupEnrollments(request, classGroupView)
                        ? viewFactory.eligibleStudentOptions(classGroup.courseId(), classGroup.subjectId())
                        : List.of()
        );
        prepareClassGroupFragment(request, classGroupView);
        forward(request, response, CLASS_GROUP_ENROLLMENTS_FRAGMENT_JSP);
    }

    private void showTeachersFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = visibleClassGroup(request, actor, classGroupId);
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        boolean canManageTeacherAssignments = !classGroupView.isCompleted()
                && canManageClassGroupStructure(request, classGroupView)
                && primaryProfile(actor) != AccessProfileType.TEACHER;

        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("classGroupTeachers", viewFactory.classGroupTeacherViews(classGroupId));
        request.setAttribute("canManageTeacherAssignments", canManageTeacherAssignments);
        request.setAttribute(
                "teacherOptions",
                canManageTeacherAssignments ? viewFactory.activeTeacherOptions(null) : List.of()
        );
        prepareClassGroupFragment(request, classGroupView);
        forward(request, response, CLASS_GROUP_TEACHERS_FRAGMENT_JSP);
    }

    private void showGradeSheetFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = visibleClassGroup(request, actor, classGroupId);
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        GradeSheetView classGroupGradeSheet = classGroupGradeSheet(request, actor, classGroupId);

        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("classGroupGradeSheet", classGroupGradeSheet);
        request.setAttribute("classGroupGradeDocument",
                classGroupGradeSheet == null ? null : classGroupGradeSheet.getDocument());
        request.setAttribute("classGroupGradeSheetAvailable", classGroupGradeSheet != null);
        prepareClassGroupFragment(request, classGroupView);
        forward(request, response, CLASS_GROUP_GRADE_SHEET_FRAGMENT_JSP);
    }

    private ClassGroup visibleClassGroup(HttpServletRequest request, SessionUser actor, long classGroupId) {
        return classGroupService.getClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        );
    }

    private boolean isCompletedClassGroup(HttpServletRequest request, long classGroupId) {
        SessionUser actor = requireCurrentUser(request);
        return visibleClassGroup(request, actor, classGroupId).state() == ClassGroupState.COMPLETED;
    }

    private static boolean isClassGroupDeletionRequest(String[] segments) {
        return segments.length == 2 && "delete".equals(segments[1]);
    }

    private void prepareClassGroupFragment(HttpServletRequest request, ClassGroupView classGroup) {
        request.setAttribute("currentReturnTo", "/learning/class-groups/" + classGroup.getId());
        request.setAttribute("mediaCacheVersion", Long.toString(System.currentTimeMillis()));
    }

    private GradeSheetView classGroupGradeSheet(HttpServletRequest request, SessionUser actor, long classGroupId)
            throws ServletException {
        try {
            return gradeCertificateServlet.visibleClassGroupGradeSheet(
                    actor,
                    primaryProfile(actor),
                    request.getRemoteAddr(),
                    classGroupId
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to load class group grade sheet", exception);
        }
    }

    private Map<Long, List<BlockContentItemView>> blockContentsByBlock(
            SessionUser actor,
            List<ContentBlockView> blocks,
            HttpServletRequest request
    ) {
        Map<Long, List<BlockContentItemView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : blocks) {
            result.put(
                    block.getId(),
                    contentAssociationService.listBlockContentItems(
                                    actor.userId(),
                                    currentSessionId(request),
                                    primaryProfile(actor),
                                    block.getId(),
                                    request.getRemoteAddr()
                            )
                            .stream()
                            .map(BlockContentItemView::from)
                            .toList()
            );
        }
        return result;
    }

    private String classGroupAssessmentWeightWarning(long classGroupId) {
        try {
            var summary =
                    assessmentDAO.summarizeAssessmentWeightsForClassGroup(classGroupId);
            if (summary == null || !summary.hasAssessments() || summary.totalIsOneHundred()) {
                return null;
            }
            return "Assessment weights total " + percentageLabel(summary.totalWeight())
                    + "%. If this is not regularized, when the class group period ends the system will redistribute "
                    + "the weights equally so the sum is 100%.";
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group assessment weight summary", exception);
        }
    }

    private static String percentageLabel(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private Map<Long, List<LessonView>> classGroupLessonsByClassGroup(
            HttpServletRequest request,
            List<ClassGroup> classGroups
    ) {
        SessionUser actor = requireCurrentUser(request);
        Map<Long, List<LessonView>> result = new LinkedHashMap<>();
        for (ClassGroup classGroup : classGroups) {
            try {
                result.put(
                        classGroup.id(),
                        lessonService.listLessonsByClassGroup(
                                        actor.userId(),
                                        currentSessionId(request),
                                        primaryProfile(actor),
                                        classGroup.id(),
                                        request.getRemoteAddr()
                                )
                                .stream()
                                .map(viewFactory::lessonView)
                                .sorted(Comparator.comparing(LessonView::getStartsAtRaw).thenComparingLong(LessonView::getId))
                                .toList()
                );
            } catch (RuntimeException exception) {
                result.put(classGroup.id(), List.of());
            }
        }
        return result;
    }

    private Map<Long, List<PhysicalRoomView>> classGroupRoomsByClassGroup(
            Map<Long, List<LessonView>> lessonsByClassGroup,
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        Map<Long, List<PhysicalRoomView>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<LessonView>> entry : lessonsByClassGroup.entrySet()) {
            Map<String, PhysicalRoomView> roomsByCode = new LinkedHashMap<>();
            for (LessonView lesson : entry.getValue()) {
                if (!lesson.isHasRoom()) {
                    continue;
                }
                try {
                    PhysicalRoom room = physicalRoomDAO.findByCode(lesson.getPhysicalRoomCode()).orElse(null);
                    if (room != null) {
                        roomsByCode.putIfAbsent(room.code(), viewFactory.physicalRoomView(room));
                    }
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to load class group physical rooms", exception);
                }
            }
            for (AssessmentView assessment : assessmentsByClassGroup.getOrDefault(entry.getKey(), List.of())) {
                if (!assessment.isHasRoom()) {
                    continue;
                }
                try {
                    PhysicalRoom room = physicalRoomDAO.findByCode(assessment.getPhysicalRoomCode()).orElse(null);
                    if (room != null) {
                        roomsByCode.putIfAbsent(room.code(), viewFactory.physicalRoomView(room));
                    }
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to load class group physical rooms", exception);
                }
            }
            result.put(entry.getKey(), List.copyOf(roomsByCode.values()));
        }
        return result;
    }

    private Map<Long, List<AssessmentView>> classGroupAssessmentsByClassGroup(List<ClassGroup> classGroups) {
        Map<Long, List<AssessmentView>> result = new LinkedHashMap<>();
        for (ClassGroup classGroup : classGroups) {
            try {
                result.put(
                        classGroup.id(),
                        assessmentDAO.findByClassGroup(classGroup.id()).stream()
                                .map(assessmentViewFactory::assessmentView)
                                .sorted(Comparator.comparing(AssessmentView::getAvailableFromRaw,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(AssessmentView::getTitle, String.CASE_INSENSITIVE_ORDER)
                                        .thenComparingLong(AssessmentView::getId))
                                .toList()
                );
            } catch (SQLException exception) {
                result.put(classGroup.id(), List.of());
            }
        }
        return result;
    }

    private Map<String, Boolean> canManagePhysicalRoomByCode(
            HttpServletRequest request,
            Map<Long, List<PhysicalRoomView>> roomsByClassGroup
    ) {
        SessionUser actor = requireCurrentUser(request);
        Map<String, Boolean> permissions = new HashMap<>();
        for (List<PhysicalRoomView> rooms : roomsByClassGroup.values()) {
            for (PhysicalRoomView room : rooms) {
                permissions.computeIfAbsent(room.getCode(), code -> roomService.canManagePhysicalRoom(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        code,
                        request.getRemoteAddr()
                ));
            }
        }
        return permissions;
    }

    private Map<Long, List<LessonView>> blockLessonsByBlock(
            SessionUser actor,
            List<ContentBlockView> blocks,
            HttpServletRequest request
    ) {
        Map<Long, List<LessonView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : blocks) {
            result.put(
                    block.getId(),
                    lessonService.listLessonsByContentBlock(
                                    actor.userId(),
                                    currentSessionId(request),
                                    primaryProfile(actor),
                                    block.getId(),
                                    request.getRemoteAddr()
                            )
                            .stream()
                            .map(viewFactory::lessonView)
                            .toList()
            );
        }
        return result;
    }

    private Map<Long, List<AssessmentView>> blockAssessmentsByBlock(List<ContentBlockView> blocks) {
        Map<Long, List<AssessmentView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : blocks) {
            try {
                result.put(
                        block.getId(),
                        assessmentDAO.findByContentBlock(block.getId()).stream()
                                .map(assessmentViewFactory::assessmentView)
                                .toList()
                );
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load block assessments", exception);
            }
        }
        return result;
    }

    /**
     * Counts are loaded with the learning-plan fragment, so both the aggregate
     * card and the assessment entries expose the same event total without a
     * per-row query or a stale, independently-calculated badge.
     */
    private Map<Long, Integer> pendingAssessmentEnrollmentCounts(
            Map<Long, List<AssessmentView>> assessmentsByBlock
    ) {
        List<Long> assessmentIds = assessmentIds(assessmentsByBlock);
        if (assessmentIds.isEmpty()) {
            return Map.of();
        }
        try {
            return assessmentEnrollmentDAO.countByAssessmentIdsAndState(assessmentIds, EnrollmentState.PENDING);
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private Map<Long, Integer> pendingAssessmentCorrectionCounts(
            Map<Long, List<AssessmentView>> assessmentsByBlock
    ) {
        List<Long> assessmentIds = assessmentIds(assessmentsByBlock);
        if (assessmentIds.isEmpty()) {
            return Map.of();
        }
        try {
            return attemptDAO.countByAssessmentIdsAndState(assessmentIds, AttemptState.SUBMITTED);
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private static List<Long> assessmentIds(Map<Long, List<AssessmentView>> assessmentsByBlock) {
        return assessmentsByBlock.values().stream()
                .flatMap(List::stream)
                .map(AssessmentView::getId)
                .distinct()
                .toList();
    }

    private static Map<Long, List<BlockActivityView>> blockActivitiesByBlock(
            List<ContentBlockView> blocks,
            Map<Long, List<BlockContentItemView>> contentsByBlock,
            Map<Long, List<LessonView>> lessonsByBlock,
            Map<Long, List<AssessmentView>> assessmentsByBlock
    ) {
        Map<Long, List<BlockActivityView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : blocks) {
            List<BlockActivityView> activities = new java.util.ArrayList<>();
            contentsByBlock.getOrDefault(block.getId(), List.of()).stream()
                    .map(BlockActivityView::fromContent)
                    .forEach(activities::add);
            lessonsByBlock.getOrDefault(block.getId(), List.of()).stream()
                    .map(BlockActivityView::fromLesson)
                    .forEach(activities::add);
            assessmentsByBlock.getOrDefault(block.getId(), List.of()).stream()
                    .map(BlockActivityView::fromAssessment)
                    .forEach(activities::add);
            result.put(
                    block.getId(),
                    activities.stream()
                            .sorted(BlockActivityView.pedagogicalOrder())
                            .toList()
            );
        }
        return result;
    }

    private void showClassGroupForm(
            HttpServletRequest request,
            HttpServletResponse response,
            ClassGroupFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareClassGroupForm(request, form, creating, error, null);
        forward(request, response, classGroupFormJsp(request));
    }

    private void showEditForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            String error
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = classGroupService.getClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        );
        ClassGroupFormData form = error == null
                ? ClassGroupFormData.from(classGroup)
                : ClassGroupFormData.from(request, classGroupId);
        prepareClassGroupForm(request, form, false, error, classGroup);
        forward(request, response, classGroupFormJsp(request));
    }

    private void showContentBlockForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            Long contentBlockId,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = classGroupService.getClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        );
        if (classGroup.state() == ClassGroupState.COMPLETED) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        ContentBlockFormData form;
        if (error != null) {
            form = ContentBlockFormData.from(request, contentBlockId);
        } else if (creating) {
            form = ContentBlockFormData.blank(classGroupId, nextBlockOrder(classGroupId));
        } else {
            ContentBlock block = contentBlockService.getContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    request.getRemoteAddr()
            );
            if (block.classGroupId() != classGroupId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            form = ContentBlockFormData.from(block);
        }
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("contentBlockFormAction", creating
                ? request.getContextPath() + "/learning/class-groups/" + classGroupId + "/blocks"
                : request.getContextPath() + "/learning/class-groups/" + classGroupId + "/blocks/" + contentBlockId);
        request.setAttribute("canManageClassGroup", Boolean.TRUE);
        request.setAttribute("canManageClassGroupStructure", canManageClassGroupStructure(request, classGroupView));
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareClassGroupContext(request, classGroupView, creating ? "block-new" : "blocks");
        prepareDashboard(request, "class-groups", creating ? "Create Content Block" : "Edit Content Block");
        forward(request, response, contentBlockFormJsp(request));
    }

    private void createClassGroup(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroupFormData form = ClassGroupFormData.from(request, null);
        try {
            requireSubjectCreationContext(request, form);
            ClassGroup created = classGroupService.createClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupCreateCommand(request),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Class group created successfully.");
            redirect(request, response, "/learning/class-groups/" + created.id());
        } catch (RuntimeException exception) {
            showClassGroupForm(request, response, form, true, messageFor(exception));
        }
    }

    private void updateClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupService.updateClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    classGroupUpdateCommand(request),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Class group updated successfully.");
            redirect(request, response, "/learning/class-groups/" + classGroupId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, classGroupId, messageFor(exception));
        }
    }

    private void deleteClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupService.deleteClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Class group deleted.");
            redirect(request, response, "/learning/class-groups");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/learning/class-groups/" + classGroupId);
        }
    }

    private void assignTeacher(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupService.assignTeacherToClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    longParameter(request, "teacherUserId"),
                    null,
                    null,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Teacher assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-teachers");
    }

    private void updateTeacherAssignment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long teacherUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            String state = text(request, "state");
            if (state == null) {
                throw new IllegalArgumentException("state is required");
            }
            classGroupService.updateTeacherAssignment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    teacherUserId,
                    RoleAssignmentState.fromDatabaseValue(state),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Teacher assignment updated successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-teachers");
    }

    private void removeTeacher(HttpServletRequest request, HttpServletResponse response, long classGroupId, long teacherUserId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupService.removeTeacherFromClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    teacherUserId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Teacher removed from class group.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-teachers");
    }

    private void reorderContentBlocks(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.reorderContentBlocks(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    orderedLongValues(request, "blockId"),
                    request.getRemoteAddr()
            );
            writeJsonStatus(response, HttpServletResponse.SC_OK, "ok");
        } catch (RuntimeException exception) {
            writeJsonStatus(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
        }
    }

    private void reorderBlockContents(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentAssociationService.reorderBlockContentItems(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    contentPlacements(request),
                    request.getRemoteAddr()
            );
            writeJsonStatus(response, HttpServletResponse.SC_OK, "ok");
        } catch (RuntimeException exception) {
            writeJsonStatus(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
        }
    }

    private void enrollStudent(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new ClassGroupEnrollmentCommand(
                            longParameter(request, "studentUserId"),
                            classGroupId,
                            null,
                            null
                    ),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Student enrolled in class group.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void updateEnrollmentPolicy(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.updateClassGroupEnrollmentPolicy(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    EnrollmentApprovalMode.parse(text(request, "approvalMode")),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Class group enrollment policy updated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void approveStudentEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long studentUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.approveClassGroupEnrollment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    classGroupId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Class group enrollment request approved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void rejectStudentEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long studentUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.rejectClassGroupEnrollment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    classGroupId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Class group enrollment request rejected.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void withdrawStudent(HttpServletRequest request, HttpServletResponse response, long classGroupId, long studentUserId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    classGroupId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Student removed from class group.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void deleteStudentEnrollment(HttpServletRequest request, HttpServletResponse response, long classGroupId, long studentUserId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.deleteClassGroupEnrollment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    classGroupId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Class group enrollment deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#class-group-enrollments");
    }

    private void createContentBlock(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.createContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockCreateCommand(request, classGroupId),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Content block created successfully.");
            redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#pedagogical-blocks");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#pedagogical-blocks");
        }
    }

    private void updateContentBlock(HttpServletRequest request, HttpServletResponse response, long classGroupId, long contentBlockId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.updateContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    contentBlockUpdateCommand(request, classGroupId),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Content block updated successfully.");
            redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#block-" + contentBlockId);
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirectToReturnPath(request, response, "/learning/class-groups/" + classGroupId + "#block-" + contentBlockId);
        }
    }

    private void deleteContentBlock(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long contentBlockId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.deleteContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Content block deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
    }

    private void createBlockLesson(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long contentBlockId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Lesson lesson = lessonService.createLesson(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    lessonCreateCommand(request, classGroupId, contentBlockId),
                    request.getRemoteAddr()
            );
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Lesson created: " + lesson.id());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(messageFor(exception));
        }
    }

    private void deleteBlockContent(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long contentBlockId,
            long contentItemId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            ContentRemovalResult removalResult = contentItemService.deleteContentItemFromBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    contentItemId,
                    request.getRemoteAddr()
            );
            deleteOrphanedContentFiles(removalResult);
            if (removalResult.deletionResult() == ContentDeletionResult.INACTIVATED) {
                flashSuccess(request, "Pedagogical content deactivated because it is protected by active learning history.");
            } else if (removalResult.orphanedRelativePaths().isEmpty()) {
                flashSuccess(request, "Pedagogical content deleted. Reused file was kept in the repository.");
            } else {
                flashSuccess(request, "Pedagogical content and unused file deleted.");
            }
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
    }

    private void deleteOrphanedContentFiles(ContentRemovalResult removalResult) {
        if (removalResult == null || removalResult.orphanedRelativePaths().isEmpty()) {
            return;
        }
        try {
            pdfUploadService.deleteStoredContentFiles(removalResult.orphanedRelativePaths());
        } catch (IOException exception) {
            throw new IllegalStateException("Pedagogical content was deleted, but the stored file could not be deleted", exception);
        }
    }

    private List<PhysicalRoomView> physicalRoomOptions(ClassGroupView classGroup) {
        try {
            return viewFactory.physicalRoomViews(physicalRoomDAO.findByOrganization(classGroup.getCourse().getOrganizationId()).stream()
                    .filter(room -> room.state() == PhysicalRoomState.ACTIVE)
                    .sorted(Comparator.comparing(PhysicalRoom::code))
                    .toList());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load physical room options", exception);
        }
    }

    private void prepareClassGroupForm(
            HttpServletRequest request,
            ClassGroupFormData form,
            boolean creating,
            String error,
            ClassGroup classGroup
    ) {
        ClassGroupView classGroupView = classGroup == null ? null : viewFactory.classGroupView(classGroup);
        List<CourseSubjectView> courseSubjectOptions = courseSubjectOptions(request);
        SubjectCreationContext subjectCreationContext = creating
                ? subjectCreationContext(request, courseSubjectOptions)
                : null;
        List<CourseView> courseOptions = subjectCreationContext == null
                ? courseOptions(courseSubjectOptions)
                : courseOptionsForSubjectCreation(subjectCreationContext);
        List<CourseOccurrenceView> courseOccurrenceOptions = courseOptions.stream()
                .flatMap(course -> viewFactory.courseOccurrenceViews(course.getId()).stream())
                .toList();
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("courseOptions", courseOptions);
        request.setAttribute("courseOccurrenceOptions", courseOccurrenceOptions);
        request.setAttribute("courseSubjectOptions", courseSubjectOptions);
        request.setAttribute("subjectCreationContext", subjectCreationContext == null ? null : subjectCreationContext.subject());
        request.setAttribute(
                "subjectCreationAssociationByCourseId",
                subjectCreationContext == null ? Map.of() : subjectCreationContext.associationsByCourseId()
        );
        request.setAttribute(
                "subjectCreationCourseSelectableByCourseId",
                subjectCreationContext == null ? Map.of() : subjectCreationContext.selectableCourseById()
        );
        request.setAttribute("classGroupSelectedYear", selectedCourseSubjectYear(form));
        request.setAttribute("classGroupSelectedTerm", selectedCourseSubjectTerm(form));
        request.setAttribute("shiftOptions", ClassGroupShift.values());
        request.setAttribute("formAction", creating
                ? request.getContextPath() + "/learning/class-groups"
                : request.getContextPath() + "/learning/class-groups/" + form.getId());
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("canManageClassGroup", classGroupView == null || canModifyClassGroup(request, classGroupView));
        boolean canManageClassGroupStructure = classGroupView != null
                && canManageClassGroupStructure(request, classGroupView);
        request.setAttribute("canManageClassGroupStructure", canManageClassGroupStructure);
        if (creating) {
            request.setAttribute("learningClassGroupActiveChild", "new");
        }
        if (classGroupView != null) {
            List<ClassGroupEnrollmentView> enrollments = viewFactory.classGroupEnrollmentViews(classGroupView.getId());
            prepareClassGroupEnrollmentAttributes(request, classGroupView, enrollments);
            request.setAttribute("classGroupEnrollmentPolicy", classGroupEnrollmentPolicy(classGroupView.getId()));
            request.setAttribute("studentOptions", viewFactory.eligibleStudentOptions(
                    classGroupView.getCourseId(),
                    classGroupView.getSubjectId()
            ));
            request.setAttribute("activeEnrollmentByStudent", activeEnrollmentByStudent(enrollments));
            request.setAttribute("canManageClassGroupEnrollments", canManageClassGroupEnrollments(request, classGroupView));
            request.setAttribute("classGroupTeachers", viewFactory.classGroupTeacherViews(classGroupView.getId()));
            request.setAttribute("teacherOptions", viewFactory.activeTeacherOptions(null));
            request.setAttribute("canManageTeacherAssignments",
                    canManageClassGroupStructure && primaryProfile(requireCurrentUser(request)) != AccessProfileType.TEACHER);
            prepareClassGroupContext(request, classGroupView, "edit");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        String pageTitle = creating && subjectCreationContext != null
                ? "Create Class Group on " + subjectCreationContext.subject().getName()
                : creating ? "Create Class Group" : "Edit Class Group";
        request.setAttribute("classGroupPageTitle", pageTitle);
        prepareDashboard(request, "class-groups", pageTitle);
    }

    private List<ClassGroup> visibleClassGroups(HttpServletRequest request) {
        try {
            return classGroupDAO.findAll().stream()
                    .filter(classGroup -> canReadClassGroup(request, classGroup.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
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

    private boolean canCreateClassGroups(HttpServletRequest request) {
        return !courseSubjectOptions(request).isEmpty();
    }

    private Map<Long, Boolean> canModifyClassGroupById(HttpServletRequest request, List<ClassGroupView> classGroups) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            permissions.put(classGroup.getId(), canModifyClassGroup(request, classGroup));
        }
        return permissions;
    }

    private static List<ClassGroupOccurrenceGroupView> classGroupOccurrenceGroups(List<ClassGroupView> classGroups) {
        Map<Long, List<ClassGroupView>> byOccurrence = new LinkedHashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            byOccurrence.computeIfAbsent(classGroup.getCourseOccurrenceId(), ignored -> new ArrayList<>())
                    .add(classGroup);
        }
        List<ClassGroupOccurrenceGroupView> groups = new ArrayList<>();
        for (List<ClassGroupView> occurrenceClassGroups : byOccurrence.values()) {
            if (occurrenceClassGroups.isEmpty()) {
                continue;
            }
            ClassGroupView first = occurrenceClassGroups.get(0);
            groups.add(new ClassGroupOccurrenceGroupView(
                    first.getCourseOccurrenceId(),
                    first.getOccurrenceLabel(),
                    first.getOccurrenceDateRangeLabel(),
                    first.getOccurrenceStateLabel(),
                    first.getOccurrenceStateBadgeClass(),
                    occurrenceClassGroups
            ));
        }
        return List.copyOf(groups);
    }

    private Map<Long, Boolean> canManageClassGroupStructureById(
            HttpServletRequest request,
            List<ClassGroupView> classGroups
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            permissions.put(classGroup.getId(), canManageClassGroupStructure(request, classGroup));
        }
        return permissions;
    }

    private boolean canManageClassGroupStructure(HttpServletRequest request, ClassGroupView classGroup) {
        SessionUser actor = requireCurrentUser(request);
        return classGroupService.canManageClassGroupStructure(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroup.getId(),
                request.getRemoteAddr()
        );
    }

    private boolean canModifyClassGroup(HttpServletRequest request, ClassGroupView classGroup) {
        SessionUser actor = requireCurrentUser(request);
        return classGroupService.canModifyClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroup.getId(),
                request.getRemoteAddr()
        );
    }

    private boolean canManageClassGroupEnrollments(HttpServletRequest request, ClassGroupView classGroup) {
        if (classGroup.isCompleted()) {
            return false;
        }
        SessionUser actor = requireCurrentUser(request);
        return classGroupService.canManageClassGroupEnrollments(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroup.getId(),
                request.getRemoteAddr()
        );
    }

    private List<CourseView> courseOptions(List<CourseSubjectView> courseSubjectOptions) {
        try {
            List<Long> courseIds = courseSubjectOptions.stream()
                    .map(CourseSubjectView::getCourseId)
                    .distinct()
                    .toList();
            return courseDAO.findCatalogCourses(null, null, null).stream()
                    .filter(course -> course.state() == CourseState.ACTIVE)
                    .filter(course -> courseIds.contains(course.id()))
                    .map(viewFactory::courseView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course options", exception);
        }
    }

    private List<CourseView> courseOptionsForSubjectCreation(SubjectCreationContext context) {
        try {
            return courseDAO.findCatalogCourses(context.subject().getOrganizationId(), null, null).stream()
                    .map(viewFactory::courseView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject-context course options", exception);
        }
    }

    private SubjectCreationContext subjectCreationContext(
            HttpServletRequest request,
            List<CourseSubjectView> creatableAssociations
    ) {
        Long subjectContextId = optionalLong(request, "subjectContextId");
        if (subjectContextId == null || subjectContextId <= 0) {
            return null;
        }
        try {
            List<CourseSubjectAssociation> associations = courseSubjectDAO.findBySubject(subjectContextId);
            if (associations.isEmpty()) {
                return null;
            }
            Map<Long, CourseSubjectView> associationsByCourseId = new LinkedHashMap<>();
            SubjectView subject = null;
            for (CourseSubjectAssociation association : associations) {
                CourseSubjectView associationView = viewFactory.courseSubjectView(association, null);
                associationsByCourseId.put(association.courseId(), associationView);
                if (subject == null) {
                    subject = associationView.getSubject();
                }
            }
            if (subject == null) {
                return null;
            }
            Map<Long, Boolean> selectableCourseById = new HashMap<>();
            for (CourseSubjectView association : creatableAssociations) {
                if (association.getSubjectId() == subjectContextId) {
                    selectableCourseById.put(association.getCourseId(), true);
                }
            }
            return new SubjectCreationContext(subject, Map.copyOf(associationsByCourseId), Map.copyOf(selectableCourseById));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject creation context", exception);
        }
    }

    private static void requireSubjectCreationContext(HttpServletRequest request, ClassGroupFormData form) {
        Long subjectContextId = optionalLong(request, "subjectContextId");
        if (subjectContextId == null) {
            return;
        }
        if (!Long.toString(subjectContextId).equals(form.getSubjectId())) {
            throw new IllegalArgumentException("A class group opened from Subject Details must keep its selected subject");
        }
    }

    private List<CourseSubjectView> courseSubjectOptions(HttpServletRequest request) {
        try {
            return courseDAO.findCatalogCourses(null, null, null).stream()
                    .filter(course -> course.state() == CourseState.ACTIVE)
                    .flatMap(course -> courseSubjects(course).stream())
                    .filter(association -> canCreateClassGroup(request, association.courseId(), association.subjectId()))
                    .map(association -> viewFactory.courseSubjectView(association, null))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course-subject options", exception);
        }
    }

    private boolean canCreateClassGroup(HttpServletRequest request, long courseId, long subjectId) {
        SessionUser actor = requireCurrentUser(request);
        return classGroupService.canCreateClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                subjectId,
                request.getRemoteAddr()
        );
    }

    private List<CourseSubjectAssociation> courseSubjects(Course course) {
        try {
            return courseSubjectDAO.findByCourse(course.id());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course subjects", exception);
        }
    }

    private String selectedCourseSubjectTerm(ClassGroupFormData form) {
        if (form == null || form.getCourseId() == null || form.getCourseId().isBlank()
                || form.getSubjectId() == null || form.getSubjectId().isBlank()) {
            return "";
        }
        try {
            long courseId = Long.parseLong(form.getCourseId());
            long subjectId = Long.parseLong(form.getSubjectId());
            return courseSubjectDAO.findByCourse(courseId).stream()
                    .filter(association -> association.subjectId() == subjectId)
                    .map(CourseSubjectAssociation::term)
                    .filter(term -> term != null)
                    .map(Enum::name)
                    .findFirst()
                    .orElse("");
        } catch (NumberFormatException exception) {
            return "";
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group subject period", exception);
        }
    }

    private String selectedCourseSubjectYear(ClassGroupFormData form) {
        if (form == null || form.getCourseId() == null || form.getCourseId().isBlank()
                || form.getSubjectId() == null || form.getSubjectId().isBlank()) {
            return "";
        }
        try {
            long courseId = Long.parseLong(form.getCourseId());
            long subjectId = Long.parseLong(form.getSubjectId());
            return courseSubjectDAO.findByCourse(courseId).stream()
                    .filter(association -> association.subjectId() == subjectId)
                    .map(CourseSubjectAssociation::curricularYear)
                    .filter(year -> year != null)
                    .map(String::valueOf)
                    .findFirst()
                    .orElse("");
        } catch (NumberFormatException exception) {
            return "";
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group subject curricular year", exception);
        }
    }

    private int nextBlockOrder(long classGroupId) {
        return viewFactory.contentBlockViews(classGroupId).stream()
                .mapToInt(ContentBlockView::getOrderNo)
                .max()
                .orElse(0) + 1;
    }

    private String nextBlockCode(long classGroupId, int orderNo) {
        String baseCode = "BLK-" + (orderNo < 10 ? "0" : "") + orderNo;
        boolean exists = viewFactory.contentBlockViews(classGroupId).stream()
                .anyMatch(block -> baseCode.equalsIgnoreCase(block.getCode()));
        if (!exists) {
            return baseCode;
        }
        return baseCode + "-" + System.currentTimeMillis();
    }

    private Map<Long, Boolean> activeEnrollmentByStudent(List<ClassGroupEnrollmentView> enrollments) {
        Map<Long, Boolean> result = new HashMap<>();
        for (ClassGroupEnrollmentView enrollment : enrollments) {
            result.put(enrollment.getStudentUserId(), enrollment.isActive() || enrollment.isPending());
        }
        return result;
    }

    private void prepareClassGroupEnrollmentAttributes(
            HttpServletRequest request,
            ClassGroupView classGroup,
            List<ClassGroupEnrollmentView> enrollments
    ) {
        request.setAttribute("classGroupEnrollments", enrollments);
        List<ClassGroupEnrollmentView> pendingEnrollments = enrollments.stream()
                .filter(ClassGroupEnrollmentView::isPending)
                .toList();
        boolean canManageEnrollments = classGroup != null && canManageClassGroupEnrollments(request, classGroup);
        request.setAttribute("pendingClassGroupEnrollments", pendingEnrollments);
        request.setAttribute(
                "classGroupDetailPendingEnrollmentCount",
                canManageEnrollments ? pendingEnrollments.size() : 0
        );
        request.setAttribute("activeClassGroupEnrollments",
                enrollments.stream().filter(ClassGroupEnrollmentView::isActive).toList());
        request.setAttribute("auditClassGroupEnrollments",
                enrollments.stream()
                        .filter(enrollment -> !enrollment.isPending() && !enrollment.isActive())
                        .toList());
        request.setAttribute("activeClassGroupEnrollmentCount",
                enrollments.stream().filter(ClassGroupEnrollmentView::isActive).count());
        request.setAttribute("canManageClassGroupEnrollments", canManageEnrollments);
    }

    private String classGroupEnrollmentPolicy(long classGroupId) {
        try {
            return enrollmentApprovalPolicyDAO.classGroupMode(classGroupId).toDatabaseValue();
        } catch (SQLException exception) {
            return EnrollmentApprovalMode.MANUAL.toDatabaseValue();
        }
    }

    private int pendingEnrollmentCount(long classGroupId) {
        try {
            return classGroupEnrollmentDAO.countPendingByClassGroupIds(List.of(classGroupId))
                    .getOrDefault(classGroupId, 0);
        } catch (SQLException exception) {
            return 0;
        }
    }

    private Map<Long, Integer> manageableEventCountsByClassGroupId(
            HttpServletRequest request,
            List<ClassGroupView> classGroups
    ) {
        if (classGroups.isEmpty()) {
            return Map.of();
        }
        try {
            List<ClassGroupView> manageableClassGroups = classGroups.stream()
                    .filter(classGroup -> canManageClassGroupEnrollments(request, classGroup))
                    .toList();
            if (manageableClassGroups.isEmpty()) {
                return Map.of();
            }
            List<Long> manageableIds = manageableClassGroups.stream().map(ClassGroupView::getId).toList();
            Map<Long, Integer> pendingCounts = classGroupEnrollmentDAO.countPendingByClassGroupIds(manageableIds);
            Map<Long, Integer> submittedAttemptCounts = assessmentDAO
                    .countSubmittedAttemptsByClassGroupIds(manageableIds);
            Map<Long, Integer> manageableCounts = new HashMap<>();
            for (ClassGroupView classGroup : manageableClassGroups) {
                int eventCount = pendingCounts.getOrDefault(classGroup.getId(), 0)
                        + submittedAttemptCounts.getOrDefault(classGroup.getId(), 0);
                if (eventCount > 0) {
                    manageableCounts.put(classGroup.getId(), eventCount);
                }
            }
            return manageableCounts;
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private String classGroupListJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_CLASS_GROUP_LIST_JSP;
            case TEACHER -> INSTRUCTOR_CLASS_GROUP_LIST_JSP;
            default -> ADMIN_CLASS_GROUP_LIST_JSP;
        };
    }

    private String classGroupDetailJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_CLASS_GROUP_DETAIL_JSP;
            case TEACHER -> INSTRUCTOR_CLASS_GROUP_DETAIL_JSP;
            default -> ADMIN_CLASS_GROUP_DETAIL_JSP;
        };
    }

    private String classGroupFormJsp(HttpServletRequest request) {
        return ADMIN_CLASS_GROUP_FORM_JSP;
    }

    private String contentBlockFormJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_CONTENT_BLOCK_FORM_JSP;
            case TEACHER -> INSTRUCTOR_CONTENT_BLOCK_FORM_JSP;
            default -> ADMIN_CONTENT_BLOCK_FORM_JSP;
        };
    }

    private ClassGroupCreateCommand classGroupCreateCommand(HttpServletRequest request) {
        return new ClassGroupCreateCommand(
                longParameter(request, "subjectId"),
                longParameter(request, "courseId"),
                optionalLong(request, "courseOccurrenceId"),
                optionalLong(request, "courseOccurrencePeriodId"),
                text(request, "code"),
                classGroupModality(text(request, "modality")),
                classGroupState(text(request, "state")),
                optionalInteger(request, "minStudents"),
                optionalInteger(request, "maxStudents"),
                null,
                null,
                classGroupShift(text(request, "shift")),
                "true".equalsIgnoreCase(request.getParameter("showContentThumbnails"))
        );
    }

    private ClassGroupUpdateCommand classGroupUpdateCommand(HttpServletRequest request) {
        return new ClassGroupUpdateCommand(
                longParameter(request, "subjectId"),
                longParameter(request, "courseId"),
                optionalLong(request, "courseOccurrenceId"),
                optionalLong(request, "courseOccurrencePeriodId"),
                text(request, "code"),
                classGroupModality(text(request, "modality")),
                classGroupState(text(request, "state")),
                optionalInteger(request, "minStudents"),
                optionalInteger(request, "maxStudents"),
                null,
                null,
                classGroupShift(text(request, "shift")),
                "true".equalsIgnoreCase(request.getParameter("showContentThumbnails"))
        );
    }

    private ContentBlockCreateCommand contentBlockCreateCommand(HttpServletRequest request, long classGroupId) {
        int orderNo = nextBlockOrder(classGroupId);
        return new ContentBlockCreateCommand(
                classGroupId,
                nextBlockCode(classGroupId, orderNo),
                text(request, "name"),
                text(request, "description"),
                orderNo,
                contentBlockState(text(request, "state"))
        );
    }

    private ContentBlockUpdateCommand contentBlockUpdateCommand(HttpServletRequest request, long classGroupId) {
        return new ContentBlockUpdateCommand(
                classGroupId,
                text(request, "code"),
                text(request, "name"),
                text(request, "description"),
                integerParameter(request, "orderNo"),
                contentBlockState(text(request, "state"))
        );
    }

    private LessonCreateCommand lessonCreateCommand(HttpServletRequest request, long classGroupId, long contentBlockId) {
        return new LessonCreateCommand(
                classGroupId,
                contentBlockId,
                text(request, "physicalRoomCode"),
                text(request, "title"),
                text(request, "description"),
                lessonType(request),
                text(request, "provider"),
                text(request, "accessUrl"),
                "true".equalsIgnoreCase(text(request, "attendanceRequired")),
                lessonState(request),
                optionalDateTime(request, "startsAt"),
                optionalDateTime(request, "endsAt")
        );
    }

    private static LessonState lessonState(HttpServletRequest request) {
        String value = text(request, "state");
        return value == null ? LessonState.DRAFT : LessonState.parse(value);
    }

    private static LessonType lessonType(HttpServletRequest request) {
        String value = text(request, "type");
        if (value == null) {
            throw new IllegalArgumentException("Lesson type is required");
        }
        for (LessonType type : LessonType.values()) {
            if (type.name().equalsIgnoreCase(value) || type.toDatabaseValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported lesson type: " + value);
    }

    private static void prepareClassGroupContext(HttpServletRequest request, ClassGroupView classGroup, String activeChild) {
        request.setAttribute("learningClassGroupContextId", classGroup.getId());
        request.setAttribute("learningClassGroupContextName", classGroup.getCode());
        request.setAttribute("learningClassGroupActiveChild", activeChild);
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static int integerParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Integer.parseInt(value);
    }

    private static boolean isListRowsFragmentRequest(HttpServletRequest request) {
        return "rows".equals(request.getParameter("fragment"));
    }

    private static int requestedListOffset(HttpServletRequest request) {
        String value = request.getParameter("offset");
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean requestedListLoadAll(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter("loadAll"));
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDate(value);
    }

    private static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static LocalDateTime optionalDateTime(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDateTime(value);
    }

    private static List<Long> orderedLongValues(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Order payload is required");
        }
        List<Long> ordered = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                ordered.add(Long.parseLong(value.trim()));
            }
        }
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("Order payload is required");
        }
        return ordered;
    }

    private static List<BlockContentPlacement> contentPlacements(HttpServletRequest request) {
        String[] values = request.getParameterValues("itemPlacement");
        if (values == null || values.length == 0) {
            values = request.getParameterValues("contentPlacement");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Pedagogical item order payload is required");
        }
        List<BlockContentPlacement> placements = new ArrayList<>();
        int orderNo = 1;
        long currentTargetBlockId = -1L;
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String[] parts = value.trim().split(":");
            if (parts.length != 3 && parts.length != 4) {
                throw new IllegalArgumentException("Invalid pedagogical item order payload");
            }
            String itemType = parts.length == 4 ? parts[0].trim() : "content";
            long sourceBlockId = Long.parseLong(parts[parts.length == 4 ? 1 : 0]);
            long targetBlockId = Long.parseLong(parts[parts.length == 4 ? 2 : 1]);
            long itemId = Long.parseLong(parts[parts.length == 4 ? 3 : 2]);
            if (targetBlockId != currentTargetBlockId) {
                currentTargetBlockId = targetBlockId;
                orderNo = 1;
            }
            placements.add(new BlockContentPlacement(itemType, sourceBlockId, targetBlockId, itemId, orderNo++));
        }
        if (placements.isEmpty()) {
            throw new IllegalArgumentException("Pedagogical item order payload is required");
        }
        return placements;
    }

    private static void writeJsonStatus(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"" + (status < 400 ? "ok" : "error")
                + "\",\"message\":\"" + json(message) + "\"}");
    }

    private static String json(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static ClassGroupModality classGroupModality(String value) {
        return enumValue(ClassGroupModality.class, value, ClassGroupModality.ONSITE, "class group modality");
    }

    private static ClassGroupState classGroupState(String value) {
        return enumValue(ClassGroupState.class, value, ClassGroupState.DRAFT, "class group state");
    }

    private static ClassGroupShift classGroupShift(String value) {
        if (value == null || value.isBlank()) {
            return ClassGroupShift.MORNING;
        }
        try {
            return ClassGroupShift.fromDatabaseValue(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid class group shift");
        }
    }

    private static ContentBlockState contentBlockState(String value) {
        return enumValue(ContentBlockState.class, value, ContentBlockState.ACTIVE, "content block state");
    }

    private static <T extends Enum<T>> T enumValue(Class<T> enumType, String value, T defaultValue, String label) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + label);
        }
    }
}
