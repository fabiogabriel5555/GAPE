package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitCreateCommand;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;
import pt.isel.gape.structure.model.OrganicUnitUpdateCommand;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationCreateCommand;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationType;
import pt.isel.gape.structure.model.OrganizationUpdateCommand;
import pt.isel.gape.structure.service.OrganicUnitService;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.AdministratorOptionView;
import pt.isel.gape.web.view.OrganicUnitAdministratorView;
import pt.isel.gape.web.view.OrganicUnitFormData;
import pt.isel.gape.web.view.OrganicUnitView;
import pt.isel.gape.web.view.OrganizationAdministratorView;
import pt.isel.gape.web.view.OrganizationClassGroupTreeView;
import pt.isel.gape.web.view.OrganizationCourseTreeView;
import pt.isel.gape.web.view.OrganizationFormData;
import pt.isel.gape.web.view.OrganizationSubjectTreeView;
import pt.isel.gape.web.view.OrganizationView;

@WebServlet(name = "organizationManagementServlet", urlPatterns = {"/admin/organizations", "/admin/organizations/*"})
@MultipartConfig(maxFileSize = 50L * 1024L * 1024L, maxRequestSize = 52L * 1024L * 1024L)
public final class OrganizationManagementServlet extends DashboardServletSupport {

    private static final String ORGANIZATIONS_LIST_JSP = "/admin/admin/organization/admin-organizations.jsp";
    private static final String ORGANIZATION_DETAIL_JSP = "/admin/admin/organization/admin-organization-detail.jsp";
    private static final String ORGANIZATION_FORM_JSP = "/admin/admin/organization/admin-organization-form.jsp";
    private static final String ORGANIC_UNIT_DETAIL_JSP = "/admin/admin/organization/admin-organic-unit-detail.jsp";
    private static final String ORGANIC_UNIT_FORM_JSP = "/admin/admin/organization/admin-organic-unit-form.jsp";

    private final OrganizationService organizationService;
    private final OrganicUnitService organicUnitService;
    private final UserService userService;
    private final ProfilePhotoStorage photoStorage;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final CourseService courseService;
    private final SubjectService subjectService;
    private final CourseSubjectService courseSubjectService;
    private final ClassGroupService classGroupService;
    private final ClassGroupActivityViewSupport activityViewSupport;

