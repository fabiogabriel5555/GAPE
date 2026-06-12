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
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
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
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.OrganizationView;
import pt.isel.gape.web.view.SubjectCourseView;
import pt.isel.gape.web.view.SubjectFormData;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

@WebServlet(name = "subjectManagementServlet", urlPatterns = {"/admin/subjects", "/admin/subjects/*"})
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public final class SubjectManagementServlet extends DashboardServletSupport {

    private static final String SUBJECT_LIST_JSP = "/admin/admin/subject/admin-subjects.jsp";
    private static final String SUBJECT_FORM_JSP = "/admin/admin/subject/admin-subject-form.jsp";
    private static final String SUBJECT_DETAIL_JSP = "/admin/admin/subject/admin-subject-detail.jsp";
    private static final String SUBJECT_COURSE_FORM_JSP = "/admin/admin/subject/admin-subject-course-form.jsp";

    private final SubjectService subjectService;
    private final CourseSubjectService courseSubjectService;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final LearningViewFactory viewFactory;
    private final ProfilePhotoStorage photoStorage;

    public SubjectManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private SubjectManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new SubjectService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new OrganizationService(connectionProvider, clock),
                new UserService(connectionProvider, clock),
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new LearningViewFactory(
                        new OrganizationDAO(connectionProvider),
                        new OrganicUnitDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new CourseSubjectDAO(connectionProvider),
                        new EnrollmentDAO(connectionProvider)
                ),
                new ProfilePhotoStorage()
        );
    }

    SubjectManagementServlet(
            SubjectService subjectService,
            CourseSubjectService courseSubjectService,
            OrganizationService organizationService,
            UserService userService,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            LearningViewFactory viewFactory,
            ProfilePhotoStorage photoStorage
    ) {
        this.subjectService = subjectService;
        this.courseSubjectService = courseSubjectService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
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
        List<Organization> organizations = managedOrganizations(request);
        List<Subject> rawSubjects = organizations.stream()
                .flatMap(organization -> subjectService.listSubjects(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        organization.id(),
                        request.getRemoteAddr()
                ).stream())
                .sorted(Comparator.comparing(Subject::name))
                .toList();
        List<SubjectView> subjects = rawSubjects.stream()
                .map(viewFactory::subjectView)
                .toList();

        request.setAttribute("subjects", subjects);
        request.setAttribute("canCreateSubjects", !organizations.isEmpty());
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, rawSubjects, request));
        request.setAttribute("subjectCoursesBySubject", subjectCoursesBySubject(subjects));
        request.setAttribute("subjectCount", subjects.size());
        request.setAttribute("activeSubjects", subjects.stream().filter(SubjectView::isActive).count());
        request.setAttribute("archivedSubjects", subjects.stream().filter(SubjectView::isArchived).count());
        prepareDashboard(
                request,
                "subjects",
                "Subjects",
                !organizations.isEmpty() ? "/admin/subjects/new" : null,
                !organizations.isEmpty() ? "New Subject" : null
        );
        forward(request, response, SUBJECT_LIST_JSP);
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
        request.setAttribute("subject", subjectView);
        request.setAttribute("subjectCourseAssociations", subjectCourseViews(subjectId));
        request.setAttribute("coordinatorOptions", coordinatorOptions(request, null));
        request.setAttribute("canModifySubject", canModifySubject);
        request.setAttribute("canAssignSubjectCoordinators", canModifySubject
                && primaryProfile(actor) == AccessProfileType.ADMINISTRATOR);
        prepareSubjectContext(request, subjectView, "detail");
        prepareDashboard(request, "subjects", "Subject Detail");
        forward(request, response, SUBJECT_DETAIL_JSP);
    }

    private void showSubjectForm(
            HttpServletRequest request,
            HttpServletResponse response,
            SubjectFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareSubjectForm(request, form, creating, error, null);
        forward(request, response, SUBJECT_FORM_JSP);
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
        request.setAttribute("canAssignSubjectCoordinators", primaryProfile(actor) == AccessProfileType.ADMINISTRATOR);
        forward(request, response, SUBJECT_FORM_JSP);
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
        boolean canModifySubject = subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        if (!canModifySubject || subjectView.isArchived()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        List<SubjectCourseView> associations = subjectCourseViews(subjectId);

        request.setAttribute("subject", subjectView);
        request.setAttribute("subjectCourseAssociations", associations);
        request.setAttribute("availableCourseOptions", availableCourseOptions(subject, associations));
        request.setAttribute("canModifySubject", Boolean.TRUE);
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareSubjectContext(request, subjectView, "courses");
        prepareDashboard(request, "subjects", "Associate Courses");
        forward(request, response, SUBJECT_COURSE_FORM_JSP);
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
                redirect(request, response, "/admin/subjects/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "Subject created, but " + messageFor(exception));
                redirect(request, response, "/admin/subjects/" + created.id());
                return;
            }
            flashSuccess(request, "Subject created successfully.");
            redirect(request, response, "/admin/subjects/" + created.id());
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
            redirect(request, response, "/admin/subjects/" + subjectId);
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
        redirect(request, response, "/admin/subjects/" + subjectId);
    }

    private void archiveSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.archiveSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject archived.");
            redirect(request, response, "/admin/subjects");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/subjects/" + subjectId);
        }
    }

    private void deleteSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.deleteSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject deleted.");
            redirect(request, response, "/admin/subjects");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/subjects/" + subjectId);
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
            redirect(request, response, "/admin/subjects/" + subjectId + "/courses");
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
            redirect(request, response, "/admin/subjects/" + subjectId + "/courses");
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
        redirect(request, response, "/admin/subjects/" + subjectId + "/courses");
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
        List<Organization> organizations = managedOrganizations(request);
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
                : availableCourseOptions(subjectForEdit, associations));
        request.setAttribute("coordinatorOptions", coordinatorOptions(request, parseOptionalLong(form.getCoordinatorUserId())));
        request.setAttribute("formAction", creating
                ? request.getContextPath() + "/admin/subjects"
                : request.getContextPath() + "/admin/subjects/" + form.getId());
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
                request.getRemoteAddr()
        );
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

    private List<CourseView> courseOptions(List<Organization> organizations) {
        return organizations.stream()
                .flatMap(organization -> coursesByOrganization(organization.id()).stream())
                .filter(course -> course.state() != CourseState.ARCHIVED)
                .map(viewFactory::courseView)
                .sorted(Comparator.comparing(CourseView::getName))
                .toList();
    }

    private List<CourseView> availableCourseOptions(Subject subject, List<SubjectCourseView> associations) {
        Set<Long> associatedCourseIds = new HashSet<>();
        for (SubjectCourseView association : associations) {
            associatedCourseIds.add(association.getCourseId());
        }
        return coursesByOrganization(subject.organizationId()).stream()
                .filter(course -> course.state() != CourseState.ARCHIVED)
                .filter(course -> !associatedCourseIds.contains(course.id()))
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
