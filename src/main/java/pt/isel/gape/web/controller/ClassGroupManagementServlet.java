package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupCreateCommand;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ClassGroupUpdateCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockAccessMode;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.ContentDeletionResult;
import pt.isel.gape.learning.model.ContentRemovalResult;
import pt.isel.gape.learning.service.ClassGroupEnrollmentService;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.ContentBlockService;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.BlockContentItemView;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupFormData;
import pt.isel.gape.web.view.ClassGroupTeacherView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.ContentBlockFormData;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.ContentRepositoryItemView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;

@WebServlet(name = "classGroupManagementServlet", urlPatterns = {"/learning/class-groups", "/learning/class-groups/*"})
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

    private final ClassGroupService classGroupService;
    private final ClassGroupEnrollmentService enrollmentService;
    private final ContentBlockService contentBlockService;
    private final ContentAssociationService contentAssociationService;
    private final ContentItemService contentItemService;
    private final PdfUploadService pdfUploadService;
    private final ClassGroupDAO classGroupDAO;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final LearningViewFactory viewFactory;

    public ClassGroupManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private ClassGroupManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ClassGroupService(connectionProvider, clock),
                new ClassGroupEnrollmentService(connectionProvider, clock),
                new ContentBlockService(connectionProvider, clock),
                new ContentAssociationService(connectionProvider, clock),
                new ContentItemService(connectionProvider, clock),
                new PdfUploadService(),
                new ClassGroupDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
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
        );
    }

    ClassGroupManagementServlet(
            ClassGroupService classGroupService,
            ClassGroupEnrollmentService enrollmentService,
            ContentBlockService contentBlockService,
            ContentAssociationService contentAssociationService,
            ContentItemService contentItemService,
            PdfUploadService pdfUploadService,
            ClassGroupDAO classGroupDAO,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            LearningViewFactory viewFactory
    ) {
        this.classGroupService = classGroupService;
        this.enrollmentService = enrollmentService;
        this.contentBlockService = contentBlockService;
        this.contentAssociationService = contentAssociationService;
        this.contentItemService = contentItemService;
        this.pdfUploadService = pdfUploadService;
        this.classGroupDAO = classGroupDAO;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.viewFactory = viewFactory;
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
            if (segments.length == 2 && "edit".equals(segments[1])) {
                showEditForm(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            if (segments.length == 3 && "blocks".equals(segments[1]) && "new".equals(segments[2])) {
                showContentBlockForm(request, response, Long.parseLong(segments[0]), null, true, null);
                return;
            }
            if (segments.length == 4 && "blocks".equals(segments[1]) && "edit".equals(segments[3])) {
                showContentBlockForm(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2]),
                        false,
                        null
                );
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
            if (segments.length == 2) {
                long classGroupId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "archive" -> archiveClassGroup(request, response, classGroupId);
                    case "delete" -> deleteClassGroup(request, response, classGroupId);
                    case "teachers" -> assignTeacher(request, response, classGroupId);
                    case "enrollments" -> enrollStudent(request, response, classGroupId);
                    case "blocks" -> createContentBlock(request, response, classGroupId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1]) && "withdraw".equals(segments[3])) {
                withdrawStudent(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "teachers".equals(segments[1]) && "remove".equals(segments[3])) {
                removeTeacher(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
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
                long classGroupId = Long.parseLong(segments[0]);
                long contentBlockId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "archive" -> archiveContentBlock(request, response, classGroupId, contentBlockId);
                    case "unarchive" -> unarchiveContentBlock(request, response, classGroupId, contentBlockId);
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
        Long courseFilter = optionalLong(request, "courseId");
        Long subjectFilter = optionalLong(request, "subjectId");
        List<ClassGroup> visible = visibleClassGroups(request).stream()
                .filter(classGroup -> courseFilter == null || classGroup.courseId() == courseFilter)
                .filter(classGroup -> subjectFilter == null || classGroup.subjectId() == subjectFilter)
                .sorted(Comparator.comparingLong(ClassGroup::id))
                .toList();
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(visible);
        request.setAttribute("classGroups", classGroups);
        request.setAttribute("classGroupCount", classGroups.size());
        request.setAttribute("activeClassGroups", classGroups.stream().filter(ClassGroupView::isActive).count());
        request.setAttribute("archivedClassGroups", classGroups.stream().filter(ClassGroupView::isArchived).count());
        request.setAttribute("canCreateClassGroups", canCreateClassGroups(request));
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(request, classGroups));
        request.setAttribute("canManageClassGroupStructureById", canManageClassGroupStructureById(request, classGroups));
        prepareDashboard(
                request,
                "class-groups",
                "Class Groups",
                canCreateClassGroups(request) ? "/learning/class-groups/new" : null,
                canCreateClassGroups(request) ? "New Class Group" : null
        );
        forward(request, response, classGroupListJsp(request));
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        ClassGroup classGroup = classGroupService.getClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        );
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        boolean canManageClassGroup = canModifyClassGroup(request, classGroupView);
        boolean canManageClassGroupStructure = canManageClassGroupStructure(request, classGroupView);
        boolean canManageClassGroupEnrollments = canManageClassGroupEnrollments(request, classGroupView);
        boolean canManageTeacherAssignments = canManageClassGroupStructure
                && primaryProfile(actor) != AccessProfileType.TEACHER;
        List<ContentBlockView> blocks = List.of();
        String blockContentLoadError = null;
        if (canManageClassGroup || canManageClassGroupStructure) {
            try {
                blocks = contentBlockService.listContentBlocksByClassGroup(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                classGroupId,
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(viewFactory::contentBlockView)
                        .toList();
            } catch (RuntimeException exception) {
                blockContentLoadError = "Could not load pedagogical blocks for this class group.";
            }
        }
        List<ClassGroupEnrollmentView> enrollments = viewFactory.classGroupEnrollmentViews(classGroupId);
        List<ClassGroupTeacherView> teacherAssignments = viewFactory.classGroupTeacherViews(classGroupId);
        Map<Long, Boolean> activeEnrollmentByStudent = activeEnrollmentByStudent(enrollments);
        Map<Long, List<BlockContentItemView>> blockContentsByBlock = new LinkedHashMap<>();
        if (blockContentLoadError == null && !blocks.isEmpty()) {
            try {
                blockContentsByBlock = blockContentsByBlock(actor, blocks, request);
            } catch (RuntimeException exception) {
                blockContentLoadError = "Could not load pedagogical contents for this class group.";
            }
        }
        int blockContentCount = blockContentsByBlock.values().stream().mapToInt(List::size).sum();
        long pdfContentCount = blockContentsByBlock.values().stream()
                .flatMap(List::stream)
                .filter(BlockContentItemView::isPdf)
                .count();
        List<ContentRepositoryItemView> contentRepository = List.of();
        if (canManageClassGroupStructure) {
            try {
                contentRepository = contentItemService.listReusableFileBackedContent(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                ContentAssociationType.CLASS_GROUP,
                                classGroupId,
                                request.getRemoteAddr()
                        )
                        .stream()
                        .map(ContentRepositoryItemView::from)
                        .toList();
            } catch (RuntimeException ignored) {
                contentRepository = List.of();
            }
        }

        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("contentBlocks", blocks);
        request.setAttribute("blockContentsByBlock", blockContentsByBlock);
        request.setAttribute("blockContentLoadSuccess", blockContentLoadError == null);
        request.setAttribute("blockContentLoadError", blockContentLoadError);
        request.setAttribute("blockContentCount", blockContentCount);
        request.setAttribute("pdfContentCount", pdfContentCount);
        request.setAttribute("contentRepository", contentRepository);
        request.setAttribute("classGroupEnrollments", enrollments);
        request.setAttribute("classGroupTeachers", teacherAssignments);
        request.setAttribute("teacherOptions", viewFactory.activeTeacherOptions(null));
        request.setAttribute("studentOptions", viewFactory.eligibleStudentOptions(classGroup.courseId(), classGroup.subjectId()));
        request.setAttribute("activeEnrollmentByStudent", activeEnrollmentByStudent);
        request.setAttribute("canManageClassGroup", canManageClassGroup);
        request.setAttribute("canManageClassGroupStructure", canManageClassGroupStructure);
        request.setAttribute("canManageClassGroupEnrollments", canManageClassGroupEnrollments);
        request.setAttribute("canManageTeacherAssignments", canManageTeacherAssignments);
        prepareClassGroupContext(request, classGroupView, "detail");
        prepareDashboard(request, "class-groups", "Class Group Detail");
        forward(request, response, classGroupDetailJsp(request));
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
        if (classGroup.state() == ClassGroupState.ARCHIVED) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
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
        if (classGroup.state() == ClassGroupState.ARCHIVED) {
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

    private void archiveClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupService.archiveClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Class group archived.");
            redirect(request, response, "/learning/class-groups");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/learning/class-groups/" + classGroupId);
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
                    optionalDate(request, "startDate"),
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Teacher assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
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
        redirect(request, response, "/learning/class-groups/" + classGroupId);
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
                            optionalDate(request, "startDate"),
                            optionalDate(request, "endDate")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Student enrolled in class group.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
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
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Student withdrawn from class group.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
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
            redirect(request, response, "/learning/class-groups/" + classGroupId);
        } catch (RuntimeException exception) {
            showContentBlockForm(request, response, classGroupId, null, true, messageFor(exception));
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
            redirect(request, response, "/learning/class-groups/" + classGroupId);
        } catch (RuntimeException exception) {
            showContentBlockForm(request, response, classGroupId, contentBlockId, false, messageFor(exception));
        }
    }

    private void archiveContentBlock(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long contentBlockId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.archiveContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Content block archived.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
    }

    private void unarchiveContentBlock(
            HttpServletRequest request,
            HttpServletResponse response,
            long classGroupId,
            long contentBlockId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            contentBlockService.unarchiveContentBlock(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    contentBlockId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Content block restored successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/class-groups/" + classGroupId);
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
            if (removalResult.deletionResult() == ContentDeletionResult.ARCHIVED) {
                flashSuccess(request, "Pedagogical content archived because it is protected by active learning history.");
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

    private void prepareClassGroupForm(
            HttpServletRequest request,
            ClassGroupFormData form,
            boolean creating,
            String error,
            ClassGroup classGroup
    ) {
        ClassGroupView classGroupView = classGroup == null ? null : viewFactory.classGroupView(classGroup);
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("courseOptions", courseOptions(request));
        request.setAttribute("courseSubjectOptions", courseSubjectOptions(request));
        request.setAttribute("shiftOptions", ClassGroupShift.values());
        request.setAttribute("formAction", creating
                ? request.getContextPath() + "/learning/class-groups"
                : request.getContextPath() + "/learning/class-groups/" + form.getId());
        request.setAttribute("classGroup", classGroupView);
        request.setAttribute("canManageClassGroup", classGroupView == null || canModifyClassGroup(request, classGroupView));
        request.setAttribute("canManageClassGroupStructure", classGroupView != null
                && canManageClassGroupStructure(request, classGroupView));
        if (classGroupView != null) {
            prepareClassGroupContext(request, classGroupView, "edit");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "class-groups", creating ? "Create Class Group" : "Edit Class Group");
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
        if (classGroup.isArchived()) {
            return false;
        }
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
        if (classGroup.isArchived()) {
            return false;
        }
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
        if (classGroup.isArchived()) {
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

    private List<CourseView> courseOptions(HttpServletRequest request) {
        try {
            List<Long> courseIds = courseSubjectOptions(request).stream()
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

    private List<CourseSubjectView> courseSubjectOptions(HttpServletRequest request) {
        try {
            return courseDAO.findCatalogCourses(null, null, null).stream()
                    .filter(course -> course.state() == CourseState.ACTIVE)
                    .flatMap(course -> activeCourseSubjects(course).stream())
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

    private List<CourseSubjectAssociation> activeCourseSubjects(Course course) {
        try {
            Map<String, CourseSubjectAssociation> distinctAssociations = new LinkedHashMap<>();
            for (CourseSubjectAssociation association : courseSubjectDAO.findActiveByCourse(course.id())) {
                distinctAssociations.putIfAbsent(
                        association.courseId() + ":" + association.subjectId(),
                        association
                );
            }
            return List.copyOf(distinctAssociations.values());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load active course subjects", exception);
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
            result.put(enrollment.getStudentUserId(), enrollment.isActive());
        }
        return result;
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
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_CLASS_GROUP_FORM_JSP;
            case TEACHER -> INSTRUCTOR_CLASS_GROUP_FORM_JSP;
            default -> ADMIN_CLASS_GROUP_FORM_JSP;
        };
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
                text(request, "code"),
                classGroupModality(text(request, "modality")),
                classGroupState(text(request, "state")),
                optionalInteger(request, "minStudents"),
                optionalInteger(request, "maxStudents"),
                optionalDate(request, "startsAt"),
                optionalDate(request, "endsAt"),
                classGroupShift(text(request, "shift"))
        );
    }

    private ClassGroupUpdateCommand classGroupUpdateCommand(HttpServletRequest request) {
        return new ClassGroupUpdateCommand(
                longParameter(request, "subjectId"),
                longParameter(request, "courseId"),
                text(request, "code"),
                classGroupModality(text(request, "modality")),
                classGroupState(text(request, "state")),
                optionalInteger(request, "minStudents"),
                optionalInteger(request, "maxStudents"),
                optionalDate(request, "startsAt"),
                optionalDate(request, "endsAt"),
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
                contentBlockAccessMode(text(request, "accessMode")),
                contentBlockState(text(request, "state")),
                optionalDateTime(request, "availableFrom"),
                optionalDateTime(request, "availableUntil")
        );
    }

    private ContentBlockUpdateCommand contentBlockUpdateCommand(HttpServletRequest request, long classGroupId) {
        return new ContentBlockUpdateCommand(
                classGroupId,
                text(request, "code"),
                text(request, "name"),
                text(request, "description"),
                integerParameter(request, "orderNo"),
                contentBlockAccessMode(text(request, "accessMode")),
                contentBlockState(text(request, "state")),
                optionalDateTime(request, "availableFrom"),
                optionalDateTime(request, "availableUntil")
        );
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

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }

    private static LocalDateTime optionalDateTime(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDateTime.parse(value);
    }

    private static ClassGroupModality classGroupModality(String value) {
        return enumValue(ClassGroupModality.class, value, ClassGroupModality.ONSITE, "class group modality");
    }

    private static ClassGroupState classGroupState(String value) {
        return enumValue(ClassGroupState.class, value, ClassGroupState.ACTIVE, "class group state");
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

    private static ContentBlockAccessMode contentBlockAccessMode(String value) {
        return enumValue(ContentBlockAccessMode.class, value, ContentBlockAccessMode.OPEN, "content block access mode");
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