    public OrganizationManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private OrganizationManagementServlet(ConnectionProvider connectionProvider, java.time.Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new OrganizationService(connectionProvider, clock),
                new OrganicUnitService(connectionProvider, clock),
                new UserService(connectionProvider),
                new ProfilePhotoStorage(),
                new CourseService(connectionProvider, clock),
                new SubjectService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new ClassGroupActivityViewSupport(connectionProvider, clock)
        );
    }

    OrganizationManagementServlet(
            ApplicationReadService readService,
            OrganizationService organizationService,
            OrganicUnitService organicUnitService,
            UserService userService,
            ProfilePhotoStorage photoStorage,
            CourseService courseService,
            SubjectService subjectService,
            CourseSubjectService courseSubjectService,
            ClassGroupService classGroupService,
            ClassGroupActivityViewSupport activityViewSupport
    ) {
        this.organizationService = organizationService;
        this.organicUnitService = organicUnitService;
        this.userService = userService;
        this.photoStorage = photoStorage;
        this.courseDAO = readService.courses();
        this.subjectDAO = readService.subjects();
        this.courseSubjectDAO = readService.courseSubjects();
        this.classGroupDAO = readService.classGroups();
        this.courseService = courseService;
        this.subjectService = subjectService;
        this.courseSubjectService = courseSubjectService;
        this.classGroupService = classGroupService;
        this.activityViewSupport = activityViewSupport;
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
                SessionUser actor = requireCurrentUser(request);
                showCreateForm(request, response, OrganizationFormData.blank(actor.userId()), null);
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
            if (segments.length == 2 && "units".equals(segments[1])) {
                redirect(request, response, "/admin/organizations");
                return;
            }
            if (segments.length == 3 && "units".equals(segments[1]) && "new".equals(segments[2])) {
                redirect(request, response, "/admin/organizations/" + Long.parseLong(segments[0]) + "?unitModal=create");
                return;
            }
            if (segments.length == 3 && "units".equals(segments[1])) {
                redirect(request, response, "/admin/organizations/" + Long.parseLong(segments[0])
                        + "?unitDetail=" + Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "units".equals(segments[1]) && "edit".equals(segments[3])) {
                redirect(request, response, "/admin/organizations/" + Long.parseLong(segments[0])
                        + "?unitEdit=" + Long.parseLong(segments[2]));
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
                createOrganization(request, response);
                return;
            }
            if (segments.length == 1) {
                updateOrganization(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "units".equals(segments[1])) {
                createOrganicUnit(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2) {
                long organizationId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "assign-admin" -> assignAdministrator(request, response, organizationId);
                    case "archive" -> archiveOrganization(request, response, organizationId);
                    case "delete" -> deleteOrganization(request, response, organizationId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 3 && "units".equals(segments[1])) {
                updateOrganicUnit(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "units".equals(segments[1])) {
                long organizationId = Long.parseLong(segments[0]);
                long organicUnitId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "delete" -> deleteOrganicUnit(request, response, organizationId, organicUnitId);
                    case "assign-admin" -> assignOrganicUnitAdministrator(request, response, organizationId, organicUnitId);
                    case "revoke-admin" -> revokeOrganicUnitAdministrator(request, response, organizationId, organicUnitId);
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
        SessionUser actor = requireCurrentUser(request);
        List<Organization> organizations = organizationService.listManagedOrganizations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                request.getRemoteAddr()
        );
        List<OrganizationView> views = new ArrayList<>();
        Map<Long, Boolean> canCreateOrganicUnitsByOrganizationId = new HashMap<>();
        Map<Long, Boolean> canModifyOrganicUnitById = new HashMap<>();
        for (Organization organization : organizations) {
            List<OrganicUnit> units = safeOrganicUnits(actor, organization.id(), request);
            OrganizationView view = OrganizationView.from(organization, hierarchyViews(units, null));
            views.add(view);
            canCreateOrganicUnitsByOrganizationId.put(
                    organization.id(),
                    canCreateAnyOrganicUnit(actor, organization.id(), units, request) && !view.isInactive()
            );
            canModifyOrganicUnitById.putAll(canModifyOrganicUnitById(actor, units, request));
        }
        views.sort(Comparator.comparingLong(OrganizationView::getId).reversed());
        request.setAttribute("organizations", views);
        request.setAttribute("canCreateOrganicUnitsByOrganizationId", canCreateOrganicUnitsByOrganizationId);
        request.setAttribute("canModifyOrganicUnitById", canModifyOrganicUnitById);
        request.setAttribute("organizationCount", views.size());
        request.setAttribute("activeOrganizations", views.stream().filter(OrganizationView::isActive).count());
        request.setAttribute("inactiveOrganizations", views.stream().filter(OrganizationView::isInactive).count());
        request.setAttribute("unitTotal", views.stream().mapToInt(OrganizationView::getOrganicUnitCount).sum());
        boolean canModifyOrganizations = canManageOrganizationRoots(actor);
        request.setAttribute("canCreateOrganizations", canModifyOrganizations);
        request.setAttribute("canModifyOrganizations", canModifyOrganizations);
        prepareDashboard(
                request,
                "organizations",
                "Organizations",
                canModifyOrganizations ? "/admin/organizations/new" : null,
                canModifyOrganizations ? "New Organization" : null
        );
        forward(request, response, ORGANIZATIONS_LIST_JSP);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        markLearningEventsReadForCurrentUser(request, "/admin/organizations/" + organization.id(), false);
        List<OrganicUnit> units = organicUnitService.listOrganicUnits(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        Map<Long, List<OrganizationCourseTreeView>> coursesByUnit = courseTreesByOrganicUnit(organizationId);
        OrganizationView organizationView = OrganizationView.from(organization, hierarchyViews(units, null, coursesByUnit));
        Set<Long> courseIds = new HashSet<>();
        Set<Long> subjectIds = new HashSet<>();
        Set<Long> classGroupIds = new HashSet<>();
        collectPedagogicalTreeIds(organizationView, courseIds, subjectIds, classGroupIds);
        boolean canModifyOrganization = canManageOrganizationRoots(actor);
        boolean canCreateOrganicUnits = canCreateAnyOrganicUnit(actor, organizationId, units, request)
                && !organizationView.isInactive();
        request.setAttribute("organization", organizationView);
        request.setAttribute("organicUnits", organizationView.getOrganicUnits());
        request.setAttribute("canModifyOrganization", canModifyOrganization);
        request.setAttribute("canAssignOrganizationAdministrators", canModifyOrganization);
        request.setAttribute("canCreateOrganicUnits", canCreateOrganicUnits);
        request.setAttribute("canModifyOrganicUnitById", canModifyOrganicUnitById(actor, units, request));
        request.setAttribute("canModifyCourseById", canModifyCourseById(actor, courseIds, request));
        request.setAttribute("canManageCourseChildrenById", canManageCourseChildrenById(actor, courseIds, request));
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, subjectIds, request));
        request.setAttribute("canManageSubjectAssociationsById",
                canManageSubjectAssociationsById(actor, subjectIds, request));
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        activityViewSupport.exposeClassGroupActivities(
                request,
                actor,
                currentSessionId(request),
                primaryProfile(actor),
                classGroupIds
        );
        exposeOrganizationElementCounts(request, organizationView);
        request.setAttribute("assignedAdministrators", organizationService.listAdministrators(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        ).stream().map(OrganizationAdministratorView::from).toList());
        request.setAttribute("administratorOptions", administratorOptions(request, Set.of()));
        prepareOrganizationContext(request, organizationView, "detail");
        prepareDashboard(request, "organizations", "Organization Details");
        forward(request, response, ORGANIZATION_DETAIL_JSP);
    }

    private void showUnitDetail(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        OrganicUnit unit = requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId);
        if (unit == null) {
            return;
        }
        List<OrganicUnit> units = safeOrganicUnits(actor, organizationId, request);
        OrganizationView organizationView = OrganizationView.from(organization, units.size());
        OrganicUnitView unitView = organicUnitView(units, unit);
        boolean canModifyUnit = organicUnitService.canModifyOrganicUnit(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                unitId,
                request.getRemoteAddr()
        ) && !unitView.isInactive();
        request.setAttribute("organization", organizationView);
        request.setAttribute("unit", unitView);
        request.setAttribute("canModifyUnit", canModifyUnit);
        request.setAttribute("canModifyOrganization", canManageOrganizationRoots(actor));
        request.setAttribute("canCreateOrganicUnits", canCreateAnyOrganicUnit(actor, organizationId, units, request)
                && !organizationView.isInactive());
        prepareUnitAdministratorPanel(request, actor, organizationId, unitId);
        prepareOrganizationContext(request, organizationView, "unit-detail");
        prepareDashboard(request, "organizations", "Organic Unit Detail");
        forward(request, response, ORGANIC_UNIT_DETAIL_JSP);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response, OrganizationFormData form, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        if (!canManageOrganizationRoots(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        request.setAttribute("form", form);
        request.setAttribute("creating", Boolean.TRUE);
        request.setAttribute("formAction", request.getContextPath() + "/admin/organizations");
        request.setAttribute("administratorOptions", administratorOptions(request, form.getAdministratorUserIds()));
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        request.setAttribute("adminOrganizationActiveChild", "new");
        prepareDashboard(request, "organizations", "Create Organization");
        forward(request, response, ORGANIZATION_FORM_JSP);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long organizationId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        if (!canManageOrganizationRoots(actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        OrganizationFormData form = error == null
                ? OrganizationFormData.from(organization)
                : OrganizationFormData.from(request, organizationId);
        OrganizationView organizationView = OrganizationView.from(organization, safeOrganicUnits(actor, organizationId, request).size());
        request.setAttribute("form", form);
        request.setAttribute("creating", Boolean.FALSE);
        request.setAttribute("formAction", request.getContextPath() + "/admin/organizations/" + organizationId);
        prepareOrganizationContext(request, organizationView, "edit");
        request.setAttribute("canModifyOrganization", Boolean.TRUE);
        request.setAttribute("canCreateOrganicUnits", Boolean.FALSE);
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "organizations", "Edit Organization");
        forward(request, response, ORGANIZATION_FORM_JSP);
    }

    private void showUnitCreateForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long organizationId,
            OrganicUnitFormData form,
            String error
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        List<OrganicUnit> units = safeOrganicUnits(actor, organizationId, request);
        if (!canCreateAnyOrganicUnit(actor, organizationId, units, request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        prepareUnitForm(request, organization, form, true, null, error);
        forward(request, response, ORGANIC_UNIT_FORM_JSP);
    }

    private void showUnitEditForm(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        OrganicUnit unit = requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId);
        if (unit == null) {
            return;
        }
        if (!organicUnitService.canModifyOrganicUnit(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                unitId,
                request.getRemoteAddr()
        )) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        OrganicUnitFormData form = error == null
                ? OrganicUnitFormData.from(unit)
                : OrganicUnitFormData.from(request, unitId, organizationId, unit.code());
        prepareUnitForm(request, organization, form, false, unitId, error);
        forward(request, response, ORGANIC_UNIT_FORM_JSP);
    }

    private void createOrganization(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        OrganizationFormData form = OrganizationFormData.from(request, null);
        Part organizationImage;
        try {
            organizationImage = organizationImagePart(request);
        } catch (IOException | ServletException exception) {
            showCreateForm(request, response, form, "The uploaded file could not be processed. Please try again.");
            return;
        }

        try {
            Organization created = organizationService.createOrganization(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new OrganizationCreateCommand(
                            text(request, "name"),
                            text(request, "acronym"),
                            text(request, "photo"),
                            organizationType(text(request, "type")),
                            organizationState(text(request, "state")),
                            form.getAdministratorUserIds()
                    ),
                    request.getRemoteAddr()
            );
            try {
                created = attachUploadedPhotoToCreatedOrganization(request, actor, created, organizationImage);
            } catch (IOException exception) {
                flashError(request, "Organization created, but the uploaded image could not be processed. Please edit the organization and try again.");
                redirect(request, response, "/admin/organizations/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "Organization created, but " + messageFor(exception));
                redirect(request, response, "/admin/organizations/" + created.id());
                return;
            }
            flashSuccess(request, "Organization created successfully.");
            redirect(request, response, "/admin/organizations/" + created.id());
        } catch (RuntimeException exception) {
            showCreateForm(request, response, form, messageFor(exception));
        }
    }

    private Organization attachUploadedPhotoToCreatedOrganization(
            HttpServletRequest request,
            SessionUser actor,
            Organization created,
            Part organizationImage
    ) throws IOException {
        String uploadedPhoto = photoStorage.saveOrganizationPhoto(
                created.id(),
                organizationImage,
                getServletContext()
        );
        if (uploadedPhoto == null) {
            return created;
        }
        return organizationService.attachCreatedOrganizationPhoto(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                created.id(),
                uploadedPhoto,
                request.getRemoteAddr()
        );
    }

    private void updateOrganization(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Organization existing = organizationService.getOrganization(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    request.getRemoteAddr()
            );
            String uploadedPhoto;
            try {
                uploadedPhoto = photoStorage.saveOrganizationPhoto(
                        organizationId,
                        organizationImagePart(request),
                        getServletContext()
                );
            } catch (IOException exception) {
                showEditForm(
                        request,
                        response,
                        organizationId,
                        "The uploaded file could not be processed. Please try again."
                );
                return;
            }
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            organizationService.updateOrganization(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    new OrganizationUpdateCommand(
                            text(request, "name"),
                            text(request, "acronym"),
                            photo,
                            organizationType(text(request, "type")),
                            organizationState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organization updated successfully.");
            redirect(request, response, "/admin/organizations/" + organizationId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, organizationId, messageFor(exception));
        }
    }

    private void assignAdministrator(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            organizationService.assignAdministrator(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    longParameter(request, "administratorUserId"),
                    optionalDate(request, "startDate"),
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Administrator assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/admin/organizations/" + organizationId);
    }

    private void archiveOrganization(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            organizationService.archiveOrganization(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organization deactivated.");
            redirect(request, response, "/admin/organizations");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/organizations/" + organizationId);
        }
    }

    private void deleteOrganization(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            organizationService.deleteOrganization(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organization deleted.");
            redirect(request, response, "/admin/organizations");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/organizations/" + organizationId);
        }
    }

    private void createOrganicUnit(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        OrganicUnitFormData form = OrganicUnitFormData.from(request, null, organizationId);
        try {
            organicUnitService.createOrganicUnit(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new OrganicUnitCreateCommand(
                            organizationId,
                            null,
                            text(request, "name"),
                            text(request, "acronym"),
                            organicUnitType(text(request, "type")),
                            organicUnitState(text(request, "state")),
                            optionalLong(request, "parentOrganicUnitId")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organic unit created successfully.");
            if (isDynamicOrganizationRequest(request)) {
                renderOrganizationDetailForDynamicRequest(request, response, organizationId, HttpServletResponse.SC_OK);
                return;
            }
            redirectToReturnPath(request, response, "/admin/organizations/" + organizationId);
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            if (isDynamicOrganizationRequest(request)) {
                renderOrganizationDetailForDynamicRequest(request, response, organizationId, 422);
                return;
            }
            redirectToReturnPath(request, response, "/admin/organizations/" + organizationId + "?unitModal=create");
        }
    }

    private void updateOrganicUnit(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        if (requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId) == null) {
            return;
        }
        try {
            organicUnitService.updateOrganicUnit(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    unitId,
                    new OrganicUnitUpdateCommand(
                            null,
                            text(request, "name"),
                            text(request, "acronym"),
                            organicUnitType(text(request, "type")),
                            organicUnitState(text(request, "state")),
                            optionalLong(request, "parentOrganicUnitId")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organic unit updated successfully.");
            if (isDynamicOrganizationRequest(request)) {
                renderOrganizationDetailForDynamicRequest(request, response, organizationId, HttpServletResponse.SC_OK);
                return;
            }
            redirectToReturnPath(request, response, "/admin/organizations/" + organizationId);
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            if (isDynamicOrganizationRequest(request)) {
                renderOrganizationDetailForDynamicRequest(request, response, organizationId, 422);
                return;
            }
            redirectToReturnPath(request, response, "/admin/organizations/" + organizationId + "?unitEdit=" + unitId);
        }
    }

    private void renderOrganizationDetailForDynamicRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long organizationId,
            int status
    ) throws ServletException, IOException {
        response.setStatus(status);
        showDetail(request, response, organizationId);
    }

    private static boolean isDynamicOrganizationRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private void deleteOrganicUnit(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId) == null) {
            return;
        }
        try {
            organicUnitService.deleteOrganicUnit(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    unitId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organic unit deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/admin/organizations");
    }

    private void assignOrganicUnitAdministrator(
            HttpServletRequest request,
            HttpServletResponse response,
            long organizationId,
            long unitId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId) == null) {
            return;
        }
        try {
            organicUnitService.assignOrganicUnitAdministrator(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    unitId,
                    longParameter(request, "administratorUserId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organic unit administrator assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/organizations/" + organizationId + "/units/" + unitId);
    }

    private void revokeOrganicUnitAdministrator(
            HttpServletRequest request,
            HttpServletResponse response,
            long organizationId,
            long unitId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (requireOrganicUnitInOrganization(request, response, actor, organizationId, unitId) == null) {
            return;
        }
        try {
            organicUnitService.revokeOrganicUnitAdministrator(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    unitId,
                    longParameter(request, "administratorUserId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Organic unit administrator removed successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/organizations/" + organizationId + "/units/" + unitId);
    }

    private void prepareUnitForm(
            HttpServletRequest request,
            Organization organization,
            OrganicUnitFormData form,
            boolean creating,
            Long editingUnitId,
            String error
    ) {
        SessionUser actor = requireCurrentUser(request);
        List<OrganicUnit> units = safeOrganicUnits(actor, organization.id(), request);
        OrganizationView organizationView = OrganizationView.from(organization, units.size());
        request.setAttribute("organization", organizationView);
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("unitFormAction", creating
                ? request.getContextPath() + "/admin/organizations/" + organization.id() + "/units"
                : request.getContextPath() + "/admin/organizations/" + organization.id() + "/units/" + editingUnitId);
        request.setAttribute("unitBackHref", creating
                ? request.getContextPath() + "/admin/organizations"
                : request.getContextPath() + "/admin/organizations/" + organization.id() + "/units/" + editingUnitId);
        request.setAttribute("parentOptions", hierarchyViews(units, editingUnitId));
        prepareOrganizationContext(request, organizationView, creating ? "unit-new" : "unit-edit");
        request.setAttribute("canModifyOrganization", canManageOrganizationRoots(actor));
        request.setAttribute("canCreateOrganicUnits", canCreateAnyOrganicUnit(actor, organization.id(), units, request)
                && !organizationView.isInactive());
        if (!creating && editingUnitId != null) {
            OrganicUnit editingUnit = units.stream()
                    .filter(unit -> unit.id() == editingUnitId)
                    .findFirst()
                    .orElse(null);
            if (editingUnit != null) {
                request.setAttribute("unit", organicUnitView(units, editingUnit));
                prepareUnitAdministratorPanel(request, actor, organization.id(), editingUnitId);
            }
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "organizations", creating ? "Create Organic Unit" : "Edit Organic Unit");
    }

    private void prepareUnitAdministratorPanel(
            HttpServletRequest request,
            SessionUser actor,
            long organizationId,
            long unitId
    ) {
        request.setAttribute("assignedOrganicUnitAdministrators",
                assignedOrganicUnitAdministrators(request, actor, unitId));
        try {
            request.setAttribute("organicUnitAdministratorOptions",
                    organicUnitAdministratorOptions(request, actor, unitId));
            request.setAttribute("canManageOrganicUnitAdministrators", Boolean.TRUE);
        } catch (RuntimeException exception) {
            request.setAttribute("organicUnitAdministratorOptions", List.of());
            request.setAttribute("canManageOrganicUnitAdministrators", Boolean.FALSE);
        }
        String returnPath = "/admin/organizations/" + organizationId + "/units/" + unitId;
        request.setAttribute("unitAdminReturnPath", returnPath);
        request.setAttribute("unitAdminAssignAction",
                request.getContextPath() + "/admin/organizations/" + organizationId + "/units/" + unitId + "/assign-admin");
        request.setAttribute("unitAdminRevokeAction",
                request.getContextPath() + "/admin/organizations/" + organizationId + "/units/" + unitId + "/revoke-admin");
    }

    private List<OrganicUnitAdministratorView> assignedOrganicUnitAdministrators(
            HttpServletRequest request,
            SessionUser actor,
            long unitId
    ) {
        try {
            return organicUnitService.listDirectOrganicUnitAdministratorIds(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            unitId,
                            request.getRemoteAddr()
                    )
                    .stream()
                    .map(userService::findById)
                    .flatMap(java.util.Optional::stream)
                    .map(user -> OrganicUnitAdministratorView.from(
                            user,
                            organicUnitService.canRevokeOrganicUnitAdministrator(
                                    actor.userId(),
                                    currentSessionId(request),
                                    primaryProfile(actor),
                                    unitId,
                                    user.id(),
                                    request.getRemoteAddr()
                            )
                    ))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<AdministratorOptionView> organicUnitAdministratorOptions(
            HttpServletRequest request,
            SessionUser actor,
            long unitId
    ) {
        return organicUnitService.listEligibleOrganicUnitAdministratorIds(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        unitId,
                        request.getRemoteAddr()
                )
                .stream()
                .map(userService::findById)
                .flatMap(java.util.Optional::stream)
                .map(user -> AdministratorOptionView.from(user, false))
                .toList();
    }

    private OrganicUnit requireOrganicUnitInOrganization(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser actor,
            long organizationId,
            long unitId
    ) throws IOException {
        OrganicUnit unit = organicUnitService.getOrganicUnit(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                unitId,
                request.getRemoteAddr()
        );
        if (unit.organizationId() != organizationId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return unit;
    }

    private static OrganicUnitView organicUnitView(List<OrganicUnit> units, OrganicUnit unit) {
        return hierarchyViews(units, null)
                .stream()
                .filter(view -> view.getId() == unit.id())
                .findFirst()
                .orElseGet(() -> OrganicUnitView.from(unit, null, 0));
    }

    private List<AdministratorOptionView> administratorOptions(HttpServletRequest request, Set<Long> selectedIds) {
        SessionUser actor = requireCurrentUser(request);
        try {
            return userService.listUsers(actor.userId(), currentSessionId(request), primaryProfile(actor), request.getRemoteAddr())
                    .stream()
                    .filter(OrganizationManagementServlet::isActiveAdministrator)
                    .map(user -> AdministratorOptionView.from(user, selectedIds.contains(user.id())))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<OrganicUnit> safeOrganicUnits(SessionUser actor, long organizationId, HttpServletRequest request) {
        try {
            return organicUnitService.listOrganicUnits(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private Map<Long, List<OrganizationCourseTreeView>> courseTreesByOrganicUnit(long organizationId) {
        Map<Long, List<OrganizationCourseTreeView>> coursesByUnit = new HashMap<>();
        try {
            for (Course course : courseDAO.findByOrganization(organizationId)) {
                if (course.organicUnitId() == null) {
                    continue;
                }
                coursesByUnit.computeIfAbsent(course.organicUnitId(), ignored -> new ArrayList<>())
                        .add(courseTree(course));
            }
            coursesByUnit.values().forEach(courses -> courses.sort(Comparator.comparingLong(OrganizationCourseTreeView::getId)));
            return coursesByUnit;
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private OrganizationCourseTreeView courseTree(Course course) throws SQLException {
        List<OrganizationSubjectTreeView> subjects = new ArrayList<>();
        for (CourseSubjectAssociation association : courseSubjectDAO.findByCourse(course.id())) {
            Subject subject = subjectDAO.findById(association.subjectId()).orElse(null);
            if (subject == null) {
                continue;
            }
            List<OrganizationClassGroupTreeView> classGroups = classGroupDAO
                    .findByCourseAndSubject(course.id(), subject.id())
                    .stream()
                    .map(OrganizationClassGroupTreeView::from)
                    .toList();
            subjects.add(OrganizationSubjectTreeView.from(association, subject, classGroups));
        }
        subjects.sort(Comparator.comparingLong(OrganizationSubjectTreeView::getSubjectId));
        return OrganizationCourseTreeView.from(course, subjects);
    }

    private static void collectPedagogicalTreeIds(
            OrganizationView organization,
            Set<Long> courseIds,
            Set<Long> subjectIds,
            Set<Long> classGroupIds
    ) {
        for (OrganicUnitView unit : organization.getOrganicUnits()) {
            for (OrganizationCourseTreeView course : unit.getCourses()) {
                courseIds.add(course.getId());
                for (OrganizationSubjectTreeView subject : course.getSubjects()) {
                    subjectIds.add(subject.getSubjectId());
                    for (OrganizationClassGroupTreeView classGroup : subject.getClassGroups()) {
                        classGroupIds.add(classGroup.getId());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void exposeOrganizationElementCounts(
            HttpServletRequest request,
            OrganizationView organization
    ) {
        Map<Long, Integer> activityCountByClassGroup =
                (Map<Long, Integer>) request.getAttribute("classGroupActivityCountByClassGroup");
        if (activityCountByClassGroup == null) {
            activityCountByClassGroup = Map.of();
        }

        Map<Long, Integer> classGroupElementCountById = new HashMap<>();
        Map<String, Integer> subjectElementCountByKey = new HashMap<>();
        Map<Long, Integer> courseElementCountById = new HashMap<>();
        Map<Long, Integer> unitElementCountById = new HashMap<>();

        for (OrganicUnitView unit : organization.getOrganicUnits()) {
            unitElementCountById.put(unit.getId(), unit.getCourses().size());
            for (OrganizationCourseTreeView course : unit.getCourses()) {
                courseElementCountById.put(course.getId(), course.getSubjects().size());
                for (OrganizationSubjectTreeView subject : course.getSubjects()) {
                    subjectElementCountByKey.put(subject.getTreeKey(), subject.getClassGroups().size());
                    for (OrganizationClassGroupTreeView classGroup : subject.getClassGroups()) {
                        classGroupElementCountById.put(
                                classGroup.getId(),
                                activityCountByClassGroup.getOrDefault(classGroup.getId(), 0)
                        );
                    }
                }
            }
        }

        request.setAttribute("organizationUnitElementCountById", unitElementCountById);
        request.setAttribute("organizationCourseElementCountById", courseElementCountById);
        request.setAttribute("organizationSubjectElementCountByKey", subjectElementCountByKey);
        request.setAttribute("organizationClassGroupElementCountById", classGroupElementCountById);
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

    private Map<Long, Boolean> canModifySubjectById(
            SessionUser actor,
            Set<Long> subjectIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long subjectId : subjectIds) {
            permissions.put(subjectId, subjectService.canModifySubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageSubjectAssociationsById(
            SessionUser actor,
            Set<Long> subjectIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long subjectId : subjectIds) {
            permissions.put(subjectId, canManageSubjectAssociations(actor, subjectId, request));
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
        try {
            return courseSubjectDAO.findBySubject(subjectId).stream()
                    .anyMatch(association -> courseSubjectService.canManageAssociation(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            association.courseId(),
                            subjectId,
                            request.getRemoteAddr()
                    ));
        } catch (SQLException exception) {
            return false;
        }
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

    private boolean canCreateAnyOrganicUnit(
            SessionUser actor,
            long organizationId,
            List<OrganicUnit> units,
            HttpServletRequest request
    ) {
        if (organicUnitService.canCreateOrganicUnit(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                null,
                request.getRemoteAddr()
        )) {
            return true;
        }
        for (OrganicUnit unit : units) {
            if (organicUnitService.canCreateOrganicUnit(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organizationId,
                    unit.id(),
                    request.getRemoteAddr()
            )) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, Boolean> canModifyOrganicUnitById(
            SessionUser actor,
            List<OrganicUnit> units,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (OrganicUnit unit : units) {
            permissions.put(unit.id(), organicUnitService.canModifyOrganicUnit(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    unit.id(),
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private static boolean canManageOrganizationRoots(SessionUser actor) {
        return actor.hasPermission(AuthorizationPolicy.MANAGE_ALL);
    }

    private static List<OrganicUnitView> hierarchyViews(List<OrganicUnit> units, Long excludedUnitId) {
        return hierarchyViews(units, excludedUnitId, Map.of());
    }

    private static List<OrganicUnitView> hierarchyViews(
            List<OrganicUnit> units,
            Long excludedUnitId,
            Map<Long, List<OrganizationCourseTreeView>> coursesByUnit
    ) {
        Map<Long, OrganicUnit> byId = new HashMap<>();
        Map<Long, List<OrganicUnit>> childrenByParent = new HashMap<>();
        for (OrganicUnit unit : units) {
            if (excludedUnitId != null && unit.id() == excludedUnitId) {
                continue;
            }
            byId.put(unit.id(), unit);
            Long parentId = unit.parentOrganicUnitId();
            childrenByParent.computeIfAbsent(parentId == null ? 0L : parentId, ignored -> new ArrayList<>()).add(unit);
        }
        childrenByParent.values().forEach(children -> children.sort(unitComparator()));

        List<OrganicUnitView> views = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        appendChildren(0L, 0, childrenByParent, byId, visited, views, coursesByUnit);
        for (OrganicUnit unit : units.stream().sorted(unitComparator()).toList()) {
            if (!visited.contains(unit.id()) && (excludedUnitId == null || unit.id() != excludedUnitId)) {
                appendUnit(unit, 0, childrenByParent, byId, visited, views, coursesByUnit);
            }
        }
        return views;
    }

    private static void appendChildren(
            long parentId,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, OrganicUnit> byId,
            Set<Long> visited,
            List<OrganicUnitView> views,
            Map<Long, List<OrganizationCourseTreeView>> coursesByUnit
    ) {
        for (OrganicUnit child : childrenByParent.getOrDefault(parentId, List.of())) {
            appendUnit(child, depth, childrenByParent, byId, visited, views, coursesByUnit);
        }
    }

    private static void appendUnit(
            OrganicUnit unit,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, OrganicUnit> byId,
            Set<Long> visited,
            List<OrganicUnitView> views,
            Map<Long, List<OrganizationCourseTreeView>> coursesByUnit
    ) {
        if (!visited.add(unit.id())) {
            return;
        }
        String parentLabel = null;
        if (unit.parentOrganicUnitId() != null) {
            OrganicUnit parent = byId.get(unit.parentOrganicUnitId());
            parentLabel = parent == null ? "Unknown parent" : parent.code() + " - " + parent.name();
        }
        views.add(OrganicUnitView.from(unit, parentLabel, depth, coursesByUnit.getOrDefault(unit.id(), List.of())));
        appendChildren(unit.id(), depth + 1, childrenByParent, byId, visited, views, coursesByUnit);
    }

    private static Comparator<OrganicUnit> unitComparator() {
        return Comparator.comparingLong(OrganicUnit::id);
    }

    private static void prepareOrganizationContext(HttpServletRequest request, OrganizationView organization, String activeChild) {
        request.setAttribute("adminOrganizationContextId", organization.getId());
        request.setAttribute("adminOrganizationContextName", organization.getName());
        request.setAttribute("adminOrganizationActiveChild", activeChild);
    }

    private static boolean isActiveAdministrator(User user) {
        return user.state() == UserState.ACTIVE
                && user.accessProfiles().stream()
                .map(AccessProfile::type)
                .anyMatch(AccessProfileType.ADMINISTRATOR::equals);
    }

    private static OrganizationType organizationType(String value) {
        return value == null || value.isBlank() ? OrganizationType.OTHER : OrganizationType.valueOf(value);
    }

    private static OrganizationState organizationState(String value) {
        return value == null || value.isBlank() ? OrganizationState.ACTIVE : OrganizationState.valueOf(value);
    }

    private static OrganicUnitType organicUnitType(String value) {
        return value == null || value.isBlank() ? OrganicUnitType.OTHER : OrganicUnitType.valueOf(value);
    }

    private static OrganicUnitState organicUnitState(String value) {
        return value == null || value.isBlank() ? OrganicUnitState.ACTIVE : OrganicUnitState.valueOf(value);
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDate(value);
    }

    private static Part organizationImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("organizationImage");
    }
}
