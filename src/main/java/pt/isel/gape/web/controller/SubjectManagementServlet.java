package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectInitialCourseAssignment;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.OrganizationView;
import pt.isel.gape.web.view.SubjectCourseView;
import pt.isel.gape.web.view.SubjectFormData;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

@WebServlet(name = "subjectManagementServlet", urlPatterns = {
        "/admin/subjects",
        "/admin/subjects/*",
        "/coordinator/subjects",
        "/coordinator/subjects/*"
})
@MultipartConfig(maxFileSize = 50L * 1024L * 1024L, maxRequestSize = 52L * 1024L * 1024L)
public final class SubjectManagementServlet extends DashboardServletSupport {

    private static final String ADMIN_SUBJECT_LIST_JSP = "/admin/admin/subject/admin-subjects.jsp";
    private static final String ADMIN_SUBJECT_FORM_JSP = "/admin/admin/subject/admin-subject-form.jsp";
    private static final String ADMIN_SUBJECT_DETAIL_JSP = "/admin/admin/subject/admin-subject-detail.jsp";
    private static final String ADMIN_SUBJECT_COURSE_FORM_JSP = "/admin/admin/subject/admin-subject-course-form.jsp";
    private static final String COORDINATOR_SUBJECT_LIST_JSP =
            "/coordinator/coordinator/subject/coordinator-subjects.jsp";
    private static final String COORDINATOR_SUBJECT_FORM_JSP =
            "/coordinator/coordinator/subject/coordinator-subject-form.jsp";
    private static final String COORDINATOR_SUBJECT_DETAIL_JSP =
            "/coordinator/coordinator/subject/coordinator-subject-detail.jsp";
    private static final String COORDINATOR_SUBJECT_COURSE_FORM_JSP =
            "/coordinator/coordinator/subject/coordinator-subject-course-form.jsp";

    private final SubjectService subjectService;
    private final CourseService courseService;
    private final CourseSubjectService courseSubjectService;
    private final ClassGroupService classGroupService;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final SubjectDAO subjectDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final OrganizationDAO organizationDAO;
    private final LearningViewFactory viewFactory;
    private final ProfilePhotoStorage photoStorage;

