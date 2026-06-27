package pt.isel.gape.web.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;

abstract class DashboardServletSupport extends HttpServlet {

    private static final String FLASH_SUCCESS = "gape.flash.success";
    private static final String FLASH_ERROR = "gape.flash.error";

    protected final SessionManager sessionManager;

    DashboardServletSupport() {
        this(new SessionManager());
    }

    DashboardServletSupport(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    protected SessionUser requireCurrentUser(HttpServletRequest request) {
        return sessionManager.getSessionUser(request)
                .orElseThrow(() -> new SecurityException("Authenticated user is required"));
    }

    protected Long currentSessionId(HttpServletRequest request) {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        return sessionId.isPresent() ? sessionId.getAsLong() : null;
    }

    protected AccessProfileType primaryProfile(SessionUser sessionUser) {
        return sessionUser.primaryProfileType()
                .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
    }

    protected void refreshCurrentUser(HttpServletRequest request, SessionUser currentUser, User updatedUser) {
        Map<AccessProfileType, Set<String>> permissionsByProfile = new EnumMap<>(AccessProfileType.class);
        for (AccessProfileType profileType : currentUser.profileTypes()) {
            permissionsByProfile.put(profileType, currentUser.permissionCodesFor(profileType));
        }
        SessionUser refreshed = new SessionUser(
                updatedUser.id(),
                updatedUser.name(),
                updatedUser.email(),
                updatedUser.photo(),
                currentUser.profileTypes(),
                permissionsByProfile
        );
        sessionManager.refreshAuthenticatedSession(request, refreshed);
    }

    protected void prepareDashboard(
            HttpServletRequest request,
            String activeMenu,
            String pageTitle,
            String topActionHref,
            String topActionLabel
    ) {
        request.setAttribute("activeMenu", activeMenu);
        request.setAttribute("pageTitle", pageTitle);
        request.setAttribute("topActionHref", topActionHref);
        request.setAttribute("topActionLabel", topActionLabel);
        request.setAttribute("mediaCacheVersion", Long.toString(System.currentTimeMillis()));
        prepareSmartNavigation(request);
        consumeFlash(request);
    }

    protected void prepareDashboard(HttpServletRequest request, String activeMenu, String pageTitle) {
        prepareDashboard(request, activeMenu, pageTitle, null, null);
    }

    protected void flashSuccess(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_SUCCESS, message);
    }

