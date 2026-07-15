package pt.isel.gape.web.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomCreateCommand;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.PhysicalRoomUpdateCommand;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.PhysicalRoomService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.OrganicUnitOptionView;
import pt.isel.gape.web.view.OrganizationOptionView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomFormData;
import pt.isel.gape.web.view.PhysicalRoomView;
import pt.isel.gape.web.view.SelectOptionView;

@WebServlet(name = "physicalRoomManagementServlet", urlPatterns = {"/learning/rooms", "/learning/rooms/*"})
public final class PhysicalRoomManagementServlet extends DashboardServletSupport {

    private static final String ROOM_LIST_JSP = "/WEB-INF/views/learning/room-list.jsp";
    private static final String ROOM_DETAIL_JSP = "/WEB-INF/views/learning/room-detail.jsp";
    private static final String ROOM_FORM_JSP = "/WEB-INF/views/learning/room-form.jsp";

    private final PhysicalRoomService roomService;
    private final LessonService lessonService;
    private final ApplicationReadService.PhysicalRooms physicalRoomDAO;
    private final ApplicationReadService.Lessons lessonDAO;
    private final ApplicationReadService.Organizations organizationDAO;
    private final ApplicationReadService.OrganicUnits organicUnitDAO;
    private final LearningViewFactory viewFactory;