    public SubjectManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private SubjectManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new SubjectService(connectionProvider, clock),
                new CourseService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new OrganizationService(connectionProvider, clock),
                new UserService(connectionProvider, clock),
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
                new OrganizationDAO(connectionProvider),
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
                new ProfilePhotoStorage()
        );
    }

    SubjectManagementServlet(
            SubjectService subjectService,
            CourseService courseService,
            CourseSubjectService courseSubjectService,
            ClassGroupService classGroupService,
            OrganizationService organizationService,
            UserService userService,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            ClassGroupDAO classGroupDAO,
            SubjectDAO subjectDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            OrganizationDAO organizationDAO,
            LearningViewFactory viewFactory,
            ProfilePhotoStorage photoStorage
    ) {
        this.subjectService = subjectService;
        this.courseService = courseService;
        this.courseSubjectService = courseSubjectService;
        this.classGroupService = classGroupService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.classGroupDAO = classGroupDAO;
        this.subjectDAO = subjectDAO;
        this.coordinateSubjectDAO = coordinateSubjectDAO;
        this.organizationDAO = organizationDAO;
        this.viewFactory = viewFactory;
        this.photoStorage = photoStorage;
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
                if (isCoordinatorSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showSubjectForm(request, response, SubjectFormData.blank(firstManagedOrganizationId(request)), true, null);
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
            if (segments.length == 2 && "courses".equals(segments[1])) {
                showCourseAssociationForm(request, response, Long.parseLong(segments[0]), null);
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
                if (isCoordinatorSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                createSubject(request, response);
                return;
            }
            if (segments.length == 1) {
                updateSubject(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2) {
                long subjectId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "assign-coordinator" -> assignCoordinator(request, response, subjectId);
                    case "courses" -> addCourseAssociation(request, response, subjectId);
                    case "archive" -> archiveSubject(request, response, subjectId);
                    case "delete" -> deleteSubject(request, response, subjectId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 3 && "courses".equals(segments[1])) {
                updateCourseAssociation(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "courses".equals(segments[1]) && "delete".equals(segments[3])) {
                removeCourseAssociation(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        boolean coordinatorMode = isCoordinatorSubjectRequest(request);
        List<Organization> organizations = coordinatorMode ? coordinatorManagedOrganizations(actor) : managedOrganizations(request);
        List<Subject> rawSubjects = coordinatorMode
                ? coordinatorSubjects(actor, organizations, request)
                : organizations.stream()
                        .flatMap(organization -> subjectService.listSubjects(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                organization.id(),
                                request.getRemoteAddr()
                        ).stream())
                        .sorted(Comparator.comparingLong(Subject::id))
                        .toList();
        List<SubjectView> subjects = rawSubjects.stream()
                .map(viewFactory::subjectView)
                .toList();
        Map<Long, List<SubjectCourseView>> subjectCoursesBySubject = subjectCoursesBySubject(subjects);
        Map<Long, Map<Long, List<ClassGroupView>>> subjectCourseClassGroups =
                subjectCourseClassGroupsBySubjectAndCourse(subjectCoursesBySubject);
        Set<Long> courseIds = courseIdsFrom(subjectCoursesBySubject);
        Set<Long> classGroupIds = classGroupIdsFrom(subjectCourseClassGroups);

        request.setAttribute("subjects", subjects);
        prepareSubjectBasePath(request);
        request.setAttribute("canCreateSubjects", canCreateAnySubject(actor, organizations, request));
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, rawSubjects, request));
        request.setAttribute("canManageSubjectAssociationsById",
                canManageSubjectAssociationsById(actor, rawSubjects, request));
        request.setAttribute("subjectCoursesBySubject", subjectCoursesBySubject);
        request.setAttribute("subjectCourseClassGroupsBySubjectAndCourse", subjectCourseClassGroups);
        request.setAttribute("canModifyCourseById", canModifyCourseById(actor, courseIds, request));
        request.setAttribute("canManageCourseChildrenById", canManageCourseChildrenById(actor, courseIds, request));
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        request.setAttribute("subjectCount", subjects.size());
        request.setAttribute("activeSubjects", subjects.stream().filter(SubjectView::isActive).count());
        request.setAttribute("archivedSubjects", subjects.stream().filter(SubjectView::isArchived).count());
        prepareDashboard(
                request,
                "subjects",
                "Subjects",
                (Boolean) request.getAttribute("canCreateSubjects") ? subjectBasePath(request) + "/new" : null,
                (Boolean) request.getAttribute("canCreateSubjects") ? "New Subject" : null
        );
        forward(request, response, subjectListJsp(request));
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        SubjectView subjectView = viewFactory.subjectView(subject);
        boolean canModifySubject = subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        boolean canManageSubjectAssociations = canManageSubjectAssociations(actor, subjectId, request);
        prepareSubjectBasePath(request);
        request.setAttribute("subject", subjectView);
        request.setAttribute("subjectCourseAssociations", subjectCourseViews(subjectId));
        request.setAttribute("classGroups", viewFactory.classGroupViews(classGroupsBySubject(subjectId)));
        request.setAttribute("coordinatorOptions", coordinatorOptions(request, null));
        request.setAttribute("canModifySubject", canModifySubject);
        request.setAttribute("canManageSubjectAssociations", canManageSubjectAssociations);
        request.setAttribute("canCreateClassGroupsForSubject",
                canCreateClassGroupsForSubject(actor, subjectId, request));
        request.setAttribute("canAssignSubjectCoordinators", canModifySubject);
        prepareSubjectContext(request, subjectView, "detail");
        prepareDashboard(request, "subjects", "Subject Detail");
        forward(request, response, subjectDetailJsp(request));
    }

    private void showSubjectForm(
            HttpServletRequest request,
            HttpServletResponse response,
            SubjectFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareSubjectForm(request, form, creating, error, null);
        forward(request, response, subjectFormJsp(request));
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long subjectId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        if (!subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        )) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        SubjectFormData form = error == null ? SubjectFormData.from(subject) : SubjectFormData.from(request, subjectId);
        prepareSubjectForm(request, form, false, error, subject);
        prepareSubjectContext(request, viewFactory.subjectView(subject), "edit");
        request.setAttribute("canModifySubject", Boolean.TRUE);
        request.setAttribute("canAssignSubjectCoordinators", Boolean.TRUE);
        forward(request, response, subjectFormJsp(request));
    }

    private void showCourseAssociationForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId,
            String error
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        SubjectView subjectView = viewFactory.subjectView(subject);
        List<SubjectCourseView> allAssociations = subjectCourseViews(subjectId);
        List<SubjectCourseView> associations = allAssociations.stream()
                .filter(association -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.getCourseId(),
                        subjectId,
                        request.getRemoteAddr()
                ))
                .toList();
        List<CourseView> availableCourseOptions = availableCourseOptions(request, subject, allAssociations);
        if ((!canManageSubjectAssociations(actor, subjectId, request)
                && associations.isEmpty()
                && availableCourseOptions.isEmpty())
                || subjectView.isArchived()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setAttribute("subject", subjectView);
        prepareSubjectBasePath(request);
        request.setAttribute("subjectCourseAssociations", associations);
        request.setAttribute("availableCourseOptions", availableCourseOptions);
        request.setAttribute("canModifySubject", subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        ));
        request.setAttribute("canManageSubjectAssociations", Boolean.TRUE);
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareSubjectContext(request, subjectView, "courses");
        prepareDashboard(request, "subjects", "Associate Courses");
        forward(request, response, subjectCourseFormJsp(request));
    }

    private void createSubject(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        SubjectFormData form = SubjectFormData.from(request, null);
        Part subjectImage;
        try {
            subjectImage = subjectImagePart(request);
        } catch (IOException | ServletException exception) {
            showSubjectForm(request, response, form, true, "The uploaded file could not be processed. Please try again.");
            return;
        }
        try {
            List<SubjectInitialCourseAssignment> initialCourseAssignments = selectedInitialCourseAssignments(request);
            long primaryInitialCourseId = initialCourseAssignments.stream()
                    .map(SubjectInitialCourseAssignment::courseId)
                    .findFirst()
                    .orElse(0L);
            Subject created = subjectService.createSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new SubjectCreateCommand(
                            longParameter(request, "organizationId"),
                            text(request, "name"),
                            text(request, "acronym"),
                            text(request, "photo"),
                            text(request, "description"),
                            optionalBigDecimal(request, "ects"),
                            optionalInteger(request, "workloadHours"),
                            subjectState(text(request, "state")),
                            primaryInitialCourseId,
                            optionalInteger(request, "initialCurricularYear"),
                            curricularTerm(text(request, "initialTerm")),
                            request.getParameter("initialMandatory") != null,
                            selectedCoordinator(text(request, "coordinatorUserId")),
                            initialCourseAssignments
                    ),
                    request.getRemoteAddr()
            );
            try {
                created = attachUploadedPhotoToCreatedSubject(request, actor, created, subjectImage);
            } catch (IOException exception) {
                flashError(request, "Subject created, but the uploaded image could not be processed. Please edit the subject and try again.");
                redirect(request, response, subjectBasePath(request) + "/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "Subject created, but " + messageFor(exception));
                redirect(request, response, subjectBasePath(request) + "/" + created.id());
                return;
            }
            flashSuccess(request, "Subject created successfully.");
            redirect(request, response, subjectBasePath(request) + "/" + created.id());
        } catch (RuntimeException exception) {
            showSubjectForm(request, response, form, true, messageFor(exception));
        }
    }

    private Subject attachUploadedPhotoToCreatedSubject(
            HttpServletRequest request,
            SessionUser actor,
            Subject created,
            Part subjectImage
    ) throws IOException {
        String uploadedPhoto = photoStorage.saveSubjectPhoto(created.id(), subjectImage, getServletContext());
        if (uploadedPhoto == null) {
            return created;
        }
        return subjectService.attachCreatedSubjectPhoto(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                created.id(),
                uploadedPhoto,
                request.getRemoteAddr()
        );
    }

    private void updateSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Subject existing = subjectService.getSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    request.getRemoteAddr()
            );
            String uploadedPhoto;
            try {
                uploadedPhoto = photoStorage.saveSubjectPhoto(
                        subjectId,
                        subjectImagePart(request),
                        getServletContext()
                );
            } catch (IOException | ServletException exception) {
                showEditForm(request, response, subjectId, "The uploaded file could not be processed. Please try again.");
                return;
            }
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            subjectService.updateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    new SubjectUpdateCommand(
                            text(request, "name"),
                            text(request, "acronym"),
                            photo,
                            text(request, "description"),
                            optionalBigDecimal(request, "ects"),
                            optionalInteger(request, "workloadHours"),
                            subjectState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject updated successfully.");
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, subjectId, messageFor(exception));
        }
    }

    private void assignCoordinator(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.assignCoordinator(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    longParameter(request, "coordinatorUserId"),
                    optionalDate(request, "startDate"),
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Coordinator assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, subjectBasePath(request) + "/" + subjectId);
    }

    private void archiveSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.archiveSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject archived.");
            redirect(request, response, subjectBasePath(request));
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        }
    }

    private void deleteSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.deleteSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject deleted.");
            redirect(request, response, subjectBasePath(request));
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        }
    }

    private void addCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.associateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseSubjectAssociationCommand(
                            longParameter(request, "courseId"),
                            subjectId,
                            optionalInteger(request, "curricularYear"),
                            curricularTerm(text(request, "term")),
                            request.getParameter("mandatory") != null,
                            courseSubjectState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course associated with subject.");
            redirect(request, response, subjectBasePath(request) + "/" + subjectId + "/courses");
        } catch (RuntimeException exception) {
            showCourseAssociationForm(request, response, subjectId, messageFor(exception));
        }
    }

    private void updateCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.updateAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseSubjectAssociationCommand(
                            courseId,
                            subjectId,
                            optionalInteger(request, "curricularYear"),
                            curricularTerm(text(request, "term")),
                            request.getParameter("mandatory") != null,
                            courseSubjectState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course association updated.");
            redirect(request, response, subjectBasePath(request) + "/" + subjectId + "/courses");
        } catch (RuntimeException exception) {
            showCourseAssociationForm(request, response, subjectId, messageFor(exception));
        }
    }

    private void removeCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.deleteAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    subjectId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course association removed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, subjectBasePath(request) + "/" + subjectId + "/courses");
    }

    private static List<SubjectInitialCourseAssignment> selectedInitialCourseAssignments(HttpServletRequest request) {
        String[] values = request.getParameterValues("initialCourseIds");
        if (values == null || values.length == 0) {
            values = request.getParameterValues("initialCourseId");
        }
        Integer curricularYear = optionalInteger(request, "initialCurricularYear");
        CurricularTerm term = curricularTerm(text(request, "initialTerm"));
        boolean mandatory = request.getParameter("initialMandatory") != null;
        Set<Long> courseIds = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    courseIds.add(Long.parseLong(value.trim()));
                }
            }
        }
        return courseIds.stream()
                .map(courseId -> new SubjectInitialCourseAssignment(courseId, curricularYear, term, mandatory))
                .toList();
    }

    private void prepareSubjectForm(
            HttpServletRequest request,
            SubjectFormData form,
            boolean creating,
            String error,
            Subject subjectForEdit
    ) {
        prepareSubjectBasePath(request);
        SessionUser actor = requireCurrentUser(request);
        List<Organization> organizations = isCoordinatorSubjectRequest(request)
                ? coordinatorSubjectOrganizations(actor, subjectForEdit)
                : managedOrganizations(request);
        long selectedOrganizationId = selectedOrganizationId(organizations, form);
        List<SubjectCourseView> associations = subjectForEdit == null
                ? List.of()
                : subjectCourseViews(subjectForEdit.id());
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("organizationOptions", organizations.stream()
                .map(organization -> OrganizationView.from(organization, 0))
                .toList());
        request.setAttribute("selectedOrganizationId", selectedOrganizationId);
        request.setAttribute("courseOptions", courseOptions(organizations));
        request.setAttribute("subjectCourseAssociations", associations);
        request.setAttribute("availableCourseOptions", subjectForEdit == null
                ? List.of()
                : availableCourseOptions(request, subjectForEdit, associations));
        request.setAttribute("coordinatorOptions", coordinatorOptions(request, parseOptionalLong(form.getCoordinatorUserId())));
        request.setAttribute("formAction", creating
                ? request.getContextPath() + subjectBasePath(request)
                : request.getContextPath() + subjectBasePath(request) + "/" + form.getId());
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "subjects", creating ? "Create Subject" : "Edit Subject");
    }

    private List<Organization> managedOrganizations(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        return organizationService.listManagedOrganizations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                AuthorizationPolicy.MANAGE_SUBJECTS,
                request.getRemoteAddr()
        );
    }

    private List<Subject> coordinatorSubjects(
            SessionUser actor,
            List<Organization> organizations,
            HttpServletRequest request
    ) {
        try {
            return coordinateSubjectDAO.findActiveSubjectIdsByCoordinator(actor.userId()).stream()
                    .map(subjectId -> subjectService.getSubject(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            subjectId,
                            request.getRemoteAddr()
                    ))
                    .sorted(Comparator.comparingLong(Subject::id))
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw new IllegalStateException("Failed to load coordinator subjects", exception);
        }
    }

    private List<Organization> coordinatorManagedOrganizations(SessionUser actor) {
        return List.of();
    }

    private List<Organization> coordinatorSubjectOrganizations(SessionUser actor, Subject subjectForEdit) {
        if (subjectForEdit == null) {
            return List.of();
        }
        try {
            return organizationDAO.findById(subjectForEdit.organizationId())
                    .map(organization -> List.of(organization))
                    .orElseGet(List::of);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject organization", exception);
        }
    }

    private void prepareSubjectBasePath(HttpServletRequest request) {
        request.setAttribute("subjectBasePath", subjectBasePath(request));
        request.setAttribute("subjectCourseBasePath", subjectCourseBasePath(request));
    }

    private static String subjectBasePath(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) ? "/coordinator/subjects" : "/admin/subjects";
    }

    private static String subjectCourseBasePath(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) ? "/courses" : "/admin/courses";
    }

    private static String subjectListJsp(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) ? COORDINATOR_SUBJECT_LIST_JSP : ADMIN_SUBJECT_LIST_JSP;
    }

    private static String subjectFormJsp(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) ? COORDINATOR_SUBJECT_FORM_JSP : ADMIN_SUBJECT_FORM_JSP;
    }

    private static String subjectDetailJsp(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) ? COORDINATOR_SUBJECT_DETAIL_JSP : ADMIN_SUBJECT_DETAIL_JSP;
    }

    private static String subjectCourseFormJsp(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request)
                ? COORDINATOR_SUBJECT_COURSE_FORM_JSP
                : ADMIN_SUBJECT_COURSE_FORM_JSP;
    }

    private static boolean isCoordinatorSubjectRequest(HttpServletRequest request) {
        return request.getServletPath() != null && request.getServletPath().startsWith("/coordinator/subjects");
    }

    private boolean canCreateAnySubject(SessionUser actor, List<Organization> organizations, HttpServletRequest request) {
        for (Organization organization : organizations) {
            if (subjectService.canCreateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organization.id(),
                    request.getRemoteAddr()
            )) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, Boolean> canModifySubjectById(
            SessionUser actor,
            List<Subject> subjects,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Subject subject : subjects) {
            permissions.put(subject.id(), subjectService.canModifySubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subject.id(),
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageSubjectAssociationsById(
            SessionUser actor,
            List<Subject> subjects,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Subject subject : subjects) {
            permissions.put(subject.id(), canManageSubjectAssociations(actor, subject.id(), request));
        }
        return permissions;
    }

    private boolean canManageSubjectAssociations(
            SessionUser actor,
            long subjectId,
            HttpServletRequest request
    ) {
        if (courseSubjectService.canManageSubjectAssociations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        )) {
            return true;
        }
        return subjectCourseViews(subjectId).stream()
                .anyMatch(association -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.getCourseId(),
                        subjectId,
                        request.getRemoteAddr()
                ));
    }

    private Map<Long, Map<Long, List<ClassGroupView>>> subjectCourseClassGroupsBySubjectAndCourse(
            Map<Long, List<SubjectCourseView>> coursesBySubject
    ) {
        Map<Long, Map<Long, List<ClassGroupView>>> classGroupsBySubjectAndCourse = new HashMap<>();
        for (Map.Entry<Long, List<SubjectCourseView>> subjectEntry : coursesBySubject.entrySet()) {
            Map<Long, List<ClassGroupView>> classGroupsByCourse = new HashMap<>();
            for (SubjectCourseView association : subjectEntry.getValue()) {
                classGroupsByCourse.put(
                        association.getCourseId(),
                        viewFactory.classGroupViews(classGroupsByCourseSubject(
                                association.getCourseId(),
                                subjectEntry.getKey()
                        ))
                );
            }
            classGroupsBySubjectAndCourse.put(subjectEntry.getKey(), classGroupsByCourse);
        }
        return classGroupsBySubjectAndCourse;
    }

    private static Set<Long> courseIdsFrom(Map<Long, List<SubjectCourseView>> coursesBySubject) {
        Set<Long> courseIds = new HashSet<>();
        for (List<SubjectCourseView> associations : coursesBySubject.values()) {
            for (SubjectCourseView association : associations) {
                courseIds.add(association.getCourseId());
            }
        }
        return courseIds;
    }

    private static Set<Long> classGroupIdsFrom(
            Map<Long, Map<Long, List<ClassGroupView>>> classGroupsBySubjectAndCourse
    ) {
        Set<Long> classGroupIds = new HashSet<>();
        for (Map<Long, List<ClassGroupView>> classGroupsByCourse : classGroupsBySubjectAndCourse.values()) {
            for (List<ClassGroupView> classGroups : classGroupsByCourse.values()) {
                for (ClassGroupView classGroup : classGroups) {
                    classGroupIds.add(classGroup.getId());
                }
            }
        }
        return classGroupIds;
    }

    private Map<Long, Boolean> canModifyCourseById(
            SessionUser actor,
            Set<Long> courseIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long courseId : courseIds) {
            permissions.put(courseId, courseService.canModifyCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageCourseChildrenById(
            SessionUser actor,
            Set<Long> courseIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long courseId : courseIds) {
            permissions.put(courseId, courseService.canManageCourseChildren(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canModifyClassGroupById(
            SessionUser actor,
            Set<Long> classGroupIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long classGroupId : classGroupIds) {
            permissions.put(classGroupId, classGroupService.canModifyClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageClassGroupStructureById(
            SessionUser actor,
            Set<Long> classGroupIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long classGroupId : classGroupIds) {
            permissions.put(classGroupId, classGroupService.canManageClassGroupStructure(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private boolean canCreateClassGroupsForSubject(
            SessionUser actor,
            long subjectId,
            HttpServletRequest request
    ) {
        try {
            for (CourseSubjectAssociation association : courseSubjectDAO.findBySubject(subjectId)) {
                if (association.state() != CourseSubjectState.ACTIVE) {
                    continue;
                }
                if (classGroupService.canCreateClassGroup(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.courseId(),
                        subjectId,
                        request.getRemoteAddr()
                )) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException | SQLException exception) {
            return false;
        }
    }

    private List<CourseView> courseOptions(List<Organization> organizations) {
        return organizations.stream()
                .flatMap(organization -> coursesByOrganization(organization.id()).stream())
                .filter(course -> course.state() != CourseState.ARCHIVED)
                .map(viewFactory::courseView)
                .sorted(Comparator.comparing(CourseView::getName))
                .toList();
    }

    private List<CourseView> availableCourseOptions(
            HttpServletRequest request,
            Subject subject,
            List<SubjectCourseView> associations
    ) {
        SessionUser actor = requireCurrentUser(request);
        Set<Long> associatedCourseIds = new HashSet<>();
        for (SubjectCourseView association : associations) {
            associatedCourseIds.add(association.getCourseId());
        }
        return coursesByOrganization(subject.organizationId()).stream()
                .filter(course -> course.state() != CourseState.ARCHIVED)
                .filter(course -> !associatedCourseIds.contains(course.id()))
                .filter(course -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        course.id(),
                        subject.id(),
                        request.getRemoteAddr()
                ))
                .map(viewFactory::courseView)
                .sorted(Comparator.comparing(CourseView::getName))
                .toList();
    }

    private List<Course> coursesByOrganization(long organizationId) {
        try {
            return courseDAO.findByOrganization(organizationId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course options", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsBySubject(long subjectId) {
        try {
            return classGroupDAO.findBySubject(subjectId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourseSubject(long courseId, long subjectId) {
        try {
            return classGroupDAO.findByCourseAndSubject(courseId, subjectId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject class groups", exception);
        }
    }

    private Map<Long, List<SubjectCourseView>> subjectCoursesBySubject(List<SubjectView> subjects) {
        Map<Long, List<SubjectCourseView>> coursesBySubject = new HashMap<>();
        for (SubjectView subject : subjects) {
            coursesBySubject.put(subject.getId(), subjectCourseViews(subject.getId()));
        }
        return coursesBySubject;
    }

    private List<SubjectCourseView> subjectCourseViews(long subjectId) {
        try {
            return courseSubjectDAO.findBySubject(subjectId).stream()
                    .map(this::subjectCourseView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject course associations", exception);
        }
    }

    private SubjectCourseView subjectCourseView(CourseSubjectAssociation association) {
        try {
            Course course = courseDAO.findById(association.courseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + association.courseId()));
            return SubjectCourseView.from(association, viewFactory.courseView(course));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course association", exception);
        }
    }

    private List<UserOptionView> coordinatorOptions(HttpServletRequest request, Long selectedId) {
        SessionUser actor = requireCurrentUser(request);
        try {
            return userService.listUsers(actor.userId(), currentSessionId(request), primaryProfile(actor), request.getRemoteAddr())
                    .stream()
                    .filter(SubjectManagementServlet::isActiveCoordinator)
                    .map(user -> UserOptionView.from(user, selectedId != null && selectedId == user.id()))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private Long firstManagedOrganizationId(HttpServletRequest request) {
        return managedOrganizations(request).stream().map(Organization::id).findFirst().orElse(null);
    }

    private long selectedOrganizationId(HttpServletRequest request, SubjectFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        Long first = firstManagedOrganizationId(request);
        return first == null ? 0 : first;
    }

    private static long selectedOrganizationId(List<Organization> organizations, SubjectFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        return organizations.stream().map(Organization::id).findFirst().orElse(0L);
    }

    private static boolean isActiveCoordinator(User user) {
        return user.state() == UserState.ACTIVE
                && user.accessProfiles().stream()
                .map(AccessProfile::type)
                .anyMatch(AccessProfileType.COORDINATOR::equals);
    }

    private static void prepareSubjectContext(HttpServletRequest request, SubjectView subject, String activeChild) {
        request.setAttribute("adminSubjectContextId", subject.getId());
        request.setAttribute("adminSubjectContextName", subject.getName());
        request.setAttribute("adminSubjectActiveChild", activeChild);
        request.setAttribute("coordinatorSubjectContextId", subject.getId());
        request.setAttribute("coordinatorSubjectContextName", subject.getName());
        request.setAttribute("coordinatorSubjectActiveChild", activeChild);
    }

    private static Set<Long> selectedCoordinator(String value) {
        Long coordinatorId = parseOptionalLong(value);
        return coordinatorId == null ? Set.of() : Set.of(coordinatorId);
    }

    private static SubjectState subjectState(String value) {
        return enumValue(SubjectState.class, value, SubjectState.ACTIVE, "subject state");
    }

    private static CurricularTerm curricularTerm(String value) {
        return enumValue(CurricularTerm.class, value, null, "curricular term");
    }

    private static CourseSubjectState courseSubjectState(String value) {
        return enumValue(CourseSubjectState.class, value, CourseSubjectState.ACTIVE, "association state");
    }

    private static <T extends Enum<T>> T enumValue(Class<T> enumType, String value, T defaultValue, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + fieldLabel + ": " + value, exception);
        }
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static BigDecimal optionalBigDecimal(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : new BigDecimal(value);
    }

    private static Part subjectImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("subjectImage");
    }

    private static Long parseOptionalLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }
}