    protected void flashError(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_ERROR, message);
    }

    protected void forward(HttpServletRequest request, HttpServletResponse response, String jsp)
            throws ServletException, IOException {
        request.getRequestDispatcher(jsp).forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + redirectPath(request, path));
    }

    protected void redirectPreservingReturnTo(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        redirect(request, response, appendReturnTo(path, safeReturnPath(request)));
    }

    protected void redirectToReturnPath(HttpServletRequest request, HttpServletResponse response, String defaultPath)
            throws IOException {
        String returnTo = text(request, "returnTo");
        if (isSafeReturnPath(returnTo)) {
            redirect(request, response, returnTo);
            return;
        }
        redirect(request, response, defaultPath);
    }

    protected boolean hasSafeReturnPath(HttpServletRequest request) {
        return isSafeReturnPath(text(request, "returnTo"));
    }

    protected String safeReturnPath(HttpServletRequest request) {
        String returnTo = text(request, "returnTo");
        return isSafeReturnPath(returnTo) ? returnTo : null;
    }

    protected String backHref(HttpServletRequest request, String defaultPath) {
        String returnTo = safeReturnPath(request);
        return request.getContextPath() + (returnTo == null ? defaultPath : returnTo);
    }

    protected String currentRequestPath(HttpServletRequest request) {
        StringBuilder path = new StringBuilder(request.getServletPath());
        if (request.getPathInfo() != null) {
            path.append(request.getPathInfo());
        }
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return path.toString();
        }
        String filteredQuery = java.util.Arrays.stream(queryString.split("&"))
                .filter(parameter -> !parameter.startsWith("returnTo="))
                .filter(parameter -> !parameter.isBlank())
                .collect(java.util.stream.Collectors.joining("&"));
        if (!filteredQuery.isBlank()) {
            path.append('?').append(filteredQuery);
        }
        return path.toString();
    }

    protected String appendReturnTo(String path, String returnTo) {
        if (!isSafeReturnPath(returnTo) || path == null || path.isBlank() || path.contains("returnTo=")) {
            return path;
        }
        return path
                + (path.contains("?") ? "&" : "?")
                + "returnTo="
                + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
    }

    private String redirectPath(HttpServletRequest request, String path) {
        String returnTo = safeReturnPath(request);
        if (returnTo == null || path == null || path.isBlank() || path.equals(returnTo)) {
            return path;
        }
        return appendReturnTo(path, returnTo);
    }

    protected static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static boolean isSafeReturnPath(String path) {
        if (path == null
                || !path.startsWith("/")
                || path.startsWith("//")
                || path.contains("\\")
                || path.contains(":")) {
            return false;
        }
        for (int index = 0; index < path.length(); index++) {
            char current = path.charAt(index);
            if (current <= 0x1F
                    || current == 0x7F
                    || Character.isWhitespace(current)
                    || current == '"'
                    || current == '\''
                    || current == '<'
                    || current == '>') {
                return false;
            }
        }
        return true;
    }

    private void prepareSmartNavigation(HttpServletRequest request) {
        String currentPath = currentRequestPath(request);
        String returnTo = safeReturnPath(request);
        request.setAttribute("currentReturnTo", currentPath);
        request.setAttribute("currentReturnToParam", URLEncoder.encode(currentPath, StandardCharsets.UTF_8));
        request.setAttribute("returnTo", returnTo);
        request.setAttribute("returnToParam", returnTo == null ? "" : URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
    }

    protected static long longParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Long.parseLong(value);
    }

    protected static Long pathLong(String pathInfo, int segmentIndex) {
        String[] segments = pathSegments(pathInfo);
        if (segments.length <= segmentIndex) {
            return null;
        }
        return Long.parseLong(segments[segmentIndex]);
    }

    protected static String[] pathSegments(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return new String[0];
        }
        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return normalized.isBlank() ? new String[0] : normalized.split("/");
    }

    protected static String messageFor(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The operation could not be completed.";
        }
        if (message.contains("Duplicate email")) {
            return "There is already a user with this email.";
        }
        if (message.contains("Duplicate document")) {
            return "There is already a user with this document.";
        }
        if (message.contains("Document type and number")) {
            return "Document type and document number must be filled together.";
        }
        if (message.contains("Unsupported document type")) {
            return "The selected document type is not supported.";
        }
        if (message.contains("Invalid document number")) {
            return "The document number does not match the selected document type.";
        }
        if (message.contains("Unsupported language")) {
            return "The selected language is not supported.";
        }
        if (message.contains("permission is required")) {
            return "You do not have permission to perform this operation.";
        }
        if (message.contains("not a supported image")) {
            return "The uploaded file is not a supported image.";
        }
        if (message.contains("WebP native") || message.contains("native WebP")) {
            return "The server cannot convert the uploaded image to WebP.";
        }
        if (message.contains("image writer")) {
            return "The server cannot process the uploaded image.";
        }
        if (message.contains("processedAt cannot be before submittedAt")) {
            return "The processing date cannot be before the submission date.";
        }
        if (message.contains("Final deletion request state requires processedAt")) {
            return "A final state requires a processing date.";
        }
        if (message.contains("Course organization is required")) {
            return "Course organization is required.";
        }
        if (message.contains("Course name is required")) {
            return "Course name is required.";
        }
        if (message.contains("Course organic unit must belong to the same organization")) {
            return "The selected organic unit must belong to the same organization as the course.";
        }
        if (message.contains("Subject organization is required")) {
            return "Subject organization is required.";
        }
        if (message.contains("Subject name is required")) {
            return "Subject name is required.";
        }
        if (message.contains("Course-subject association already exists")) {
            return "This subject is already associated with the selected course.";
        }
        if (message.contains("Curricular year and term must be provided together")) {
            return "Curricular year and period must be filled together.";
        }
        if (message.contains("Course and subject must belong to the same organization")) {
            return "Course and subject must belong to the same organization.";
        }
        if (message.contains("Active course enrollment overlaps")) {
            return "There is already an active enrollment for this course in the selected period.";
        }
        if (message.contains("Active subject enrollment overlaps")) {
            return "There is already an active enrollment for this subject in the selected period.";
        }
        if (message.contains("Student must be actively enrolled in the course")
                || message.contains("Subject enrollment requires active Course enrollment")) {
            return "You must be enrolled in the course before enrolling in one of its subjects.";
        }
        if (message.contains("Enrollment requires an active course")) {
            return "Enrollment requires an active course.";
        }
        if (message.contains("Enrollment requires an active subject")) {
            return "Enrollment requires an active subject.";
        }
        if (message.contains("Subject is not integrated in the course")) {
            return "This subject is not integrated in the selected course.";
        }
        if (message.contains("Class group code is required")) {
            return "Class group code is required.";
        }
        if (message.contains("Class group course is required")) {
            return "Class group course is required.";
        }
        if (message.contains("Class group subject is required")) {
            return "Class group subject is required.";
        }
        if (message.contains("Class group course and subject must belong to the same organization")) {
            return "Class group course and subject must belong to the same organization.";
        }
        if (message.contains("Class group min students cannot exceed max students")) {
            return "Minimum students cannot exceed maximum students.";
        }
        if (message.contains("Class group end date cannot be before start date")) {
            return "Class group end date cannot be before start date.";
        }
        if (message.contains("Class group maximum capacity exceeded")) {
            return "Class group maximum capacity exceeded.";
        }
        if (message.contains("Student must be actively enrolled in the subject")) {
            return "The student must be actively enrolled in this subject before joining the class group.";
        }
        if (message.contains("Class group enrollment already exists")) {
            return "This student already has a class group enrollment.";
        }
        if (message.contains("Active class group enrollment overlaps")) {
            return "There is already an active class group enrollment in the selected period.";
        }
        if (message.contains("Content block code is required")) {
            return "Content block code is required.";
        }
        if (message.contains("Content block name is required")) {
            return "Content block name is required.";
        }
        if (message.contains("Content block order must be positive")) {
            return "Content block order must be positive.";
        }
        if (message.contains("Active content block order already exists")) {
            return "Another active content block already uses this order.";
        }
        if (message.contains("Scheduled content blocks require an availability start date")) {
            return "Scheduled content blocks require an availability start date.";
        }
        if (message.contains("Content block availability end cannot be before start")) {
            return "Content block availability end cannot be before start.";
        }
        if (message.contains("Lesson title is required")) {
            return "Lesson title is required.";
        }
        if (message.contains("Lesson end date must be after start date")) {
            return "Lesson end date must be after the start date.";
        }
        if (message.contains("Lesson start date cannot be in the past")) {
            return "Lesson start date cannot be in the past.";
        }
        if (message.contains("Online lessons cannot have a physical room")) {
            return "Online lessons cannot have a physical room.";
        }
        if (message.contains("Onsite lessons require a physical room")) {
            return "Presential lessons require a physical room.";
        }
        if (message.contains("Hybrid lessons require a physical room")) {
            return "Hybrid lessons require a physical room.";
        }
        if (message.contains("Onsite lessons cannot have a video conference access URL")
                || message.contains("Onsite lessons cannot have a video conference provider")) {
            return "Presential lessons cannot have videoconference data.";
        }
        if (message.contains("Video conference access URL is required")) {
            return "Online and hybrid lessons require a meeting link.";
        }
        if (message.contains("Video conference provider is required")) {
            return "Online and hybrid lessons require Zoom, Teams or Meet as provider.";
        }
        if (message.contains("Unsupported video conference provider")) {
            return "Provider must be Zoom, Teams or Meet.";
        }
        if (message.contains("meeting links must use")) {
            return "The meeting link must match the selected provider.";
        }
        if (message.contains("Video conference access URL must use HTTPS")
                || message.contains("Video conference access URL is invalid")
                || message.contains("Video conference access URL must be absolute")) {
            return "The meeting link must be a valid HTTPS URL.";
        }
        if (message.contains("Video conference access URL cannot target")) {
            return "The meeting link cannot target localhost or private network hosts.";
        }
        if (message.contains("Physical room already has an overlapping lesson")) {
            return "The physical room already has a lesson in the selected period.";
        }
        if (message.contains("Lesson physical room must belong to the same organization")) {
            return "The physical room must belong to the same organization as the class group.";
        }
        if (message.contains("Lesson physical room must be active")) {
            return "The selected physical room must be active.";
        }
        if (message.contains("Lesson content block must belong to the same class group")) {
            return "The selected pedagogical block must belong to the same class group.";
        }
        if (message.contains("Physical room code is required")) {
            return "Physical room code is required.";
        }
        if (message.contains("Physical room name is required")) {
            return "Physical room name is required.";
        }
        if (message.contains("Physical room capacity must be greater than zero")) {
            return "Physical room capacity must be greater than zero.";
        }
        if (message.contains("Physical room capacity is below active class group enrollments")) {
            return "Physical room capacity is below the active class group enrollments.";
        }
        if (message.contains("Physical room organic unit must belong to the same organization")) {
            return "The selected organic unit must belong to the same organization as the room.";
        }
        if (message.contains("Physical room with lessons cannot be deleted")) {
            return "Physical rooms with lessons cannot be deleted.";
        }
        if (message.contains("Physical room with scheduled or active lessons cannot be deactivated")) {
            return "Physical rooms with scheduled or active lessons cannot be deactivated.";
        }
        if (message.contains("Schedule event title is required")) {
            return "Schedule event title is required.";
        }
        if (message.contains("schedule event start is required")) {
            return "Schedule event start is required.";
        }
        if (message.contains("schedule event end is required")) {
            return "Schedule event end is required.";
        }
        if (message.contains("Schedule event end cannot be before start")) {
            return "Schedule event end cannot be before the start date.";
        }
        if (message.contains("Enabled reminders require minutes before")) {
            return "Enabled reminders require minutes before.";
        }
        if (message.contains("Schedule event requires at least one class group")) {
            return "Schedule event requires at least one class group.";
        }
        if (message.contains("Schedule event period must match the referenced lesson")) {
            return "The event period must match the referenced lesson.";
        }
        if (message.contains("Student is not enrolled in the lesson class group")) {
            return "The student is not enrolled in the lesson class group.";
        }
        if (message.contains("An active attendance record already exists")) {
            return "There is already an active attendance record for this student and lesson.";
        }
        if (message.contains("Attendance check-out cannot be before check-in")) {
            return "Attendance check-out cannot be before check-in.";
        }
        if (message.contains("Attendance check-out requires check-in")) {
            return "Attendance check-out requires check-in.";
        }
        if (message.contains("Absent attendance cannot include permanence timestamps")) {
            return "Absent records cannot include permanence timestamps.";
        }
        if (message.contains("Attendance notes are too long")) {
            return "Attendance notes are too long.";
        }
        if (message.contains("Attendance record is not compatible with absence justification")) {
            return "This attendance record cannot be justified.";
        }
        if (message.contains("Students can only justify their own attendance records")) {
            return "Students can only justify their own attendance records.";
        }
        if (message.contains("Attendance record already has an absence justification")) {
            return "This attendance record already has a justification.";
        }
        if (message.contains("Absence justification reason is required")) {
            return "Justification reason is required.";
        }
        if (message.contains("Absence justification reason is too long")) {
            return "Justification reason is too long.";
        }
        if (message.contains("Attachment path is not allowed")) {
            return "Attachment path is not allowed.";
        }
        if (message.contains("Absence justification processed date cannot be before submission")) {
            return "The processing date cannot be before the submission date.";
        }
        if (message.contains("Absence justification processing requires a final decision")) {
            return "Processing requires approval or rejection.";
        }
        if (message.contains("Absence justification processed date is required")) {
            return "Processing date is required.";
        }
        if (message.contains("Absence justification already has a final state")) {
            return "This justification already has a final decision.";
        }
        if (message.contains("Decision notes are too long")) {
            return "Decision notes are too long.";
        }
        if (message.contains("Unknown")) {
            return "The selected record does not exist.";
        }
        return message;
    }

    private static void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveFlash(session, request, FLASH_SUCCESS, "successMessage");
        moveFlash(session, request, FLASH_ERROR, "errorMessage");
    }

    private static void moveFlash(HttpSession session, HttpServletRequest request, String sessionKey, String requestKey) {
        Object value = session.getAttribute(sessionKey);
        if (value != null) {
            request.setAttribute(requestKey, value);
            session.removeAttribute(sessionKey);
        }
    }
}