    public PhysicalRoomManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private PhysicalRoomManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new PhysicalRoomService(connectionProvider, clock),
                new LessonService(connectionProvider, clock)
        );
    }

    PhysicalRoomManagementServlet(
            ApplicationReadService readService,
            PhysicalRoomService roomService,
            LessonService lessonService
    ) {
        this.roomService = roomService;
        this.lessonService = lessonService;
        this.physicalRoomDAO = readService.physicalRooms();
        this.lessonDAO = readService.lessons();
        this.organizationDAO = readService.organizations();
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length == 0) {
            showList(request, response);
            return;
        }
        if (segments.length == 1 && "new".equals(segments[0])) {
            showCreateForm(request, response, null);
            return;
        }
        if (segments.length == 1) {
            showDetail(request, response, decode(segments[0]));
            return;
        }
        if (segments.length == 2 && "edit".equals(segments[1])) {
            showEditForm(request, response, decode(segments[0]), null);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length == 0) {
            createRoom(request, response);
            return;
        }
        if (segments.length == 1) {
            updateRoom(request, response, decode(segments[0]));
            return;
        }
        if (segments.length == 2) {
            String code = decode(segments[0]);
            switch (segments[1]) {
                case "archive" -> archiveRoom(request, response, code);
                case "delete" -> deleteRoom(request, response, code);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RoomScope selectedScope = roomScope(request);
        List<Organization> organizations = readableOrganizations(request);
        List<Organization> manageableOrganizations = manageableOrganizations(request);
        List<PhysicalRoomView> rooms = new ArrayList<>();
        for (Organization organization : organizations) {
            if (!selectedScope.matchesOrganization(organization.id())) {
                continue;
            }
            rooms.addAll(listRooms(request, organization.id()).stream()
                    .map(viewFactory::physicalRoomView)
                    .filter(selectedScope::matchesRoom)
                    .toList());
        }
        rooms = rooms.stream()
                .sorted(Comparator.comparing(PhysicalRoomView::getCode))
                .toList();

        request.setAttribute("rooms", rooms);
        request.setAttribute("organizationOptions", organizations.stream().map(OrganizationOptionView::from).toList());
        request.setAttribute("roomScopeOptions", roomScopeOptions(organizations, selectedScope.value()));
        request.setAttribute("canManageRoomByCode", canManageRoomMap(request, rooms));
        request.setAttribute("canManageRooms", !manageableOrganizations.isEmpty());
        request.setAttribute("selectedRoomScope", selectedScope.value());
        request.setAttribute("selectedOrganizationId", selectedScope.organizationId());
        request.setAttribute("roomCount", rooms.size());
        request.setAttribute("activeRoomCount", rooms.stream().filter(PhysicalRoomView::isActive).count());
        request.setAttribute("inactiveRoomCount", rooms.stream().filter(PhysicalRoomView::isInactive).count());
        prepareDashboard(
                request,
                "rooms",
                "Rooms",
                manageableOrganizations.isEmpty() ? null : "/learning/rooms/new",
                manageableOrganizations.isEmpty() ? null : "New Room"
        );
        forward(request, response, ROOM_LIST_JSP);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, String code)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        PhysicalRoom room = roomService.getPhysicalRoom(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                code,
                request.getRemoteAddr()
        );
        PhysicalRoomView roomView = viewFactory.physicalRoomView(room);
        boolean canManageRoom = canManageRoom(request, room);
        request.setAttribute("room", roomView);
        request.setAttribute("roomLessons", roomLessons(request, room.code()));
        request.setAttribute("canManageRoom", canManageRoom);
        request.setAttribute("roomBackHref", backHref(request, "/learning/rooms"));
        request.setAttribute("roomEditHref", request.getContextPath()
                + appendReturnTo("/learning/rooms/" + encodePath(room.code()) + "/edit", currentRequestPath(request)));
        prepareRoomContext(request, roomView, "detail");
        prepareDashboard(request, "rooms", "Room Detail");
        forward(request, response, ROOM_DETAIL_JSP);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        if (manageableOrganizations(request).isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        showRoomForm(
                request,
                response,
                PhysicalRoomFormData.blank(optionalLong(request, "organizationId")),
                true,
                error
        );
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, String code, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        PhysicalRoom room = roomService.getPhysicalRoom(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                code,
                request.getRemoteAddr()
        );
        if (!canManageRoom(request, room)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        showRoomForm(request, response, PhysicalRoomFormData.from(room), false, error);
    }

    private void showRoomForm(
            HttpServletRequest request,
            HttpServletResponse response,
            PhysicalRoomFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        List<Organization> organizations = manageableOrganizations(request);
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("formAction", creating
                ? request.getContextPath() + "/learning/rooms"
                : request.getContextPath() + "/learning/rooms/" + encodePath(form.getCode()));
        request.setAttribute("formReturnTo", safeReturnPath(request));
        request.setAttribute("roomBackHref", backHref(request, creating
                ? "/learning/rooms"
                : "/learning/rooms/" + encodePath(form.getCode())));
        request.setAttribute("organizationOptions", organizations.stream().map(OrganizationOptionView::from).toList());
        request.setAttribute("organicUnitOptions", organicUnitOptions(organizations));
        request.setAttribute("roomStates", roomStateOptions(form));
        if (!creating && form.getCode() != null && !form.getCode().isBlank()) {
            request.setAttribute("canManageRoom", true);
            request.setAttribute("learningRoomContextCode", encodePath(form.getCode()));
            request.setAttribute("learningRoomActiveChild", "edit");
        } else if (creating) {
            request.setAttribute("learningRoomActiveChild", "new");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "rooms", creating ? "Create Physical Room" : "Edit Physical Room");
        forward(request, response, ROOM_FORM_JSP);
    }

    private void createRoom(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        if (manageableOrganizations(request).isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        PhysicalRoomFormData form = PhysicalRoomFormData.from(request, true, null);
        try {
            PhysicalRoom room = roomService.createPhysicalRoom(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    createCommand(form),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Physical room created.");
            redirectPreservingReturnTo(request, response, "/learning/rooms/" + encodePath(room.code()));
        } catch (RuntimeException exception) {
            showRoomForm(request, response, form, true, messageFor(exception));
        }
    }

    private void updateRoom(HttpServletRequest request, HttpServletResponse response, String code)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        PhysicalRoomFormData form = PhysicalRoomFormData.from(request, false, code);
        if (!canManageRoomCode(request, code)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            roomService.updatePhysicalRoom(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    code,
                    updateCommand(form),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Physical room updated.");
            redirectPreservingReturnTo(request, response, "/learning/rooms/" + encodePath(code));
        } catch (RuntimeException exception) {
            showRoomForm(request, response, form, false, messageFor(exception));
        }
    }

    private void archiveRoom(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (!canManageRoomCode(request, code)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            roomService.archivePhysicalRoom(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    code,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Physical room deactivated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/rooms/" + encodePath(code));
    }

    private void deleteRoom(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (!canManageRoomCode(request, code)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            roomService.deletePhysicalRoom(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    code,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Physical room deleted.");
            redirect(request, response, "/learning/rooms");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/learning/rooms/" + encodePath(code));
        }
    }

    private PhysicalRoomCreateCommand createCommand(PhysicalRoomFormData form) {
        return new PhysicalRoomCreateCommand(
                form.getCode(),
                requiredLong(form.getOrganizationId(), "organizationId"),
                form.getOrganicUnitId(),
                form.getName(),
                nullIfBlank(form.getDescription()),
                form.capacityValue(),
                nullIfBlank(form.getLocation()),
                form.roomState()
        );
    }

    private PhysicalRoomUpdateCommand updateCommand(PhysicalRoomFormData form) {
        return new PhysicalRoomUpdateCommand(
                requiredLong(form.getOrganizationId(), "organizationId"),
                form.getOrganicUnitId(),
                form.getName(),
                nullIfBlank(form.getDescription()),
                form.capacityValue(),
                nullIfBlank(form.getLocation()),
                form.roomState()
        );
    }

    private List<Organization> manageableOrganizations(HttpServletRequest request) {
        try {
            return organizationDAO.findActive().stream()
                    .filter(organization -> canManageOrganizationRooms(request, organization.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load organizations", exception);
        }
    }

    private List<Organization> readableOrganizations(HttpServletRequest request) {
        try {
            return organizationDAO.findActive().stream()
                    .filter(organization -> canReadOrganizationRooms(request, organization.id()))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load organizations", exception);
        }
    }

    private boolean canManageOrganizationRooms(HttpServletRequest request, long organizationId) {
        SessionUser actor = requireCurrentUser(request);
        return roomService.canManagePhysicalRoomsByOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
    }

    private boolean canReadOrganizationRooms(HttpServletRequest request, long organizationId) {
        try {
            listRooms(request, organizationId);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean canManageRoomCode(HttpServletRequest request, String code) {
        SessionUser actor = requireCurrentUser(request);
        return roomService.canManagePhysicalRoom(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                code,
                request.getRemoteAddr()
        );
    }

    private boolean canManageRoom(HttpServletRequest request, PhysicalRoom room) {
        return canManageRoomCode(request, room.code());
    }

    private java.util.Map<String, Boolean> canManageRoomMap(
            HttpServletRequest request,
            List<PhysicalRoomView> rooms
    ) {
        java.util.Map<String, Boolean> result = new java.util.HashMap<>();
        for (PhysicalRoomView room : rooms) {
            result.put(room.getCode(), canManageRoomCode(request, room.getCode()));
        }
        return result;
    }

    private List<PhysicalRoom> listRooms(HttpServletRequest request, long organizationId) {
        SessionUser actor = requireCurrentUser(request);
        return roomService.listPhysicalRoomsByOrganization(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
    }

    private List<OrganicUnitOptionView> organicUnitOptions(List<Organization> organizations) {
        List<Long> organizationIds = organizations.stream().map(Organization::id).toList();
        List<OrganicUnit> units = new ArrayList<>();
        for (Long organizationId : organizationIds) {
            try {
                units.addAll(organicUnitDAO.findByOrganization(organizationId));
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load organic units", exception);
            }
        }
        return units.stream()
                .map(OrganicUnitOptionView::from)
                .toList();
    }

    private List<SelectOptionView> roomScopeOptions(List<Organization> organizations, String selectedScope) {
        List<SelectOptionView> options = new ArrayList<>();
        options.add(new SelectOptionView(
                "",
                "All organizations",
                "All organizations and organic units",
                selectedScope == null || selectedScope.isBlank()
        ));
        for (Organization organization : organizations) {
            String organizationValue = "org:" + organization.id();
            OrganizationOptionView organizationOption = OrganizationOptionView.from(organization);
            options.add(new SelectOptionView(
                    organizationValue,
                    organizationOption.getLabel(),
                    organization.name(),
                    organizationValue.equals(selectedScope)
            ));
            for (OrganicUnitOptionView unit : organicUnitOptions(List.of(organization))) {
                String unitValue = "unit:" + unit.getId();
                options.add(new SelectOptionView(
                        unitValue,
                        "-- " + unit.getAcronym() + " | " + organizationOption.getAcronym(),
                        unit.getName() + " | " + organization.name(),
                        unitValue.equals(selectedScope)
                ));
            }
        }
        return options;
    }

    private List<LessonView> roomLessons(HttpServletRequest request, String code) {
        SessionUser actor = requireCurrentUser(request);
        try {
            lessonService.synchronizeTemporalStates();
            return physicalRoomDAO.findByCode(code).isEmpty()
                    ? List.of()
                    : lessonDAO.findByRoom(code)
                            .stream()
                            .filter(lesson -> canReadLesson(
                                    request,
                                    actor,
                                    lesson.id()
                            ))
                            .map(viewFactory::lessonView)
                            .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load room lessons", exception);
        }
    }

    private boolean canReadLesson(HttpServletRequest request, SessionUser actor, long lessonId) {
        try {
            lessonService.getLesson(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    lessonId,
                    request.getRemoteAddr()
            );
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static List<SelectOptionView> roomStateOptions(PhysicalRoomFormData form) {
        return Arrays.stream(PhysicalRoomState.values())
                .map(state -> new SelectOptionView(state.name(), state.toDatabaseValue(), form.isStateSelected(state.name())))
                .toList();
    }

    private RoomScope roomScope(HttpServletRequest request) {
        String scope = text(request, "scope");
        if (scope == null || scope.isBlank()) {
            Long legacyOrganizationId = optionalLong(request, "organizationId");
            return legacyOrganizationId == null
                    ? RoomScope.all()
                    : RoomScope.organization(legacyOrganizationId);
        }
        if (scope.startsWith("org:")) {
            Long organizationId = parseLong(scope.substring("org:".length()));
            return organizationId == null ? RoomScope.all() : RoomScope.organization(organizationId);
        }
        if (scope.startsWith("unit:")) {
            Long organicUnitId = parseLong(scope.substring("unit:".length()));
            if (organicUnitId == null) {
                return RoomScope.all();
            }
            try {
                return organicUnitDAO.findById(organicUnitId)
                        .map(unit -> RoomScope.organicUnit(unit.organizationId(), unit.id()))
                        .orElse(RoomScope.all());
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load organic unit filter", exception);
            }
        }
        return RoomScope.all();
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encodePath(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void prepareRoomContext(HttpServletRequest request, PhysicalRoomView room, String activeChild) {
        request.setAttribute("learningRoomContextCode", room.getEncodedCode());
        request.setAttribute("learningRoomActiveChild", activeChild);
    }

    private record RoomScope(String value, Long organizationId, Long organicUnitId) {
        static RoomScope all() {
            return new RoomScope("", null, null);
        }

        static RoomScope organization(long organizationId) {
            return new RoomScope("org:" + organizationId, organizationId, null);
        }

        static RoomScope organicUnit(long organizationId, long organicUnitId) {
            return new RoomScope("unit:" + organicUnitId, organizationId, organicUnitId);
        }

        boolean matchesOrganization(long candidateOrganizationId) {
            return organizationId == null || organizationId == candidateOrganizationId;
        }

        boolean matchesRoom(PhysicalRoomView room) {
            return organicUnitId == null || organicUnitId.equals(room.getOrganicUnitId());
        }
    }
}
