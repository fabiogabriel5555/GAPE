package pt.isel.gape.web.controller;

import java.io.IOException;
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
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.AdministratorOptionView;
import pt.isel.gape.web.view.OrganicUnitFormData;
import pt.isel.gape.web.view.OrganicUnitView;
import pt.isel.gape.web.view.OrganizationAdministratorView;
import pt.isel.gape.web.view.OrganizationFormData;
import pt.isel.gape.web.view.OrganizationView;

@WebServlet(name = "organizationManagementServlet", urlPatterns = {"/admin/organizations", "/admin/organizations/*"})
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public final class OrganizationManagementServlet extends DashboardServletSupport {

    private static final String ORGANIZATIONS_LIST_JSP = "/admin/admin/organization/admin-organizations.jsp";
    private static final String ORGANIZATION_DETAIL_JSP = "/admin/admin/organization/admin-organization-detail.jsp";
    private static final String ORGANIZATION_UNITS_JSP = "/admin/admin/organization/admin-organization-units.jsp";
    private static final String ORGANIZATION_FORM_JSP = "/admin/admin/organization/admin-organization-form.jsp";
    private static final String ORGANIC_UNIT_FORM_JSP = "/admin/admin/organization/admin-organic-unit-form.jsp";

    private final OrganizationService organizationService;
    private final OrganicUnitService organicUnitService;
    private final UserService userService;
    private final ProfilePhotoStorage photoStorage;

    public OrganizationManagementServlet() {
        this(
                new OrganizationService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new OrganicUnitService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new UserService(ConnectionProvider.defaultProvider()),
                new ProfilePhotoStorage()
        );
    }

    OrganizationManagementServlet(
            OrganizationService organizationService,
            OrganicUnitService organicUnitService,
            UserService userService,
            ProfilePhotoStorage photoStorage
    ) {
        this.organizationService = organizationService;
        this.organicUnitService = organicUnitService;
        this.userService = userService;
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
                showUnits(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 3 && "units".equals(segments[1]) && "new".equals(segments[2])) {
                showUnitCreateForm(request, response, Long.parseLong(segments[0]),
                        OrganicUnitFormData.blank(Long.parseLong(segments[0])), null);
                return;
            }
            if (segments.length == 4 && "units".equals(segments[1]) && "edit".equals(segments[3])) {
                showUnitEditForm(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]), null);
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
        List<OrganizationView> views = organizations.stream()
                .map(organization -> OrganizationView.from(
                        organization,
                        safeOrganicUnits(actor, organization.id(), request).size()
                ))
                .toList();
        request.setAttribute("organizations", views);
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
        List<OrganicUnit> units = organicUnitService.listOrganicUnits(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        OrganizationView organizationView = OrganizationView.from(organization, units.size());
        boolean canModifyOrganization = canManageOrganizationRoots(actor);
        boolean canCreateOrganicUnits = canCreateAnyOrganicUnit(actor, organizationId, units, request)
                && !organizationView.isArchived();
        request.setAttribute("organization", organizationView);
        request.setAttribute("organicUnits", hierarchyViews(units, null));
        request.setAttribute("canModifyOrganization", canModifyOrganization);
        request.setAttribute("canAssignOrganizationAdministrators", canModifyOrganization);
        request.setAttribute("canCreateOrganicUnits", canCreateOrganicUnits);
        request.setAttribute("canModifyOrganicUnitById", canModifyOrganicUnitById(actor, units, request));
        request.setAttribute("assignedAdministrators", organizationService.listAdministrators(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        ).stream().map(OrganizationAdministratorView::from).toList());
        request.setAttribute("administratorOptions", administratorOptions(request, Set.of()));
        prepareOrganizationContext(request, organizationView, "detail");
        prepareDashboard(request, "organizations", "Organization Detail");
        forward(request, response, ORGANIZATION_DETAIL_JSP);
    }

    private void showUnits(HttpServletRequest request, HttpServletResponse response, long organizationId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Organization organization = organizationService.getOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        List<OrganicUnit> units = organicUnitService.listOrganicUnits(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
        OrganizationView organizationView = OrganizationView.from(organization, units.size());
        boolean canCreateOrganicUnits = canCreateAnyOrganicUnit(actor, organizationId, units, request)
                && !organizationView.isArchived();
        request.setAttribute("organization", organizationView);
        request.setAttribute("organicUnits", hierarchyViews(units, null));
        request.setAttribute("canModifyOrganization", canManageOrganizationRoots(actor));
        request.setAttribute("canCreateOrganicUnits", canCreateOrganicUnits);
        request.setAttribute("canModifyOrganicUnitById", canModifyOrganicUnitById(actor, units, request));
        prepareOrganizationContext(request, organizationView, "units");
        prepareDashboard(request, "organizations", "Organic Units");
        forward(request, response, ORGANIZATION_UNITS_JSP);
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
        OrganicUnit unit = organicUnitService.getOrganicUnit(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                unitId,
                request.getRemoteAddr()
        );
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
            flashSuccess(request, "Organization archived.");
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
            redirect(request, response, "/admin/organizations/" + organizationId + "/units");
        } catch (RuntimeException exception) {
            showUnitCreateForm(request, response, organizationId, form, messageFor(exception));
        }
    }

    private void updateOrganicUnit(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
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
            redirect(request, response, "/admin/organizations/" + organizationId + "/units");
        } catch (RuntimeException exception) {
            showUnitEditForm(request, response, organizationId, unitId, messageFor(exception));
        }
    }

    private void deleteOrganicUnit(HttpServletRequest request, HttpServletResponse response, long organizationId, long unitId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
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
        redirect(request, response, "/admin/organizations/" + organizationId + "/units");
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
        request.setAttribute("parentOptions", hierarchyViews(units, editingUnitId));
        prepareOrganizationContext(request, organizationView, creating ? "unit-new" : "unit-edit");
        request.setAttribute("canModifyOrganization", canManageOrganizationRoots(actor));
        request.setAttribute("canCreateOrganicUnits", canCreateAnyOrganicUnit(actor, organization.id(), units, request)
                && !organizationView.isArchived());
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "organizations", creating ? "Create Organic Unit" : "Edit Organic Unit");
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
            if (!unit.state().equals(OrganicUnitState.ARCHIVED)
                    && organicUnitService.canCreateOrganicUnit(
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
        appendChildren(0L, 0, childrenByParent, byId, visited, views);
        for (OrganicUnit unit : units.stream().sorted(unitComparator()).toList()) {
            if (!visited.contains(unit.id()) && (excludedUnitId == null || unit.id() != excludedUnitId)) {
                appendUnit(unit, 0, childrenByParent, byId, visited, views);
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
            List<OrganicUnitView> views
    ) {
        for (OrganicUnit child : childrenByParent.getOrDefault(parentId, List.of())) {
            appendUnit(child, depth, childrenByParent, byId, visited, views);
        }
    }

    private static void appendUnit(
            OrganicUnit unit,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, OrganicUnit> byId,
            Set<Long> visited,
            List<OrganicUnitView> views
    ) {
        if (!visited.add(unit.id())) {
            return;
        }
        String parentLabel = null;
        if (unit.parentOrganicUnitId() != null) {
            OrganicUnit parent = byId.get(unit.parentOrganicUnitId());
            parentLabel = parent == null ? "Unknown parent" : parent.code() + " - " + parent.name();
        }
        views.add(OrganicUnitView.from(unit, parentLabel, depth));
        appendChildren(unit.id(), depth + 1, childrenByParent, byId, visited, views);
    }

    private static Comparator<OrganicUnit> unitComparator() {
        return Comparator.comparing(OrganicUnit::code).thenComparing(OrganicUnit::name);
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
        return value == null ? null : LocalDate.parse(value);
    }

    private static Part organizationImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("organizationImage");
    }
}
