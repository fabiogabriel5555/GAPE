package pt.isel.gape.web.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.storage.UploadRootResolver;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.CertificateValidationResult;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.GradeSheetUpdateCommand;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.service.CertificateService;
import pt.isel.gape.learning.service.GradeCertificateReadService;
import pt.isel.gape.learning.service.GradeRecordService;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.web.view.CertificateView;
import pt.isel.gape.web.view.CertificateValidationView;
import pt.isel.gape.web.view.GradeDocumentView;
import pt.isel.gape.web.view.GradeRecordView;
import pt.isel.gape.web.view.GradeSheetView;
import pt.isel.gape.web.view.SelectOptionView;

@WebServlet(name = "gradeCertificateServlet", urlPatterns = {
        "/learning/grades",
        "/learning/grades/*",
        "/student/grades",
        "/student/grades/*",
        "/certificates/validate",
        "/certificates/validate/*"
})
public final class GradeCertificateServlet extends DashboardServletSupport {

    private static final String MANAGEMENT_JSP = "/WEB-INF/views/learning/grades-certificates.jsp";
    private static final String ADMIN_MANAGEMENT_JSP = "/admin/admin/grade/admin-grades-certificates.jsp";
    private static final String COORDINATOR_MANAGEMENT_JSP = "/coordinator/coordinator/grade/coordinator-grades-certificates.jsp";
    private static final String INSTRUCTOR_MANAGEMENT_JSP = "/instructor/instructor/grade/instructor-grades-certificates.jsp";
    private static final String STUDENT_JSP = "/student/student/grade/student-grades-certificates.jsp";
    private static final String VALIDATION_JSP = "/WEB-INF/views/public/certificate-validation.jsp";
    private static final DateTimeFormatter DOCUMENT_DATE = ApplicationDateTimeFormat.DISPLAY_DATE;
    private static final int MANAGEMENT_PAGE_SIZE = 10;

    private final GradeSheetService gradeSheetService;
    private final GradeRecordService gradeRecordService;
    private final CertificateService certificateService;
    private final GradeCertificateReadService readService;
    private final Clock clock;

    public GradeCertificateServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private GradeCertificateServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new GradeSheetService(connectionProvider, clock),
                new GradeRecordService(connectionProvider, clock),
                new CertificateService(connectionProvider, clock),
                new GradeCertificateReadService(connectionProvider),
                clock
        );
    }

    GradeCertificateServlet(
            ConnectionProvider connectionProvider,
            GradeSheetService gradeSheetService,
            GradeRecordService gradeRecordService,
            CertificateService certificateService,
            GradeCertificateReadService readService
    ) {
        this(
                connectionProvider,
                gradeSheetService,
                gradeRecordService,
                certificateService,
                readService,
                ApplicationClock.system()
        );
    }

    GradeCertificateServlet(
            ConnectionProvider connectionProvider,
            GradeSheetService gradeSheetService,
            GradeRecordService gradeRecordService,
            CertificateService certificateService,
            GradeCertificateReadService readService,
            Clock clock
    ) {
        Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.gradeSheetService = Objects.requireNonNull(gradeSheetService, "gradeSheetService is required");
        this.gradeRecordService = Objects.requireNonNull(gradeRecordService, "gradeRecordService is required");
        this.certificateService = Objects.requireNonNull(certificateService, "certificateService is required");
        this.readService = Objects.requireNonNull(readService, "readService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if (servletPath.startsWith("/certificates/validate")) {
            showCertificateValidation(request, response);
            return;
        }
        String[] segments = pathSegments(request.getPathInfo());
        if (servletPath.startsWith("/student/grades")) {
            if (segments.length == 3 && "certificates".equals(segments[0]) && "download".equals(segments[2])) {
                downloadCertificate(request, response, Long.parseLong(segments[1]));
                return;
            }
            redirect(request, response, "/student/attendance#grades-certificates");
            return;
        }
        if (segments.length == 3 && "sheets".equals(segments[0]) && "download".equals(segments[2])) {
            downloadGradeSheet(request, response, Long.parseLong(segments[1]));
            return;
        }
        if (segments.length == 5
                && "courses".equals(segments[0])
                && "subjects".equals(segments[2])
                && "download".equals(segments[4])) {
            downloadSubjectGradeSheet(request, response, Long.parseLong(segments[1]), null, Long.parseLong(segments[3]));
            return;
        }
        if (segments.length == 7
                && "courses".equals(segments[0])
                && "occurrences".equals(segments[2])
                && "subjects".equals(segments[4])
                && "download".equals(segments[6])) {
            downloadSubjectGradeSheet(
                    request,
                    response,
                    Long.parseLong(segments[1]),
                    Long.parseLong(segments[3]),
                    Long.parseLong(segments[5])
            );
            return;
        }
        if (segments.length == 3 && "certificates".equals(segments[0]) && "download".equals(segments[2])) {
            downloadCertificate(request, response, Long.parseLong(segments[1]));
            return;
        }
        redirect(request, response, "/learning/attendance#grade-sheets");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!request.getServletPath().startsWith("/learning/grades")) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        String[] segments = pathSegments(request.getPathInfo());
        String sourceIp = request.getRemoteAddr();
        boolean jsonResponse = wantsJson(request);
        try {
            if (segments.length == 3 && "sheets".equals(segments[0]) && "configure".equals(segments[2])) {
                configureGradeSheet(request, actor, profile, Long.parseLong(segments[1]), sourceIp);
                if (jsonResponse) {
                    writeJsonStatus(response, HttpServletResponse.SC_OK, true, "");
                    return;
                }
                redirect(request, response, "/learning/attendance#grade-sheets");
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            if (jsonResponse) {
                writeJsonStatus(response, HttpServletResponse.SC_BAD_REQUEST, false, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
            redirect(request, response, failureRedirect(segments));
        }
    }

    private String managementJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_MANAGEMENT_JSP;
            case TEACHER -> INSTRUCTOR_MANAGEMENT_JSP;
            default -> ADMIN_MANAGEMENT_JSP;
        };
    }

    private static String failureRedirect(String[] segments) {
        if (segments.length > 0 && "certificates".equals(segments[0])) {
            return "/learning/attendance#certificates";
        }
        return "/learning/attendance#grade-sheets";
    }

    private void downloadGradeSheet(HttpServletRequest request, HttpServletResponse response, long gradeSheetId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        String sourceIp = request.getRemoteAddr();
        try {
            GradeSheet gradeSheet = gradeSheetService.getGradeSheet(
                    actor.userId(),
                    currentSessionId(request),
                    profile,
                    gradeSheetId,
                    sourceIp
            );
            Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
            Map<Long, ClassGroup> classGroups = mapById(readService.findAllClassGroups(), ClassGroup::id);
            Map<Long, Assessment> assessments = mapById(readService.findAllAssessments(), Assessment::id);
            Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
            Map<Long, CourseOccurrence> occurrences = mapById(readService.findAllCourseOccurrences(), CourseOccurrence::id);
            Map<Long, Organization> organizations = mapById(readService.findActiveOrganizations(), Organization::id);
            Map<Long, OrganicUnit> organicUnits = loadOrganicUnits(organizations);
            Map<Long, User> users = mapById(readService.findAllUsers(), User::id);
            GradeSheetView sheetView = gradeSheetView(
                    gradeSheet,
                    subjects,
                    classGroups,
                    assessments,
                    courses,
                    occurrences,
                    organizations,
                    organicUnits,
                    users
            );
            writeGradeSheetDownload(response, request.getParameter("format"), "grade-sheet-" + gradeSheetId, sheetView);
        } catch (SQLException exception) {
            throw new ServletException("Failed to download grade sheet", exception);
        } catch (RuntimeException exception) {
            throw new ServletException(messageFor(exception), exception);
        }
    }

    private void downloadSubjectGradeSheet(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            Long courseOccurrenceId,
            long subjectId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        String sourceIp = request.getRemoteAddr();
        try {
            GradeSheetCourseGroupView courseGroup = findCourseGroup(
                    gradeSheetGroups(visibleGradeSheetViews(actor, profile, sourceIp)),
                    courseId
            );
            GradeSheetSubjectGroupView subjectGroup = courseOccurrenceId == null
                    ? findSubjectGroup(courseGroup, subjectId)
                    : findSubjectGroup(courseGroup, subjectId, courseOccurrenceId);
            writeGradeDocumentDownload(
                    response,
                    request.getParameter("format"),
                    "subject-grade-sheet-" + courseId + "-" + subjectGroup.getOccurrenceId() + "-" + subjectId,
                    subjectGroup.getSubjectDocument()
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to download subject grade sheet", exception);
        } catch (RuntimeException exception) {
            throw new ServletException(messageFor(exception), exception);
        }
    }

    private void downloadCertificate(HttpServletRequest request, HttpServletResponse response, long certificateId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        String sourceIp = request.getRemoteAddr();
        try {
            Certificate certificate = certificateService.getCertificate(
                    actor.userId(),
                    currentSessionId(request),
                    profile,
                    certificateId,
                    sourceIp
            );
            if (!certificateComplete(certificate)
                    && (profile == AccessProfileType.STUDENT || certificate.state() != CertificateState.DRAFT)) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Certificate is incomplete");
                return;
            }
            Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
            Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
            Map<Long, User> users = mapById(readService.findAllUsers(), User::id);
            CertificateView certificateView = certificateView(certificate, courses, subjects, users, null);
            writeCertificateDownload(response, request.getParameter("format"), "certificate-" + certificateId, certificateView);
        } catch (SQLException exception) {
            throw new ServletException("Failed to download certificate", exception);
        } catch (RuntimeException exception) {
            throw new ServletException(messageFor(exception), exception);
        }
    }

    private void showManagement(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        String sourceIp = request.getRemoteAddr();
        try {
            populateManagementAttributes(request, actor, profile, sourceIp);
            prepareDashboard(request, "attendance", "Enrollments & Certificates");
            forward(request, response, managementJsp(request));
        } catch (SQLException exception) {
            throw new ServletException("Failed to load grade and certificate management", exception);
        }
    }

    private void showStudentGrades(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            populateStudentAttributes(request, actor);
            request.setAttribute("studentPageTitle", "Enrollments & Certificates");
            prepareDashboard(request, "attendance", "Enrollments & Certificates");
            forward(request, response, STUDENT_JSP);
        } catch (RuntimeException exception) {
            throw new ServletException(messageFor(exception), exception);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student grades and certificates", exception);
        }
    }

    void populateManagementAttributes(
            HttpServletRequest request,
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp
    ) throws SQLException {
        Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
        Map<Long, ClassGroup> classGroups = mapById(readService.findAllClassGroups(), ClassGroup::id);
        List<Assessment> assessments = readService.findAllAssessments();
        Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
        Map<Long, User> users = mapById(readService.findAllUsers(), User::id);

        List<GradeSheet> visibleGradeSheets = visibleGradeSheets(actor, profile, sourceIp);
        List<GradeSheetView> gradeSheetViews = visibleGradeSheetViews(visibleGradeSheets, subjects, courses, users);
        List<GradeRecordView> gradeRecordViews = visibleRecords(visibleGradeSheets, subjects, users);
        List<CertificateView> certificateViews = visibleCertificates(
                actor,
                profile,
                sourceIp,
                courses,
                subjects,
                users,
                visibleGradeSheets
        );
        List<Subject> optionSubjects = managementSubjects(actor, profile, subjects);
        List<ClassGroup> optionClassGroups = managementClassGroups(actor, profile, classGroups, optionSubjects);
        Set<Long> optionSubjectIds = optionSubjects.stream().map(Subject::id).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> optionClassGroupIds = optionClassGroups.stream().map(ClassGroup::id).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> optionCourseIds = optionClassGroups.stream().map(ClassGroup::courseId).collect(Collectors.toCollection(LinkedHashSet::new));
        visibleGradeSheets.stream()
                .flatMap(sheet -> sheet.classGroupIds().stream())
                .map(classGroups::get)
                .filter(Objects::nonNull)
                .map(ClassGroup::courseId)
                .forEach(optionCourseIds::add);
        Set<Long> optionStudentIds = new LinkedHashSet<>(readService.findActiveStudentIdsInClassGroups(List.copyOf(optionClassGroupIds)));

        /*
         * Page the grade sheets themselves, not the handful of course
         * aggregators produced for display.  A course can contain dozens of
         * sheets, and paging only the aggregators made a 83-sheet list look as
         * if it had four items and therefore suppressed the paginator.
         */
        List<GradeSheetView> activeGradeSheets = gradeSheetViews.stream()
                .filter(sheet -> !sheet.isPublished())
                .toList();
        List<GradeSheetView> publishedGradeSheets = gradeSheetViews.stream()
                .filter(GradeSheetView::isPublished)
                .toList();
        boolean publishedGradesScope = "published".equals(text(request, "gradesScope"));
        ManagementPage<GradeSheetView> gradePage = managementPage(
                request,
                "gradesPage",
                publishedGradesScope ? publishedGradeSheets : activeGradeSheets
        );
        List<CertificateStudentGroupView> allCertificateGroups = certificateGroups(certificateViews);
        List<CertificateStudentGroupView> activeCertificateGroups = allCertificateGroups.stream()
                .filter(group -> !group.isPublished())
                .toList();
        List<CertificateStudentGroupView> publishedCertificateGroups = allCertificateGroups.stream()
                .filter(CertificateStudentGroupView::isPublished)
                .toList();
        boolean publishedCertificatesScope = "published".equals(text(request, "certificatesScope"));
        ManagementPage<CertificateStudentGroupView> certificatePage = managementPage(
                request,
                "certificatesPage",
                publishedCertificatesScope ? publishedCertificateGroups : activeCertificateGroups
        );

        request.setAttribute("gradeSheets", gradeSheetViews);
        request.setAttribute("gradeSheetGroups", gradeSheetGroups(gradePage.rows()));
        request.setAttribute("gradeRecords", gradeRecordViews);
        request.setAttribute("certificates", certificateViews);
        request.setAttribute("certificateGroups", certificatePage.rows());
        request.setAttribute("certificateDraftDownloadsAllowed", profile != AccessProfileType.STUDENT);
        request.setAttribute("gradeSheetCount", gradeSheetViews.size());
        request.setAttribute("publishedGradeSheetCount", gradeSheetViews.stream().filter(GradeSheetView::isPublished).count());
        request.setAttribute("gradeRecordCount", gradeRecordViews.size());
        request.setAttribute("certificateCount", certificateViews.size());
        setManagementPageAttributes(
                request,
                "gradeManagement",
                publishedGradesScope ? "published" : "active",
                gradePage,
                publishedGradeSheets.size()
        );
        setManagementPageAttributes(
                request,
                "certificateManagement",
                publishedCertificatesScope ? "published" : "active",
                certificatePage,
                publishedCertificateGroups.size()
        );
        request.setAttribute("subjectOptions", subjectOptions(mapById(optionSubjects, Subject::id)));
        request.setAttribute("classGroupOptions", classGroupOptions(mapById(optionClassGroups, ClassGroup::id)));
        request.setAttribute("assessmentOptions", assessmentOptions(assessmentOptionsForContext(assessments, optionSubjectIds, optionClassGroupIds)));
        request.setAttribute("gradeSheetOptions", gradeSheetOptions(visibleGradeSheets));
        request.setAttribute("draftGradeSheetOptions", gradeSheetOptions(
                visibleGradeSheets.stream().filter(gradeSheet -> gradeSheet.state() == GradeSheetState.DRAFT).toList()));
        request.setAttribute("publishedGradeSheetOptions", gradeSheetOptions(
                visibleGradeSheets.stream().filter(gradeSheet -> gradeSheet.state().blocksDirectChanges()).toList()));
        request.setAttribute("courseOptions", courseOptions(courses.entrySet().stream()
                .filter(entry -> profile == AccessProfileType.ADMINISTRATOR || optionCourseIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new))));
        request.setAttribute("studentOptions", userOptions(readService.findActiveStudents().stream()
                .filter(user -> profile == AccessProfileType.ADMINISTRATOR || optionStudentIds.contains(user.id()))
                .toList()));
    }

    private static <T> ManagementPage<T> managementPage(
            HttpServletRequest request,
            String pageParameter,
            List<T> items
    ) {
        int total = items.size();
        String loadAllParameter = pageParameter.endsWith("Page")
                ? pageParameter.substring(0, pageParameter.length() - "Page".length()) + "LoadAll"
                : pageParameter + "LoadAll";
        boolean loadAll = Boolean.parseBoolean(text(request, loadAllParameter));
        int pageCount = Math.max(1, (total + MANAGEMENT_PAGE_SIZE - 1) / MANAGEMENT_PAGE_SIZE);
        int currentPage = loadAll
                ? 1
                : Math.min(pageCount, Math.max(1, integerParameter(request, pageParameter, 1)));
        int fromIndex = loadAll ? 0 : Math.min((currentPage - 1) * MANAGEMENT_PAGE_SIZE, total);
        int toIndex = loadAll ? total : Math.min(fromIndex + MANAGEMENT_PAGE_SIZE, total);
        return new ManagementPage<>(List.copyOf(items.subList(fromIndex, toIndex)), total, currentPage, pageCount, loadAll);
    }

    private static void setManagementPageAttributes(
            HttpServletRequest request,
            String prefix,
            String scope,
            ManagementPage<?> page,
            int publishedCount
    ) {
        request.setAttribute(prefix + "Scope", scope);
        request.setAttribute(prefix + "Total", page.total());
        request.setAttribute(prefix + "CurrentPage", page.currentPage());
        request.setAttribute(prefix + "PageCount", page.pageCount());
        request.setAttribute(prefix + "HasPreviousPage", page.currentPage() > 1);
        request.setAttribute(prefix + "HasNextPage", page.currentPage() < page.pageCount());
        request.setAttribute(prefix + "PreviousPage", Math.max(1, page.currentPage() - 1));
        request.setAttribute(prefix + "NextPage", Math.min(page.pageCount(), page.currentPage() + 1));
        request.setAttribute(prefix + "PublishedTotal", publishedCount);
        request.setAttribute(prefix + "LoadAll", page.loadAll());
    }

    private static int integerParameter(HttpServletRequest request, String name, int fallback) {
        String value = text(request, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record ManagementPage<T>(List<T> rows, int total, int currentPage, int pageCount, boolean loadAll) {
    }

    List<GradeSheetCourseGroupView> visibleSubjectGradeSheetCourseGroups(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            long subjectId
    ) throws SQLException {
        List<GradeSheetCourseGroupView> subjectCourseGroups = new ArrayList<>();
        for (GradeSheetCourseGroupView courseGroup : gradeSheetGroups(visibleGradeSheetViews(actor, profile, sourceIp))) {
            List<GradeSheetSubjectGroupView> subjectGroups = courseGroup.getSubjects().stream()
                    .filter(group -> group.getSubjectId() == subjectId)
                    .toList();
            if (!subjectGroups.isEmpty()) {
                subjectCourseGroups.add(new GradeSheetCourseGroupView(subjectGroups));
            }
        }
        return List.copyOf(subjectCourseGroups);
    }

    /**
     * Returns the grade-sheet groups for one subject at the granularity used by
     * Subject Details: one entry per course occurrence.  The newest grade sheet
     * in each entry defines the default order, so new records are consistently
     * shown before older ones.
     */
    List<GradeSheetSubjectGroupView> visibleSubjectGradeSheetOccurrenceGroups(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            long subjectId
    ) throws SQLException {
        return visibleSubjectGradeSheetCourseGroups(actor, profile, sourceIp, subjectId).stream()
                .flatMap(group -> group.getSubjects().stream())
                .sorted(Comparator.comparingLong(GradeSheetSubjectGroupView::getNewestSheetId).reversed())
                .toList();
    }

    int visibleSubjectGradeSheetCount(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            long subjectId
    ) throws SQLException {
        return visibleSubjectGradeSheetOccurrenceGroups(actor, profile, sourceIp, subjectId).size();
    }

    GradeSheetView visibleClassGroupGradeSheet(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            long classGroupId
    ) throws SQLException {
        return visibleGradeSheetViews(actor, profile, sourceIp).stream()
                .filter(sheet -> sheet.getClassGroupIds().contains(classGroupId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the class-group source sheet, including Draft sheets, for the
     * student Class Group Details modal.  The caller must already have
     * checked the student's active enrollment in that exact class group.
     */
    GradeSheetView studentClassGroupGradeSheet(long classGroupId) throws SQLException {
        GradeSheet gradeSheet = readService.findAllGradeSheets().stream()
                .filter(sheet -> sheet.classGroupIds().contains(classGroupId))
                .findFirst()
                .orElse(null);
        if (gradeSheet == null) {
            return null;
        }
        Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
        Map<Long, ClassGroup> classGroups = mapById(readService.findAllClassGroups(), ClassGroup::id);
        Map<Long, Assessment> assessments = mapById(readService.findAllAssessments(), Assessment::id);
        Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
        Map<Long, CourseOccurrence> occurrences = mapById(readService.findAllCourseOccurrences(), CourseOccurrence::id);
        Map<Long, Organization> organizations = mapById(readService.findActiveOrganizations(), Organization::id);
        Map<Long, OrganicUnit> organicUnits = loadOrganicUnits(organizations);
        Map<Long, User> users = mapById(readService.findAllUsers(), User::id);
        return gradeSheetView(
                gradeSheet,
                subjects,
                classGroups,
                assessments,
                courses,
                occurrences,
                organizations,
                organicUnits,
                users
        );
    }

    private List<GradeSheetView> visibleGradeSheetViews(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp
    ) throws SQLException {
        Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
        Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
        Map<Long, User> users = mapById(readService.findAllUsers(), User::id);
        return visibleGradeSheetViews(visibleGradeSheets(actor, profile, sourceIp), subjects, courses, users);
    }

    private List<Subject> managementSubjects(
            SessionUser actor,
            AccessProfileType profile,
            Map<Long, Subject> subjects
    ) throws SQLException {
        return switch (profile) {
            case ADMINISTRATOR -> subjects.values().stream().toList();
            case COORDINATOR -> readService.findSubjectsByCoordinator(actor.userId());
            case TEACHER -> readService.findSubjectsByTeacher(actor.userId());
            case STUDENT -> List.of();
        };
    }

    private List<ClassGroup> managementClassGroups(
            SessionUser actor,
            AccessProfileType profile,
            Map<Long, ClassGroup> classGroups,
            List<Subject> optionSubjects
    ) throws SQLException {
        if (profile == AccessProfileType.ADMINISTRATOR) {
            return classGroups.values().stream().toList();
        }
        Set<Long> allowedClassGroupIds = switch (profile) {
            case COORDINATOR -> new LinkedHashSet<>(readService.findCoordinatorManagedClassGroupIds(actor.userId()));
            case TEACHER -> new LinkedHashSet<>(readService.findTeacherManagedClassGroupIds(actor.userId()));
            default -> new LinkedHashSet<>();
        };
        Set<Long> allowedSubjectIds = optionSubjects.stream()
                .map(Subject::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return classGroups.values().stream()
                .filter(classGroup -> allowedClassGroupIds.contains(classGroup.id()))
                .filter(classGroup -> allowedSubjectIds.contains(classGroup.subjectId()))
                .toList();
    }

    private Map<Long, Assessment> assessmentOptionsForContext(
            List<Assessment> assessments,
            Set<Long> subjectIds,
            Set<Long> classGroupIds
    ) throws SQLException {
        Map<Long, Assessment> scoped = new LinkedHashMap<>();
        for (Assessment assessment : assessments) {
            boolean subjectVisible = assessment.subjectId() != null && subjectIds.contains(assessment.subjectId());
            List<Long> applicableClassGroups = readService.findApplicableClassGroupIds(assessment.id());
            boolean classGroupVisible = applicableClassGroups.stream().anyMatch(classGroupIds::contains);
            if ((subjectVisible && (applicableClassGroups.isEmpty() || classGroupVisible))
                    || (assessment.subjectId() == null && classGroupVisible)) {
                scoped.put(assessment.id(), assessment);
            }
        }
        return scoped;
    }

    private List<GradeSheetView> visibleGradeSheetViews(
            List<GradeSheet> visibleGradeSheets,
            Map<Long, Subject> subjects,
            Map<Long, Course> courses,
            Map<Long, User> users
    ) throws SQLException {
        Map<Long, ClassGroup> classGroups = mapById(readService.findAllClassGroups(), ClassGroup::id);
        Map<Long, Assessment> assessments = mapById(readService.findAllAssessments(), Assessment::id);
        Map<Long, CourseOccurrence> occurrences = mapById(readService.findAllCourseOccurrences(), CourseOccurrence::id);
        Map<Long, Organization> organizations = mapById(readService.findActiveOrganizations(), Organization::id);
        Map<Long, OrganicUnit> organicUnits = loadOrganicUnits(organizations);
        return visibleGradeSheets.stream()
                .map(gradeSheet -> gradeSheetView(
                        gradeSheet,
                        subjects,
                        classGroups,
                        assessments,
                        courses,
                        occurrences,
                        organizations,
                        organicUnits,
                        users
                ))
                .toList();
    }

    void populateStudentAttributes(HttpServletRequest request, SessionUser actor) throws SQLException {
        if (!actor.profileTypes().contains(AccessProfileType.STUDENT)) {
            throw new SecurityException("Operation requires a student profile");
        }
        Map<Long, Subject> subjects = mapById(readService.findAllSubjects(), Subject::id);
        Map<Long, Course> courses = mapById(readService.findCatalogCourses(), Course::id);
        List<GradeRecord> records = gradeRecordService.listOwnGradeRecords(
                actor.userId(),
                currentSessionId(request),
                AccessProfileType.STUDENT,
                request.getRemoteAddr()
        );
        List<GradeRecordView> recordViews = records.stream()
                .map(record -> gradeRecordView(record, subjects, Map.of()))
                .toList();
        Map<Long, ClassGroup> classGroups = mapById(readService.findAllClassGroups(), ClassGroup::id);
        Map<Long, Assessment> assessments = mapById(readService.findAllAssessments(), Assessment::id);
        Map<Long, CourseOccurrence> occurrences = mapById(readService.findAllCourseOccurrences(), CourseOccurrence::id);
        Map<Long, User> users = mapById(readService.findAllUsers(), User::id);
        List<Long> ownSheetIds = records.stream().map(GradeRecord::gradeSheetId).distinct().toList();
        List<GradeSheetView> ownGradeSheets = readService.findAllGradeSheets().stream()
                .filter(sheet -> ownSheetIds.contains(sheet.id()))
                .map(sheet -> gradeSheetView(sheet, subjects, classGroups, assessments, courses, occurrences, Map.of(), Map.of(), users))
                .toList();
        List<CertificateView> certificateViews = certificateService.listOwnCertificates(
                        actor.userId(),
                        currentSessionId(request),
                        AccessProfileType.STUDENT,
                        request.getRemoteAddr()
                ).stream()
                .map(certificate -> certificateView(certificate, courses, subjects, Map.of(), null))
                .toList();

        request.setAttribute("gradeRecords", recordViews);
        request.setAttribute("gradeSheetGroups", gradeSheetGroups(ownGradeSheets));
        request.setAttribute("studentGradeSheets", ownGradeSheets);
        request.setAttribute("certificates", certificateViews);
        request.setAttribute("gradeRecordCount", recordViews.size());
        request.setAttribute("approvedGradeRecordCount", recordViews.stream()
                .filter(record -> "approved".equals(record.getResultValue()))
                .count());
        request.setAttribute("certificateCount", certificateViews.size());
    }

    private void showCertificateValidation(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = text(request, "code");
        if (code == null) {
            String[] segments = pathSegments(request.getPathInfo());
            if (segments.length == 1) {
                code = segments[0];
            } else if (segments.length > 1) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        try {
            if (code != null) {
                CertificateValidationResult result = certificateService.validateCertificate(
                        code,
                        request.getRemoteAddr()
                );
                String courseLabel = result.courseId() == null
                        ? null
                        : readService.findCourseById(result.courseId()).map(this::courseLabel).orElse(null);
                String occurrenceLabel = result.courseOccurrenceId() == null
                        ? null
                        : readService.findCourseOccurrenceById(result.courseOccurrenceId())
                                .map(CourseOccurrence::label)
                                .orElse(null);
                request.setAttribute(
                        "validation",
                        CertificateValidationView.from(result, courseLabel, occurrenceLabel)
                );
                request.setAttribute("validationCode", code);
            }
            forward(request, response, VALIDATION_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to validate certificate", exception);
        }
    }

    private void configureGradeSheet(
            HttpServletRequest request,
            SessionUser actor,
            AccessProfileType profile,
            long gradeSheetId,
            String sourceIp
    ) {
        GradeSheet existing = gradeSheetService.getGradeSheet(
                actor.userId(),
                currentSessionId(request),
                profile,
                gradeSheetId,
                sourceIp
        );
        Long subjectId = optionalLongParameter(request, "subjectId");
        long effectiveSubjectId = subjectId == null ? existing.subjectId() : subjectId;
        BigDecimal maxGrade = optionalDecimalParameter(request, "maxGrade");
        BigDecimal passingGrade = optionalDecimalParameter(request, "passingGrade");
        GradeSheetUpdateCommand command = new GradeSheetUpdateCommand(
                effectiveSubjectId,
                defaultText(request, "title", existing.title()),
                GradeSheetType.parse(defaultText(request, "type", existing.type().toDatabaseValue())),
                maxGrade == null ? existing.maxGrade() : maxGrade,
                passingGrade == null ? existing.passingGrade() : passingGrade,
                classGroupParametersForSubject(request, effectiveSubjectId),
                assessmentWeights(request)
        );
        gradeSheetService.updateGradeSheet(actor.userId(), currentSessionId(request), profile, gradeSheetId, command, sourceIp);
    }

    private List<Long> classGroupParametersForSubject(HttpServletRequest request, long subjectId) {
        List<Long> submitted = longParameters(request, "classGroupIds");
        if (submitted.isEmpty()) {
            return submitted;
        }
        try {
            Set<Long> compatibleIds = readService.findAllClassGroups().stream()
                    .filter(classGroup -> classGroup.subjectId() == subjectId)
                    .filter(classGroup -> classGroup.state() == ClassGroupState.ACTIVE)
                    .map(ClassGroup::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return submitted.stream()
                    .filter(compatibleIds::contains)
                    .distinct()
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to validate grade sheet class groups", exception);
        }
    }

    private List<GradeSheet> visibleGradeSheets(SessionUser actor, AccessProfileType profile, String sourceIp)
            throws SQLException {
        List<GradeSheet> visible = new ArrayList<>();
        for (GradeSheet gradeSheet : readService.findAllGradeSheets()) {
            try {
                GradeSheet synchronizedSheet = gradeSheetService.getGradeSheet(
                        actor.userId(),
                        null,
                        profile,
                        gradeSheet.id(),
                        sourceIp
                );
                visible.add(synchronizedSheet);
            } catch (SecurityException ignored) {
                // Hidden from this actor's management context.
            }
        }
        return List.copyOf(visible);
    }

    private List<GradeSheet> visibleSubjectGradeSheets(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            long subjectId
    ) throws SQLException {
        List<GradeSheet> visible = new ArrayList<>();
        for (GradeSheet gradeSheet : readService.findGradeSheetsBySubject(subjectId)) {
            try {
                visible.add(gradeSheetService.getGradeSheet(
                        actor.userId(),
                        null,
                        profile,
                        gradeSheet.id(),
                        sourceIp
                ));
            } catch (SecurityException ignored) {
                // Hidden from this actor's management context.
            }
        }
        return List.copyOf(visible);
    }

    private List<GradeRecordView> visibleRecords(
            List<GradeSheet> visibleGradeSheets,
            Map<Long, Subject> subjects,
            Map<Long, User> users
    ) throws SQLException {
        Map<Long, GradeSheet> gradeSheetsById = mapById(visibleGradeSheets, GradeSheet::id);
        List<GradeRecordView> views = new ArrayList<>();
        for (GradeSheet gradeSheet : visibleGradeSheets) {
            for (GradeRecord record : readService.findGradeRecordsBySheet(gradeSheet.id())) {
                views.add(gradeRecordView(record, subjects, users, gradeSheetsById.get(record.gradeSheetId())));
            }
        }
        return List.copyOf(views);
    }

    private List<CertificateView> visibleCertificates(
            SessionUser actor,
            AccessProfileType profile,
            String sourceIp,
            Map<Long, Course> courses,
            Map<Long, Subject> subjects,
            Map<Long, User> users,
            List<GradeSheet> visibleGradeSheets
    ) throws SQLException {
        Map<Long, GradeSheet> visibleById = mapById(visibleGradeSheets, GradeSheet::id);
        List<CertificateView> visible = new ArrayList<>();
        for (Certificate certificate : readService.findAllCertificates()) {
            try {
                Certificate visibleCertificate = certificateService.getCertificate(
                        actor.userId(),
                        null,
                        profile,
                        certificate.id(),
                        sourceIp
                );
                visible.add(certificateView(visibleCertificate, courses, subjects, users, visibleById));
            } catch (SecurityException ignored) {
                // Hidden from this actor's management context.
            }
        }
        return List.copyOf(visible);
    }

    private static List<GradeSheetCourseGroupView> gradeSheetGroups(List<GradeSheetView> gradeSheets) {
        Map<Long, Map<SubjectOccurrenceKey, List<GradeSheetView>>> byCourseAndSubjectOccurrence = new LinkedHashMap<>();
        gradeSheets.stream()
                .sorted(Comparator
                        .comparing(GradeSheetView::getCourseLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GradeSheetView::getCourseOccurrenceLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GradeSheetView::getSubjectLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GradeSheetView::getClassGroupCode, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GradeSheetView::getTitle, String.CASE_INSENSITIVE_ORDER))
                .forEach(sheet -> byCourseAndSubjectOccurrence
                        .computeIfAbsent(sheet.getCourseId(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(
                                new SubjectOccurrenceKey(sheet.getSubjectId(), sheet.getCourseOccurrenceId()),
                                ignored -> new ArrayList<>()
                        )
                        .add(sheet));
        List<GradeSheetCourseGroupView> groups = new ArrayList<>();
        for (Map<SubjectOccurrenceKey, List<GradeSheetView>> subjectMap : byCourseAndSubjectOccurrence.values()) {
            List<GradeSheetSubjectGroupView> subjectGroups = new ArrayList<>();
            for (List<GradeSheetView> subjectSheets : subjectMap.values()) {
                subjectGroups.add(new GradeSheetSubjectGroupView(List.copyOf(subjectSheets)));
            }
            groups.add(new GradeSheetCourseGroupView(List.copyOf(subjectGroups)));
        }
        return List.copyOf(groups);
    }

    private static GradeSheetCourseGroupView findCourseGroup(List<GradeSheetCourseGroupView> groups, long courseId) {
        return groups.stream()
                .filter(group -> group.getCourseId() == courseId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No subject grade sheets are available for course: " + courseId));
    }

    private static GradeSheetSubjectGroupView findSubjectGroup(GradeSheetCourseGroupView courseGroup, long subjectId) {
        return courseGroup.getSubjects().stream()
                .filter(group -> group.getSubjectId() == subjectId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Subject grade sheet not found: " + subjectId));
    }

    private static GradeSheetSubjectGroupView findSubjectGroup(
            GradeSheetCourseGroupView courseGroup,
            long subjectId,
            long courseOccurrenceId
    ) {
        return courseGroup.getSubjects().stream()
                .filter(group -> group.getSubjectId() == subjectId)
                .filter(group -> group.getOccurrenceId() == courseOccurrenceId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject grade sheet not found for occurrence: " + courseOccurrenceId
                ));
    }

    private static GradeDocumentView subjectDocument(GradeSheetSubjectGroupView group) {
        GradeSheetView primary = group.primarySheet();
        List<GradeDocumentView.ColumnView> columns;
        if (primary.getAssessmentColumns().isEmpty()) {
            columns = List.of(new GradeDocumentView.ColumnView("Class group grade sheets", ""));
        } else {
            columns = primary.getAssessmentColumns().stream()
                    .map(column -> new GradeDocumentView.ColumnView(
                            column.getTitle(),
                            column.getWeightLabel() + "%"
                    ))
                    .toList();
        }
        Map<Long, GradeDocumentView.RowView> rows = new LinkedHashMap<>();
        for (GradeSheetView sheet : List.of(primary)) {
            for (GradeSheetView.StudentGradeRowView row : sheet.getStudentRows()) {
                rows.putIfAbsent(row.getStudentId(), new GradeDocumentView.RowView(
                        row.getStudentId(),
                        row.getStudentName(),
                        normalizedAssessmentValues(row.getAssessmentValues(), columns.size()),
                        row.getFinalGradeValue()
                ));
            }
        }
        List<GradeDocumentView.RowView> documentRows = rows.values().stream()
                .sorted(Comparator.comparing(GradeDocumentView.RowView::getStudentName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new GradeDocumentView(
                "Subject grade sheet",
                "Subject",
                group.getSubjectName(),
                "<strong>" + GradeSheetView.escapeHtml(group.getCourseName()) + "</strong><span>" + group.getSubjectContextHtml() + "</span>",
                group.getSubjectContextTitle(),
                group.getPeriodLabel(),
                group.getSubjectPhoto(),
                "ph ph-image",
                "No grade records.",
                group.getRemarks(),
                columns,
                documentRows
        );
    }

    private static List<String> normalizedAssessmentValues(List<String> values, int columnCount) {
        int safeCount = Math.max(1, columnCount);
        List<String> normalized = new ArrayList<>();
        for (int index = 0; index < safeCount; index++) {
            normalized.add(index < values.size() ? values.get(index) : "-");
        }
        return normalized;
    }

    private static BigDecimal scaleGrade(BigDecimal grade, BigDecimal sourceMax, BigDecimal targetMax) {
        if (grade == null) {
            return null;
        }
        if (sourceMax == null || sourceMax.compareTo(BigDecimal.ZERO) <= 0
                || targetMax == null || targetMax.compareTo(BigDecimal.ZERO) <= 0
                || sourceMax.compareTo(targetMax) == 0) {
            return grade;
        }
        return grade.multiply(targetMax).divide(sourceMax, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal documentGrade(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static List<CertificateStudentGroupView> certificateGroups(List<CertificateView> certificates) {
        Map<Long, List<CertificateView>> byStudent = new LinkedHashMap<>();
        certificates.stream()
                .sorted((left, right) -> left.getStudentName().compareToIgnoreCase(right.getStudentName()))
                .forEach(certificate -> byStudent
                        .computeIfAbsent(certificate.getStudentUserId(), ignored -> new ArrayList<>())
                        .add(certificate));
        List<CertificateStudentGroupView> groups = new ArrayList<>();
        for (List<CertificateView> studentCertificates : byStudent.values()) {
            studentCertificates.sort((left, right) -> Long.compare(left.getId(), right.getId()));
            groups.add(new CertificateStudentGroupView(List.copyOf(studentCertificates)));
        }
        return List.copyOf(groups);
    }

    private GradeSheetView gradeSheetView(
            GradeSheet gradeSheet,
            Map<Long, Subject> subjects,
            Map<Long, ClassGroup> classGroups,
            Map<Long, Assessment> assessments,
            Map<Long, Course> courses,
            Map<Long, CourseOccurrence> occurrences,
            Map<Long, Organization> organizations,
            Map<Long, OrganicUnit> organicUnits,
            Map<Long, User> users
    ) {
        List<ClassGroup> sheetClassGroups = gradeSheet.classGroupIds().stream()
                .map(classGroups::get)
                .filter(Objects::nonNull)
                .toList();
        ClassGroup primaryClassGroup = sheetClassGroups.isEmpty() ? null : sheetClassGroups.get(0);
        Subject subject = subjects.get(gradeSheet.subjectId());
        CourseOccurrence occurrence = occurrences.get(gradeSheet.courseOccurrenceId());
        Course course = occurrence == null ? null : courses.get(occurrence.courseId());
        if (course == null && primaryClassGroup != null) {
            course = courses.get(primaryClassGroup.courseId());
        }
        if (course == null && !courses.isEmpty()) {
            course = courses.values().stream()
                    .filter(candidate -> candidate.organizationId() == (subject == null ? -1 : subject.organizationId()))
                    .findFirst()
                    .orElse(null);
        }
        Organization organization = course == null ? null : organizations.get(course.organizationId());
        OrganicUnit organicUnit = course == null || course.organicUnitId() == null
                ? null
                : organicUnits.get(course.organicUnitId());
        String classGroupLabel = sheetClassGroups.stream()
                .map(group -> group.code() + " (#" + group.id() + ")")
                .collect(Collectors.joining(", "));
        List<GradeAssessmentWeight> sheetAssessmentWeights = assessmentWeightsForSheet(gradeSheet);
        String assessmentWeightLabel = sheetAssessmentWeights.stream()
                .map(weight -> {
                    Assessment assessment = assessments.get(weight.assessmentId());
                    String title = assessment == null ? "Assessment " + weight.assessmentId() : assessment.title();
                    return title + " (" + GradeSheetView.gradeLabel(weight.weight()) + "%)";
                })
                .collect(Collectors.joining(", "));
        List<GradeSheetView.AssessmentColumnView> assessmentColumns = sheetAssessmentWeights.stream()
                .map(weight -> {
                    Assessment assessment = assessments.get(weight.assessmentId());
                    return new GradeSheetView.AssessmentColumnView(
                            weight.assessmentId(),
                            assessment == null ? "Assessment " + weight.assessmentId() : assessment.title(),
                            weight.weight()
                    );
                })
                .toList();
        List<GradeRecord> records = recordsForSheet(gradeSheet.id());
        List<GradeSheetStudentSeed> students = gradeSheetStudents(gradeSheet, users);
        Map<Long, GradeRecord> recordsByStudent = new LinkedHashMap<>();
        for (GradeRecord record : records) {
            recordsByStudent.putIfAbsent(record.studentUserId(), record);
        }
        Map<Long, Map<Long, BigDecimal>> assessmentScores = latestAssessmentScores(
                sheetAssessmentWeights,
                students
        );
        List<GradeSheetView.StudentGradeRowView> studentRows = new ArrayList<>();
        for (GradeSheetStudentSeed student : students) {
            GradeRecord record = recordsByStudent.get(student.studentUserId());
            Map<Long, BigDecimal> studentAssessmentScores = assessmentScores.getOrDefault(
                    student.studentUserId(),
                    Map.of()
            );
            List<String> assessmentValues = new ArrayList<>();
            for (GradeAssessmentWeight weight : sheetAssessmentWeights) {
                BigDecimal score = studentAssessmentScores.get(weight.assessmentId());
                assessmentValues.add(GradeSheetView.gradeLabel(score));
            }
            BigDecimal finalGrade = record == null ? null : record.value();
            studentRows.add(new GradeSheetView.StudentGradeRowView(
                    studentRows.size() + 1,
                    student.studentUserId(),
                    student.studentName(),
                    assessmentValues,
                    finalGrade,
                    record == null || record.result() == null
                            ? "-"
                            : record.result().toDatabaseValue()
            ));
        }
        String contextHtml = gradeSheetContextHtml(primaryClassGroup, subject, course, organicUnit, organization);
        String contextTitle = gradeSheetContextTitle(primaryClassGroup, subject, course, organicUnit, organization);
        String courseContextHtml = courseContextHtml(course, organicUnit, organization);
        String courseContextTitle = courseContextTitle(course, organicUnit, organization);
        String subjectContextHtml = gradeSheetContextHtml(null, subject, course, organicUnit, organization);
        String subjectContextTitle = gradeSheetContextTitle(null, subject, course, organicUnit, organization);
        return GradeSheetView.from(
                gradeSheet,
                course == null ? 0L : course.id(),
                courseLabel(course),
                course == null ? null : course.name(),
                course == null ? null : course.acronym(),
                course == null ? null : course.photo(),
                course == null ? null : course.certificateMaxGrade(),
                courseContextHtml,
                courseContextTitle,
                organization == null ? 0L : organization.id(),
                organization == null ? null : organization.name(),
                organicUnit == null ? null : organicUnit.id(),
                organicUnit == null ? null : organicUnit.name(),
                gradeSheet.courseOccurrenceId(),
                occurrence == null ? null : occurrence.label(),
                occurrenceDateRangeLabel(occurrence),
                occurrence == null ? null : occurrence.state().toDatabaseValue(),
                primaryClassGroup == null ? null : primaryClassGroup.id(),
                primaryClassGroup != null && primaryClassGroup.state() == ClassGroupState.COMPLETED,
                subjectLabel(subjects.get(gradeSheet.subjectId())),
                subject == null ? null : subject.name(),
                subject == null ? null : subject.acronym(),
                subject == null ? null : subject.photo(),
                subject == null ? null : subject.ects(),
                subject == null ? null : subject.finalGradeMax(),
                subjectContextHtml,
                subjectContextTitle,
                classGroupLabel,
                primaryClassGroup == null ? classGroupLabel : primaryClassGroup.code(),
                periodLabel(sheetClassGroups),
                contextHtml,
                contextTitle,
                assessmentWeightLabel,
                records.size(),
                assessmentColumns,
                studentRows
        );
    }

    private List<GradeAssessmentWeight> assessmentWeightsForSheet(GradeSheet gradeSheet) {
        if (gradeSheet.classGroupIds().isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> configuredWeights = new LinkedHashMap<>();
        for (GradeAssessmentWeight weight : gradeSheet.assessmentWeights()) {
            configuredWeights.putIfAbsent(weight.assessmentId(), weight.weight());
        }
        try {
            List<Long> contextAssessmentIds = readService.findAssessmentIdsForSheetContext(gradeSheet);
            if (contextAssessmentIds.isEmpty()) {
                return gradeSheet.assessmentWeights();
            }
            List<GradeAssessmentWeight> weights = new ArrayList<>();
            for (Long assessmentId : contextAssessmentIds) {
                weights.add(new GradeAssessmentWeight(assessmentId, configuredWeights.get(assessmentId)));
            }
            return List.copyOf(weights);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet assessment context", exception);
        }
    }

    private GradeRecordView gradeRecordView(
            GradeRecord record,
            Map<Long, Subject> subjects,
            Map<Long, User> users
    ) {
        GradeSheet gradeSheet;
        try {
            gradeSheet = readService.findGradeSheetById(record.gradeSheetId()).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet", exception);
        }
        return gradeRecordView(record, subjects, users, gradeSheet);
    }

    private GradeRecordView gradeRecordView(
            GradeRecord record,
            Map<Long, Subject> subjects,
            Map<Long, User> users,
            GradeSheet gradeSheet
    ) {
        User student = users.get(record.studentUserId());
        return GradeRecordView.from(
                record,
                gradeSheet == null ? null : gradeSheet.title(),
                gradeSheet == null ? null : subjectLabel(subjects.get(gradeSheet.subjectId())),
                student == null ? null : student.name(),
                student == null ? null : student.email()
        );
    }

    private CertificateView certificateView(
            Certificate certificate,
            Map<Long, Course> courses,
            Map<Long, Subject> subjects,
            Map<Long, User> users,
            Map<Long, GradeSheet> visibleGradeSheets
    ) {
        User student = users.get(certificate.studentUserId());
        Course course = courses.get(certificate.courseId());
        List<CertificateView.CertificateSubjectRowView> subjectRows = new ArrayList<>();
        Map<Long, GradeSheet> linkedSheetsBySubject = new LinkedHashMap<>();
        String gradeSheetLabel = certificate.gradeSheetIds().stream()
                .map(id -> {
                    GradeSheet gradeSheet = visibleGradeSheets == null ? null : visibleGradeSheets.get(id);
                    if (gradeSheet == null) {
                        try {
                            gradeSheet = readService.findGradeSheetById(id).orElse(null);
                        } catch (SQLException exception) {
                            throw new IllegalStateException("Failed to load certificate grade sheet", exception);
                        }
                    }
                    if (gradeSheet != null) {
                        linkedSheetsBySubject.putIfAbsent(gradeSheet.subjectId(), gradeSheet);
                    }
                    return gradeSheet == null ? "Grade sheet " + id : gradeSheet.title();
                })
                .collect(Collectors.joining(", "));
        if (course != null && certificate.state() != CertificateState.ISSUED) {
            try {
                for (CourseSubjectAssociation association : readService.findCourseSubjects(course.id())) {
                    Subject subject = subjects.get(association.subjectId());
                    GradeSheet gradeSheet = linkedSheetsBySubject.get(association.subjectId());
                    subjectRows.add(new CertificateView.CertificateSubjectRowView(
                            subjectLabel(subject),
                            subject == null ? null : subject.ects(),
                            gradeSheet == null
                                    ? null
                                    : finalGradeForCertificateSheet(gradeSheet.id(), certificate.studentUserId())
                    ));
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load certificate course subjects", exception);
            }
        } else {
            for (GradeSheet gradeSheet : linkedSheetsBySubject.values()) {
                Subject subject = subjects.get(gradeSheet.subjectId());
                subjectRows.add(new CertificateView.CertificateSubjectRowView(
                        subjectLabel(subject),
                        subject == null ? null : subject.ects(),
                        finalGradeForCertificateSheet(gradeSheet.id(), certificate.studentUserId())
                ));
            }
        }
        return CertificateView.from(
                certificate,
                courseLabel(courses.get(certificate.courseId())),
                course == null ? null : course.name(),
                course == null ? null : course.acronym(),
                course == null ? null : course.ects(),
                course == null ? null : course.certificateMaxGrade(),
                course == null ? null : course.duration(),
                student == null ? null : student.name(),
                student == null ? null : student.email(),
                gradeSheetLabel,
                subjectRows
        );
    }

    private List<GradeRecord> recordsForSheet(long gradeSheetId) {
        try {
            return readService.findGradeRecordsBySheet(gradeSheetId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade records", exception);
        }
    }

    private List<GradeSheetStudentSeed> gradeSheetStudents(
            GradeSheet gradeSheet,
            Map<Long, User> users
    ) {
        Map<Long, GradeSheetStudentSeed> students = new LinkedHashMap<>();
        try {
            for (Long studentUserId : readService.findStudentUserIdsForSheetContext(gradeSheet)) {
                putGradeSheetStudent(students, studentUserId, users);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet students", exception);
        }
        return students.values().stream()
                .sorted(Comparator
                        .comparing(GradeSheetStudentSeed::studentName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(GradeSheetStudentSeed::studentUserId))
                .toList();
    }

    private static void putGradeSheetStudent(
            Map<Long, GradeSheetStudentSeed> students,
            long studentUserId,
            Map<Long, User> users
    ) {
        students.putIfAbsent(
                studentUserId,
                new GradeSheetStudentSeed(studentUserId, studentName(users.get(studentUserId)))
        );
    }

    private Map<Long, Map<Long, BigDecimal>> latestAssessmentScores(
            List<GradeAssessmentWeight> weights,
            List<GradeSheetStudentSeed> students
    ) {
        Map<Long, Map<Long, BigDecimal>> scores = new LinkedHashMap<>();
        if (weights.isEmpty() || students.isEmpty()) {
            return scores;
        }
        try {
            for (GradeSheetStudentSeed student : students) {
                Map<Long, BigDecimal> studentScores = new LinkedHashMap<>();
                for (GradeAssessmentWeight weight : weights) {
                    readService.findLatestCorrectedAssessmentScore(student.studentUserId(), weight.assessmentId())
                            .ifPresent(score -> studentScores.put(weight.assessmentId(), score));
                }
                scores.put(student.studentUserId(), studentScores);
            }
            return scores;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load assessment grades", exception);
        }
    }

    private BigDecimal finalGradeForCertificateSheet(long gradeSheetId, long studentUserId) {
        try {
            GradeSheet gradeSheet = readService.findGradeSheetById(gradeSheetId).orElse(null);
            if (gradeSheet == null || !gradeSheetCompleteForCertificateStudent(gradeSheet, studentUserId)) {
                return null;
            }
            return readService.findActiveGradeRecord(gradeSheetId, studentUserId)
                    .map(GradeRecord::value)
                    .orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load certificate grade record", exception);
        }
    }

    private boolean gradeSheetCompleteForCertificateStudent(
            GradeSheet gradeSheet,
            long studentUserId
    ) throws SQLException {
        List<Long> assessmentIds = gradeSheetAssessmentIdsForCertificate(gradeSheet);
        for (Long assessmentId : assessmentIds) {
            if (readService.findLatestCorrectedAssessmentScore(studentUserId, assessmentId).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<Long> gradeSheetAssessmentIdsForCertificate(GradeSheet gradeSheet)
            throws SQLException {
        List<Long> contextAssessmentIds = readService.findAssessmentIdsForSheetContext(gradeSheet);
        if (!contextAssessmentIds.isEmpty()) {
            return contextAssessmentIds;
        }
        return gradeSheet.assessmentWeights().stream()
                .map(GradeAssessmentWeight::assessmentId)
                .toList();
    }

    private Map<Long, OrganicUnit> loadOrganicUnits(Map<Long, Organization> organizations) throws SQLException {
        Map<Long, OrganicUnit> units = new LinkedHashMap<>();
        for (Long organizationId : organizations.keySet()) {
            for (OrganicUnit unit : readService.findOrganicUnitsByOrganization(organizationId)) {
                units.put(unit.id(), unit);
            }
        }
        return units;
    }

    private static String studentName(User user) {
        return user == null ? "Student" : user.name();
    }

    private static String periodLabel(List<ClassGroup> classGroups) {
        LocalDate startsAt = null;
        LocalDate endsAt = null;
        for (ClassGroup classGroup : classGroups) {
            if (classGroup.startsAt() != null && (startsAt == null || classGroup.startsAt().isBefore(startsAt))) {
                startsAt = classGroup.startsAt();
            }
            if (classGroup.endsAt() != null && (endsAt == null || classGroup.endsAt().isAfter(endsAt))) {
                endsAt = classGroup.endsAt();
            }
        }
        if (startsAt == null && endsAt == null) {
            return "__-__-____ - __-__-____";
        }
        return (startsAt == null ? "__-__-____" : DOCUMENT_DATE.format(startsAt))
                + " - "
                + (endsAt == null ? "__-__-____" : DOCUMENT_DATE.format(endsAt));
    }

    private static String occurrenceDateRangeLabel(CourseOccurrence occurrence) {
        if (occurrence == null) {
            return null;
        }
        return (occurrence.startsAt() == null ? "-" : ApplicationDateTimeFormat.date(occurrence.startsAt()))
                + " to "
                + (occurrence.endsAt() == null ? "-" : ApplicationDateTimeFormat.date(occurrence.endsAt()));
    }

    private static String courseContextHtml(Course course, OrganicUnit organicUnit, Organization organization) {
        List<String> parts = new ArrayList<>();
        if (course != null) {
            parts.add(GradeSheetView.contextPartHtml(course.acronym(), course.name()));
        }
        if (organicUnit != null) {
            parts.add(GradeSheetView.contextPartHtml(organicUnit.acronym(), organicUnit.name()));
        }
        if (organization != null) {
            parts.add(GradeSheetView.contextPartHtml(organization.acronym(), organization.name()));
        }
        return String.join(" | ", parts);
    }

    private static String courseContextTitle(Course course, OrganicUnit organicUnit, Organization organization) {
        List<String> parts = new ArrayList<>();
        if (course != null) {
            parts.add(course.name());
        }
        if (organicUnit != null) {
            parts.add(organicUnit.name());
        }
        if (organization != null) {
            parts.add(organization.name());
        }
        return String.join(" | ", parts);
    }

    private static String gradeSheetContextHtml(
            ClassGroup classGroup,
            Subject subject,
            Course course,
            OrganicUnit organicUnit,
            Organization organization
    ) {
        List<String> parts = new ArrayList<>();
        if (classGroup != null) {
            parts.add(GradeSheetView.contextPartHtml(classGroup.code(), classGroup.code()));
        }
        if (subject != null) {
            parts.add(GradeSheetView.contextPartHtml(subject.acronym(), subject.name()));
        }
        if (course != null) {
            parts.add(GradeSheetView.contextPartHtml(course.acronym(), course.name()));
        }
        if (organicUnit != null) {
            parts.add(GradeSheetView.contextPartHtml(organicUnit.acronym(), organicUnit.name()));
        }
        if (organization != null) {
            parts.add(GradeSheetView.contextPartHtml(organization.acronym(), organization.name()));
        }
        return String.join(" | ", parts);
    }

    private static String gradeSheetContextTitle(
            ClassGroup classGroup,
            Subject subject,
            Course course,
            OrganicUnit organicUnit,
            Organization organization
    ) {
        List<String> parts = new ArrayList<>();
        if (classGroup != null) {
            parts.add(classGroup.code());
        }
        if (subject != null) {
            parts.add(subject.name());
        }
        if (course != null) {
            parts.add(course.name());
        }
        if (organicUnit != null) {
            parts.add(organicUnit.name());
        }
        if (organization != null) {
            parts.add(organization.name());
        }
        return String.join(" | ", parts);
    }

    private List<SelectOptionView> subjectOptions(Map<Long, Subject> subjects) {
        return subjects.values().stream()
                .map(subject -> new SelectOptionView(Long.toString(subject.id()), subjectLabel(subject), false))
                .toList();
    }

    private List<SelectOptionView> classGroupOptions(Map<Long, ClassGroup> classGroups) {
        return classGroups.values().stream()
                .map(group -> new SelectOptionView(Long.toString(group.id()), group.code() + " (#" + group.id() + ")", false))
                .toList();
    }

    private List<SelectOptionView> assessmentOptions(Map<Long, Assessment> assessments) {
        return assessments.values().stream()
                .map(assessment -> new SelectOptionView(Long.toString(assessment.id()),
                        assessment.title() + " (#" + assessment.id() + ")", false))
                .toList();
    }

    private List<SelectOptionView> gradeSheetOptions(List<GradeSheet> gradeSheets) {
        return gradeSheets.stream()
                .map(gradeSheet -> new SelectOptionView(Long.toString(gradeSheet.id()),
                        gradeSheet.title() + " (#" + gradeSheet.id() + ")", false))
                .toList();
    }

    private List<SelectOptionView> courseOptions(Map<Long, Course> courses) {
        return courses.values().stream()
                .map(course -> new SelectOptionView(Long.toString(course.id()), courseLabel(course), false))
                .toList();
    }

    private List<SelectOptionView> userOptions(List<User> users) {
        return users.stream()
                .map(user -> new SelectOptionView(Long.toString(user.id()), user.name() + " (#" + user.id() + ")", false))
                .toList();
    }

    private List<GradeAssessmentWeight> assessmentWeights(HttpServletRequest request) {
        String[] ids = request.getParameterValues("assessmentId");
        String[] weights = request.getParameterValues("assessmentWeight");
        if (ids == null || weights == null) {
            return List.of();
        }
        int count = Math.min(ids.length, weights.length);
        List<GradeAssessmentWeight> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            if (ids[index] == null || ids[index].isBlank() || weights[index] == null || weights[index].isBlank()) {
                continue;
            }
            values.add(new GradeAssessmentWeight(Long.parseLong(ids[index].trim()), new BigDecimal(weights[index].trim())));
        }
        return List.copyOf(values);
    }

    private static List<Long> longParameters(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                ids.add(Long.parseLong(value.trim()));
            }
        }
        return List.copyOf(ids);
    }

    private static Long optionalLongParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static BigDecimal decimalParameter(HttpServletRequest request, String name) {
        return new BigDecimal(requiredText(request, name));
    }

    private static BigDecimal optionalDecimalParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : new BigDecimal(value);
    }

    private static String requiredText(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String defaultText(HttpServletRequest request, String name, String defaultValue) {
        String value = text(request, name);
        return value == null ? defaultValue : value;
    }

    private static boolean wantsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || "fetch".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json"));
    }

    private static void writeJsonStatus(
            HttpServletResponse response,
            int status,
            boolean success,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":" + success
                + ",\"message\":\"" + json(message) + "\"}");
    }

    private static String json(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void writeGradeSheetDownload(
            HttpServletResponse response,
            String requestedFormat,
            String baseFilename,
            GradeSheetView sheet
    ) throws IOException {
        writeGradeDocumentDownload(response, requestedFormat, baseFilename, sheet.getDocument());
    }

    private void writeGradeDocumentDownload(
            HttpServletResponse response,
            String requestedFormat,
            String baseFilename,
            GradeDocumentView document
    ) throws IOException {
        String format = requestedFormat == null ? "pdf" : requestedFormat.trim().toLowerCase();
        if ("excel".equals(format) || "xls".equals(format)) {
            writeHtmlExcel(response, baseFilename, gradeDocumentExcelHtml(document, imageDataUri(document.getImagePath())));
            return;
        }
        byte[] pdf = gradeDocumentPdf(document, resolveMediaPath(document.getImagePath()));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + baseFilename + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private static void writeCertificateDownload(
            HttpServletResponse response,
            String requestedFormat,
            String baseFilename,
            CertificateView certificate
    ) throws IOException {
        String format = requestedFormat == null ? "pdf" : requestedFormat.trim().toLowerCase();
        if ("excel".equals(format) || "xls".equals(format)) {
            writeHtmlExcel(response, baseFilename, certificateExcelHtml(certificate));
            return;
        }
        byte[] pdf = certificatePdf(certificate);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + baseFilename + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private static void writeHtmlExcel(HttpServletResponse response, String baseFilename, String html)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + baseFilename + ".xls\"");
        response.getWriter().write(html);
    }

    private byte[] gradeDocumentPdf(GradeDocumentView view, Path imagePath) throws IOException {
        try (PDDocument pdf = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            float left = 20;
            float rightMargin = 20;
            float idWidth = 38;
            float finalWidth = 76;
            int valueColumnCount = view.getVariableColumnCount();
            float nameWidth = gradeDocumentNameColumnWidth(bodyFont, boldFont, view);
            float valueWidth = gradeDocumentValueColumnWidth(boldFont, view);
            float tableWidth = idWidth + nameWidth + finalWidth + valueColumnCount * valueWidth;
            float pageWidth = Math.max(PDRectangle.A4.getHeight(), tableWidth + left + rightMargin);
            float pageHeight = PDRectangle.A4.getWidth();
            float availableWidth = pageWidth - left - rightMargin;
            if (tableWidth < availableWidth) {
                float extraWidth = availableWidth - tableWidth;
                nameWidth += extraWidth * 0.45f;
                valueWidth += (extraWidth * 0.45f) / valueColumnCount;
                finalWidth += extraWidth * 0.10f;
                tableWidth = idWidth + nameWidth + finalWidth + valueColumnCount * valueWidth;
                finalWidth += availableWidth - tableWidth;
            }
            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
            pdf.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                float right = pageWidth - rightMargin;
                float headerTop = pageHeight - 16;
                float headerHeight = 126;
                float headerBottom = headerTop - headerHeight;

                drawDocumentHeader(stream, pdf, bodyFont, boldFont, view, imagePath, left, right, headerBottom, headerHeight);

                float tableTop = headerBottom - 10;
                float tableBottom = 96;
                float headerRowHeight = 27;
                int rowCount = Math.max(1, view.getRows().size());
                float rowHeight = Math.min(23, Math.max(14, (tableTop - tableBottom - headerRowHeight) / rowCount));

                drawGradeDocumentHeader(stream, boldFont, left, tableTop, headerRowHeight, idWidth, nameWidth,
                        valueWidth, finalWidth, view);
                float y = tableTop - headerRowHeight;
                if (view.getRows().isEmpty()) {
                    drawGradeDocumentEmptyRow(stream, bodyFont, left, y, rowHeight, idWidth, nameWidth,
                            valueWidth, finalWidth, valueColumnCount, view.getEmptyMessage());
                } else {
                    for (GradeDocumentView.RowView row : view.getRows()) {
                        if (y - rowHeight < tableBottom) {
                            break;
                        }
                        drawGradeDocumentRow(stream, bodyFont, boldFont, left, y, rowHeight, idWidth, nameWidth,
                                valueWidth, finalWidth, row, valueColumnCount);
                        y -= rowHeight;
                    }
                }

                if (view.isHasAlert()) {
                    writeFittedPdfLine(stream, bodyFont, 8, view.getAlert(), left, 76, 360);
                }
                drawCenteredFittedText(stream, bodyFont, 10, "Responsible person", right - 130, 56, 180);
                stream.moveTo(right - 226, 40);
                stream.lineTo(right - 6, 40);
                stream.stroke();
            }
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private static float gradeDocumentNameColumnWidth(
            PDType1Font bodyFont,
            PDType1Font boldFont,
            GradeDocumentView view
    ) throws IOException {
        float width = Math.max(170, pdfTextWidth(boldFont, 9, "Student name") + 18);
        for (GradeDocumentView.RowView row : view.getRows()) {
            width = Math.max(width, pdfTextWidth(bodyFont, 9, row.getStudentName()) + 18);
        }
        return width;
    }

    private static float gradeDocumentValueColumnWidth(PDType1Font boldFont, GradeDocumentView view)
            throws IOException {
        float width = Math.max(84, pdfTextWidth(boldFont, 8, "Avaliacoes") + 18);
        for (GradeDocumentView.ColumnView column : view.getColumns()) {
            width = Math.max(width, pdfTextWidth(boldFont, 8, column.getHeaderLabel()) + 18);
        }
        return width;
    }

    private void drawDocumentHeader(
            PDPageContentStream stream,
            PDDocument pdf,
            PDType1Font bodyFont,
            PDType1Font boldFont,
            GradeDocumentView view,
            Path imagePath,
            float left,
            float right,
            float bottom,
            float height
    ) throws IOException {
        float width = right - left;
        float top = bottom + height;
        strokeRect(stream, left, bottom, width, height, Color.BLACK);
        writeFittedPdfLine(stream, boldFont, 11, "Published on: ___ / ___ / _____", left + 10, top - 24, 190);

        float imageSize = 46;
        float imageY = top - 58;
        float institutionX = left + Math.min(276, Math.max(220, width * 0.30f));
        float mediaWidth = 138;
        float imageX = institutionX + (mediaWidth - imageSize) / 2;
        drawDocumentImage(stream, pdf, boldFont, view, imagePath, imageX, imageY, imageSize);

        float separatorX = institutionX + mediaWidth;
        stream.moveTo(separatorX, top - 10);
        stream.lineTo(separatorX, top - 62);
        stream.stroke();

        writeFittedPdfLine(stream, boldFont, 10, documentBrandLine(view), separatorX + 14,
                top - 31, right - separatorX - 28);

        float entityY = bottom + 24;
        String entityLabel = view.getEntityLabel() + ":";
        float entityX = left + 10;
        float entityNameX = entityX + pdfTextWidth(boldFont, 11, entityLabel) + 14;
        float entityUnderlineEnd = left + 238;
        writeFittedPdfLine(stream, boldFont, 11, entityLabel, entityX, entityY, entityNameX - entityX - 6);
        writeFittedPdfLine(stream, bodyFont, 11, view.getEntityName(), entityNameX, entityY,
                Math.max(80, entityUnderlineEnd - entityNameX));
        stream.moveTo(left + 10, entityY - 6);
        stream.lineTo(entityUnderlineEnd, entityY - 6);
        stream.stroke();

        drawCenteredFittedText(stream, boldFont, 18, view.getTitle(), left + width / 2, bottom + 18, 260);
        writeFittedPdfLine(stream, boldFont, 10, "Academic period: " + view.getPeriodLabel(), right - 205, bottom + 17, 198);
    }

    private static String documentBrandTitle(GradeDocumentView view) {
        String name = firstStrongText(view.getContextHtml());
        if (name.isBlank()) {
            name = view.getEntityName();
        }
        if (name == null || name.isBlank()) {
            name = view.getContextTitle();
        }
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }

    private static String documentBrandContext(GradeDocumentView view) {
        String context = htmlToPlainText(view.getContextHtml());
        if (context == null || context.isBlank()) {
            return "";
        }
        String name = firstStrongText(view.getContextHtml());
        if (name == null || name.isBlank()) {
            name = view.getEntityName();
        }
        if (name != null && !name.isBlank()) {
            String trimmed = context.trim();
            String heading = name.trim();
            if (trimmed.equalsIgnoreCase(heading)) {
                return "";
            }
            if (trimmed.regionMatches(true, 0, heading, 0, heading.length())) {
                String suffix = trimmed.substring(heading.length()).trim();
                while (suffix.startsWith("|") || suffix.startsWith("-")) {
                    suffix = suffix.substring(1).trim();
                }
                if (!suffix.isBlank()) {
                    return suffix;
                }
            }
        }
        return context;
    }

    private static String documentBrandLine(GradeDocumentView view) {
        String title = documentBrandTitle(view);
        String context = documentBrandContext(view);
        if (title.isBlank()) {
            return context;
        }
        if (context.isBlank()) {
            return title;
        }
        return title + " | " + context;
    }

    private static String firstStrongText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<strong>");
        int end = lower.indexOf("</strong>");
        if (start < 0 || end <= start) {
            return "";
        }
        return decodeBasicHtml(html.substring(start + "<strong>".length(), end)).trim();
    }

    private static String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return decodeBasicHtml(html.replaceAll("<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String decodeBasicHtml(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static void drawDocumentImage(
            PDPageContentStream stream,
            PDDocument pdf,
            PDType1Font boldFont,
            GradeDocumentView view,
            Path imagePath,
            float x,
            float y,
            float size
    ) throws IOException {
        if (imagePath != null && Files.isRegularFile(imagePath)) {
            try {
                PDImageXObject image = PDImageXObject.createFromFileByExtension(imagePath.toFile(), pdf);
                stream.drawImage(image, x, y, size, size);
                return;
            } catch (RuntimeException | IOException ignored) {
                // Fall back to the same placeholder block used by the UI.
            }
        }
        fillRect(stream, x, y, size, size, new Color(238, 242, 247));
        strokeRect(stream, x, y, size, size, new Color(216, 224, 234));
        drawPdfPlaceholderIcon(stream, x, y, size);
    }

    private static void drawPdfPlaceholderIcon(
            PDPageContentStream stream,
            float x,
            float y,
            float size
    ) throws IOException {
        Color iconColor = new Color(31, 59, 87);
        float iconWidth = size * 0.36f;
        float iconHeight = size * 0.28f;
        float iconX = x + (size - iconWidth) / 2;
        float iconY = y + (size - iconHeight) / 2;
        stream.setStrokingColor(iconColor);
        stream.setLineWidth(1.3f);
        stream.addRect(iconX, iconY, iconWidth, iconHeight);
        stream.stroke();
        stream.moveTo(iconX + iconWidth * 0.12f, iconY + iconHeight * 0.22f);
        stream.lineTo(iconX + iconWidth * 0.34f, iconY + iconHeight * 0.50f);
        stream.lineTo(iconX + iconWidth * 0.48f, iconY + iconHeight * 0.36f);
        stream.lineTo(iconX + iconWidth * 0.68f, iconY + iconHeight * 0.64f);
        stream.lineTo(iconX + iconWidth * 0.88f, iconY + iconHeight * 0.28f);
        stream.stroke();
        float dotSize = size * 0.045f;
        stream.addRect(iconX + iconWidth * 0.68f, iconY + iconHeight * 0.66f, dotSize, dotSize);
        stream.stroke();
        stream.setLineWidth(1);
        stream.setStrokingColor(Color.BLACK);
    }

    private static void drawGradeDocumentHeader(
            PDPageContentStream stream,
            PDType1Font boldFont,
            float left,
            float y,
            float rowHeight,
            float idWidth,
            float nameWidth,
            float valueWidth,
            float finalWidth,
            GradeDocumentView view
    ) throws IOException {
        float baseline = centeredBaseline(y, rowHeight, 9);
        strokeRect(stream, left, y - rowHeight, idWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, 9, "ID", left + idWidth / 2, baseline, idWidth - 4);
        strokeRect(stream, left + idWidth, y - rowHeight, nameWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, 9, "Student name", left + idWidth + nameWidth / 2, baseline, nameWidth - 8);
        float x = left + idWidth + nameWidth;
        if (view.getColumns().isEmpty()) {
            strokeRect(stream, x, y - rowHeight, valueWidth, rowHeight, Color.BLACK);
            drawCenteredFittedText(stream, boldFont, 8, "Avaliacoes", x + valueWidth / 2, baseline, valueWidth - 8);
            x += valueWidth;
        } else {
            float fontSize = valueWidth < 58 ? 6.6f : 8f;
            for (GradeDocumentView.ColumnView column : view.getColumns()) {
                strokeRect(stream, x, y - rowHeight, valueWidth, rowHeight, Color.BLACK);
                drawCenteredFittedText(stream, boldFont, fontSize, column.getHeaderLabel(), x + valueWidth / 2,
                        centeredBaseline(y, rowHeight, fontSize), valueWidth - 8);
                x += valueWidth;
            }
        }
        strokeRect(stream, x, y - rowHeight, finalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, 9, "Final grade", x + finalWidth / 2, baseline, finalWidth - 8);
    }

    private static void drawGradeDocumentRow(
            PDPageContentStream stream,
            PDType1Font bodyFont,
            PDType1Font boldFont,
            float left,
            float y,
            float rowHeight,
            float idWidth,
            float nameWidth,
            float valueWidth,
            float finalWidth,
            GradeDocumentView.RowView row,
            int valueColumnCount
    ) throws IOException {
        float fontSize = rowHeight < 16 ? 7f : 9f;
        float baseline = centeredBaseline(y, rowHeight, fontSize);
        strokeRect(stream, left, y - rowHeight, idWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, bodyFont, fontSize, Long.toString(row.getStudentId()), left + idWidth / 2,
                baseline, idWidth - 4);
        strokeRect(stream, left + idWidth, y - rowHeight, nameWidth, rowHeight, Color.BLACK);
        writeFittedPdfLine(stream, bodyFont, fontSize, row.getStudentName(), left + idWidth + 6, baseline, nameWidth - 10);
        float x = left + idWidth + nameWidth;
        int columns = Math.max(1, valueColumnCount);
        for (int index = 0; index < columns; index++) {
            strokeRect(stream, x, y - rowHeight, valueWidth, rowHeight, Color.BLACK);
            String value = index < row.getValues().size() ? row.getValues().get(index) : "-";
            drawCenteredFittedText(stream, boldFont, fontSize, value, x + valueWidth / 2, baseline, valueWidth - 6);
            x += valueWidth;
        }
        strokeRect(stream, x, y - rowHeight, finalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, fontSize, row.getFinalGrade(), x + finalWidth / 2, baseline,
                finalWidth - 6);
    }

    private static void drawGradeDocumentEmptyRow(
            PDPageContentStream stream,
            PDType1Font bodyFont,
            float left,
            float y,
            float rowHeight,
            float idWidth,
            float nameWidth,
            float valueWidth,
            float finalWidth,
            int valueColumnCount,
            String emptyMessage
    ) throws IOException {
        float totalWidth = idWidth + nameWidth + valueWidth * Math.max(1, valueColumnCount) + finalWidth;
        strokeRect(stream, left, y - rowHeight, totalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, bodyFont, 9, emptyMessage, left + totalWidth / 2,
                centeredBaseline(y, rowHeight, 9), totalWidth - 16);
    }

    private String gradeDocumentExcelHtml(GradeDocumentView view, String imageDataUri) throws IOException {
        int valueColumns = view.getVariableColumnCount();
        int totalColumns = view.getDocumentColumnCount();
        StringBuilder html = new StringBuilder();
        html.append("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:x="urn:schemas-microsoft-com:office:excel"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><style>
                @page Section1{size:11.0in 8.5in;mso-page-orientation:landscape;margin:.25in .20in .25in .20in}
                div.Section1{page:Section1}
                body{font-family:Arial,sans-serif;color:#000;font-size:11pt}
                table{border-collapse:collapse;table-layout:fixed}
                .doc-head{border:1px solid #000;margin-bottom:10px}
                .doc-head td{border:none;padding:8px 10px;vertical-align:middle}
                .posted{font-weight:bold;text-align:left}
                .brand{border-left:2px solid #000;text-align:left;font-weight:bold;padding-left:14pt;white-space:nowrap}
                .logo{text-align:center;width:150pt}
                .logo img{height:42pt;max-width:56pt;object-fit:cover}
                .entity{border-bottom:1px solid #000;font-size:12pt}
                .title{font-size:18pt;font-weight:bold;text-align:center}
                .period{font-weight:bold;text-align:right}
                .sheet th,.sheet td{border:1px solid #000;padding:5px 7px;vertical-align:middle;white-space:nowrap}
                .sheet th{font-size:11pt;font-weight:bold;text-align:center}
                .id{text-align:center}
                .student{text-align:left}
                .grade{text-align:center;font-weight:bold}
                .empty{text-align:center;height:24pt}
                .alert{font-size:9pt;padding-top:8px}
                .signature{text-align:center;padding-top:24pt}
                .line{border-top:1px solid #000;display:block;height:1px;margin:18pt 0 0 auto;width:170pt}
                </style>
                <!--[if gte mso 9]><xml>
                <x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Grade sheet</x:Name>
                <x:WorksheetOptions><x:PageSetup><x:Layout x:Orientation="Landscape"/></x:PageSetup>
                <x:FitToPage/><x:Print><x:FitHeight>1</x:FitHeight><x:FitWidth>1</x:FitWidth></x:Print>
                </x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook>
                </xml><![endif]--></head><body><div class="Section1">
                """);
        List<Integer> sharedColumnWidths = gradeDocumentExcelColumnWidths(view);
        String sharedColGroup = gradeDocumentExcelColGroup(sharedColumnWidths);
        int tableWidthPt = sharedColumnWidths.stream().mapToInt(Integer::intValue).sum();
        html.append("<table class=\"doc-head\" style=\"width:").append(tableWidthPt).append("pt\">")
                .append(sharedColGroup)
                .append("<tr>")
                .append("<td class=\"posted\" colspan=\"2\">Published on: ___ / ___ / _____</td>")
                .append("<td class=\"logo\" colspan=\"").append(Math.max(1, totalColumns - 4)).append("\" rowspan=\"2\">")
                .append("<img src=\"")
                .append(htmlSafe(imageDataUri == null ? placeholderImageDataUri() : imageDataUri))
                .append("\" alt=\"\">")
                .append("</td>")
                .append("<td class=\"brand\" colspan=\"2\" rowspan=\"2\">")
                .append(htmlSafe(documentBrandLine(view))).append("</td>")
                .append("</tr><tr><td colspan=\"2\">&nbsp;</td></tr>")
                .append("<tr><td class=\"entity\" colspan=\"2\"><strong>").append(htmlSafe(view.getEntityLabel()))
                .append(":</strong> ").append(htmlSafe(view.getEntityName())).append("</td>")
                .append("<td class=\"title\" colspan=\"").append(Math.max(1, totalColumns - 4)).append("\">")
                .append(htmlSafe(view.getTitle())).append("</td>")
                .append("<td class=\"period\" colspan=\"2\">Academic period: ").append(htmlSafe(view.getPeriodLabel()))
                .append("</td></tr></table>");
        html.append("<table class=\"sheet\" style=\"width:").append(tableWidthPt).append("pt\">")
                .append(sharedColGroup)
                .append("<tr><th>ID</th><th>Student name</th>");
        if (view.getColumns().isEmpty()) {
            html.append("<th>Avaliacoes</th>");
        } else {
            for (GradeDocumentView.ColumnView column : view.getColumns()) {
                html.append("<th>").append(htmlSafe(column.getHeaderLabel())).append("</th>");
            }
        }
        html.append("<th>Final grade</th></tr>");
        if (view.getRows().isEmpty()) {
            html.append("<tr><td class=\"empty\" colspan=\"").append(totalColumns).append("\">")
                    .append(htmlSafe(view.getEmptyMessage())).append("</td></tr>");
        } else {
            for (GradeDocumentView.RowView row : view.getRows()) {
                html.append("<tr><td class=\"id\">").append(row.getStudentId()).append("</td>")
                        .append("<td class=\"student\">").append(htmlSafe(row.getStudentName())).append("</td>");
                for (int index = 0; index < valueColumns; index++) {
                    String value = index < row.getValues().size() ? row.getValues().get(index) : "-";
                    html.append("<td class=\"grade\">").append(htmlSafe(value)).append("</td>");
                }
                html.append("<td class=\"grade\">").append(htmlSafe(row.getFinalGrade())).append("</td></tr>");
            }
        }
        html.append("</table>");
        if (view.isHasAlert()) {
            html.append("<div class=\"alert\">").append(htmlSafe(view.getAlert())).append("</div>");
        }
        html.append("<div class=\"signature\">Responsible person<span class=\"line\"></span></div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    private static String gradeDocumentExcelColGroup(List<Integer> columnWidths) {
        StringBuilder colGroup = new StringBuilder("<colgroup>");
        for (Integer width : columnWidths) {
            colGroup.append("<col style=\"width:").append(width).append("pt\">");
        }
        colGroup.append("</colgroup>");
        return colGroup.toString();
    }

    private static List<Integer> gradeDocumentExcelColumnWidths(GradeDocumentView view) {
        int valueColumns = view.getVariableColumnCount();
        List<Integer> widths = new ArrayList<>();
        widths.add(38);
        widths.add(gradeDocumentExcelNameColumnWidth(view));
        for (int index = 0; index < valueColumns; index++) {
            String label = index < view.getColumns().size()
                    ? view.getColumns().get(index).getHeaderLabel()
                    : "Avaliacoes";
            widths.add(excelColumnWidthPt(label, valueColumns > 7 ? 72 : 112));
        }
        widths.add(82);
        int totalWidth = widths.stream().mapToInt(Integer::intValue).sum();
        int minimumDocumentWidth = 760;
        if (totalWidth < minimumDocumentWidth) {
            int extraWidth = minimumDocumentWidth - totalWidth;
            int nameExtra = Math.round(extraWidth * 0.45f);
            int valueExtra = Math.round((extraWidth * 0.45f) / valueColumns);
            widths.set(1, widths.get(1) + nameExtra);
            for (int index = 0; index < valueColumns; index++) {
                int valueColumnIndex = 2 + index;
                widths.set(valueColumnIndex, widths.get(valueColumnIndex) + valueExtra);
            }
            int stretchedWidth = widths.stream().mapToInt(Integer::intValue).sum();
            int finalColumnIndex = widths.size() - 1;
            widths.set(finalColumnIndex, widths.get(finalColumnIndex) + minimumDocumentWidth - stretchedWidth);
        }
        return List.copyOf(widths);
    }

    private static int gradeDocumentExcelNameColumnWidth(GradeDocumentView view) {
        int width = excelColumnWidthPt("Student name", 230);
        for (GradeDocumentView.RowView row : view.getRows()) {
            width = Math.max(width, excelColumnWidthPt(row.getStudentName(), 230));
        }
        return width;
    }

    private static int excelColumnWidthPt(String text, int minimumWidth) {
        String safe = text == null || text.isBlank() ? "-" : text.trim();
        return Math.max(minimumWidth, safe.length() * 5 + 24);
    }

    private Path resolveMediaPath(String relativePath) {
        String safe = MediaPathValidator.safeRelativePath(relativePath).orElse(null);
        if (safe == null) {
            return null;
        }
        String realPath = getServletContext() == null ? null : getServletContext().getRealPath("/");
        Path uploadRoot = UploadRootResolver.resolve(
                UploadRootResolver.configuredUploadDirectory("gape.upload.dir", "uploads"),
                realPath
        );
        Path uploadCandidate = uploadRoot.resolve(safe).normalize();
        if (uploadCandidate.startsWith(uploadRoot) && Files.isRegularFile(uploadCandidate)) {
            return uploadCandidate;
        }
        if (realPath != null && !realPath.isBlank()) {
            Path webappRoot = Path.of(realPath).toAbsolutePath().normalize();
            Path webappCandidate = webappRoot.resolve("media").resolve(safe).normalize();
            if (webappCandidate.startsWith(webappRoot) && Files.isRegularFile(webappCandidate)) {
                return webappCandidate;
            }
        }
        return null;
    }

    private String imageDataUri(String relativePath) throws IOException {
        Path path = resolveMediaPath(relativePath);
        if (path == null) {
            return null;
        }
        String mimeType = mimeType(path);
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    private static String placeholderImageDataUri() throws IOException {
        int size = 96;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(238, 242, 247));
            graphics.fillRoundRect(0, 0, size - 1, size - 1, 14, 14);
            graphics.setColor(new Color(216, 224, 234));
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawRoundRect(1, 1, size - 3, size - 3, 14, 14);
            graphics.setColor(new Color(31, 59, 87));
            graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int frameX = 31;
            int frameY = 31;
            int frameW = 34;
            int frameH = 28;
            graphics.drawRoundRect(frameX, frameY, frameW, frameH, 3, 3);
            graphics.drawPolyline(
                    new int[]{frameX + 5, frameX + 15, frameX + 22, frameX + 30},
                    new int[]{frameY + 21, frameY + 12, frameY + 18, frameY + 9},
                    4
            );
            graphics.fillOval(frameX + 23, frameY + 7, 5, 5);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    private static String mimeType(Path path) {
        String filename = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private static byte[] gradeSheetPdf(GradeSheetView sheet) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float left = 26;
                float right = pageWidth - 26;
                float top = pageHeight - 24;
                strokeRect(stream, left, 470, right - left, 92, Color.BLACK);
                writeFittedPdfLine(stream, boldFont, 9, "Posted on: ____ / ____ / ______", left + 6, top - 24, 190);
                drawCenteredFittedText(stream, boldFont, 15, "GAPE", pageWidth / 2, top - 18, 160);
                drawCenteredFittedText(stream, bodyFont, 8, "Academic and Pedagogical Management", pageWidth / 2, top - 31, 190);
                drawCenteredFittedText(stream, boldFont, 9, sheet.getCourseName(), pageWidth / 2, top - 62, 260);
                drawCenteredFittedText(stream, boldFont, 16, "Class group grade sheet", pageWidth / 2, top - 82, 220);
                writeFittedPdfLine(stream, bodyFont, 9, "Subject: " + sheet.getSubjectName(), left + 18, top - 78, 210);
                writeFittedPdfLine(stream, boldFont, 9, "Course: " + sheet.getCourseAcronym(), right - 156, top - 18, 150);
                writeFittedPdfLine(stream, boldFont, 9, "Class group: " + sheet.getClassGroupCode(), right - 156, top - 50, 150);
                writeFittedPdfLine(stream, boldFont, 9, "Scale: 0-" + sheet.getMaxGrade(), right - 156, top - 78, 150);

                float tableTop = 452;
                float tableBottom = 112;
                float headerHeight = 28;
                float numberWidth = 28;
                float finalWidth = 76;
                float tableWidth = right - left;
                int assessmentCount = Math.max(1, sheet.getAssessmentColumns().size());
                float minAssessmentWidth = assessmentCount > 6 ? 42 : 80;
                float nameWidth = Math.min(220, Math.max(160,
                        tableWidth - numberWidth - finalWidth - assessmentCount * minAssessmentWidth));
                float assessmentWidth = (right - left - numberWidth - nameWidth - finalWidth) / assessmentCount;
                int rowCount = Math.max(1, sheet.getStudentRows().size());
                float rowHeight = Math.min(18, Math.max(13, (tableTop - tableBottom - headerHeight) / rowCount));
                drawGradeSheetHeader(stream, boldFont, left, tableTop, headerHeight, numberWidth, nameWidth,
                        assessmentWidth, finalWidth, sheet);
                float y = tableTop - headerHeight;
                if (sheet.getStudentRows().isEmpty()) {
                    drawGradeSheetEmptyRow(stream, bodyFont, left, y, rowHeight, numberWidth, nameWidth,
                            assessmentWidth, finalWidth, assessmentCount);
                } else {
                    for (GradeSheetView.StudentGradeRowView row : sheet.getStudentRows()) {
                        if (y - rowHeight < tableBottom) {
                            break;
                        }
                        drawGradeSheetRow(stream, bodyFont, boldFont, left, y, rowHeight, numberWidth, nameWidth,
                                assessmentWidth, finalWidth, row, sheet.getAssessmentColumns().size());
                        y -= rowHeight;
                    }
                }
                strokeRect(stream, left, 24, right - left, 68, Color.BLACK);
                writePdfLine(stream, boldFont, 8, "NOTES:", left + 6, 78);
                float noteY = 64;
                float notesWidth = 216;
                if (sheet.getAssessmentColumns().isEmpty()) {
                    writeFittedPdfLine(stream, bodyFont, 8, "No assessments configured.", left + 6, noteY, notesWidth);
                } else {
                    for (GradeSheetView.AssessmentColumnView column : sheet.getAssessmentColumns()) {
                        if (noteY < 32) {
                            break;
                        }
                        writeFittedPdfLine(stream, bodyFont, 8,
                                column.getTitle() + " - " + column.getWeightLabel() + "% of the final grade weight",
                                left + 6,
                                noteY,
                                notesWidth);
                        noteY -= 10;
                    }
                }
                writePdfLine(stream, boldFont, 8, "Remarks:", left + 250, 78);
                if (sheet.isHasRemarks()) {
                    float alertY = 64;
                    for (String line : wrapText(sheet.getRemarks(), bodyFont, 8, 240, 3)) {
                        writePdfLine(stream, bodyFont, 8, line, left + 250, alertY);
                        alertY -= 11;
                    }
                }
                drawCenteredFittedText(
                        stream,
                        bodyFont,
                        8,
                        "Lisbon, " + ApplicationDateTimeFormat.date(LocalDate.now(ApplicationClock.system())),
                        right - 130,
                        78,
                        180
                );
                drawCenteredFittedText(stream, boldFont, 8, "Class group teacher", right - 130, 58, 180);
                stream.moveTo(right - 220, 42);
                stream.lineTo(right - 42, 42);
                stream.stroke();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static void drawGradeSheetHeader(
            PDPageContentStream stream,
            PDType1Font boldFont,
            float left,
            float y,
            float rowHeight,
            float numberWidth,
            float nameWidth,
            float assessmentWidth,
            float finalWidth,
            GradeSheetView sheet
    ) throws IOException {
        fillRect(stream, left, y - rowHeight, numberWidth, rowHeight, Color.WHITE);
        fillRect(stream, left + numberWidth, y - rowHeight, nameWidth, rowHeight, Color.WHITE);
        strokeRect(stream, left, y - rowHeight, numberWidth, rowHeight, Color.BLACK);
        strokeRect(stream, left + numberWidth, y - rowHeight, nameWidth, rowHeight, Color.BLACK);
        float baseline = centeredBaseline(y, rowHeight, 8);
        drawCenteredFittedText(stream, boldFont, 8, "N.", left + numberWidth / 2, baseline, numberWidth - 4);
        drawCenteredFittedText(stream, boldFont, 8, "Student name", left + numberWidth + nameWidth / 2, baseline, nameWidth - 8);
        float x = left + numberWidth + nameWidth;
        if (sheet.getAssessmentColumns().isEmpty()) {
            strokeRect(stream, x, y - rowHeight, assessmentWidth, rowHeight, Color.BLACK);
            drawCenteredFittedText(stream, boldFont, 8, "Assessments", x + assessmentWidth / 2, baseline, assessmentWidth - 8);
            x += assessmentWidth;
        } else {
            float fontSize = assessmentWidth < 58 ? 6.5f : 8f;
            for (GradeSheetView.AssessmentColumnView column : sheet.getAssessmentColumns()) {
                strokeRect(stream, x, y - rowHeight, assessmentWidth, rowHeight, Color.BLACK);
                drawCenteredFittedText(stream, boldFont, fontSize, column.getHeaderLabel(),
                        x + assessmentWidth / 2, centeredBaseline(y, rowHeight, fontSize), assessmentWidth - 8);
                x += assessmentWidth;
            }
        }
        strokeRect(stream, x, y - rowHeight, finalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, 8, "Final grade", x + finalWidth / 2, baseline, finalWidth - 8);
    }

    private static void drawGradeSheetRow(
            PDPageContentStream stream,
            PDType1Font bodyFont,
            PDType1Font boldFont,
            float left,
            float y,
            float rowHeight,
            float numberWidth,
            float nameWidth,
            float assessmentWidth,
            float finalWidth,
            GradeSheetView.StudentGradeRowView row,
            int assessmentColumnCount
    ) throws IOException {
        fillRect(stream, left, y - rowHeight, numberWidth, rowHeight, new Color(235, 247, 248));
        fillRect(stream, left + numberWidth, y - rowHeight, nameWidth, rowHeight, new Color(235, 247, 248));
        strokeRect(stream, left, y - rowHeight, numberWidth, rowHeight, Color.BLACK);
        strokeRect(stream, left + numberWidth, y - rowHeight, nameWidth, rowHeight, Color.BLACK);
        float fontSize = rowHeight < 15 ? 6.8f : 8f;
        float baseline = centeredBaseline(y, rowHeight, fontSize);
        drawCenteredFittedText(stream, bodyFont, fontSize, Integer.toString(row.getNumber()), left + numberWidth / 2,
                baseline, numberWidth - 4);
        writeFittedPdfLine(stream, bodyFont, fontSize, row.getStudentName(), left + numberWidth + 6, baseline,
                nameWidth - 10);
        float x = left + numberWidth + nameWidth;
        int columns = Math.max(1, assessmentColumnCount);
        for (int index = 0; index < columns; index++) {
            fillRect(stream, x, y - rowHeight, assessmentWidth, rowHeight, new Color(248, 244, 232));
            strokeRect(stream, x, y - rowHeight, assessmentWidth, rowHeight, Color.BLACK);
            String value = index < row.getAssessmentValues().size() ? row.getAssessmentValues().get(index) : "-";
            drawCenteredFittedText(stream, boldFont, fontSize, value, x + assessmentWidth / 2, baseline,
                    assessmentWidth - 6);
            x += assessmentWidth;
        }
        strokeRect(stream, x, y - rowHeight, finalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, boldFont, fontSize, row.getFinalGrade(), x + finalWidth / 2, baseline,
                finalWidth - 6);
    }

    private static void drawGradeSheetEmptyRow(
            PDPageContentStream stream,
            PDType1Font bodyFont,
            float left,
            float y,
            float rowHeight,
            float numberWidth,
            float nameWidth,
            float assessmentWidth,
            float finalWidth,
            int assessmentCount
    ) throws IOException {
        float totalWidth = numberWidth + nameWidth + assessmentWidth * assessmentCount + finalWidth;
        strokeRect(stream, left, y - rowHeight, totalWidth, rowHeight, Color.BLACK);
        drawCenteredFittedText(stream, bodyFont, 8, "No grade records.", left + totalWidth / 2,
                centeredBaseline(y, rowHeight, 8), totalWidth - 16);
    }

    private static byte[] certificatePdf(CertificateView certificate) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                Color teal = new Color(0, 96, 112);
                fillRect(stream, 28, 648, 28, 150, teal);
                fillRect(stream, 28, 292, 28, 318, teal);
                fillRect(stream, 28, 96, 28, 150, teal);

                float bodyX = 76;
                float bodyW = 470;
                stream.setNonStrokingColor(teal);
                float titleBottom = writeWrappedPdfLines(stream, titleFont, 25,
                        "Course Completion Certificate for " + certificate.getCourseName(),
                        bodyX, 764, bodyW, 30, 3);
                stream.setNonStrokingColor(Color.BLACK);

                float infoTop = Math.min(682, titleBottom - 36);
                writePdfLine(stream, bodyFont, 11, "This is to certify that", bodyX, infoTop);
                writeFittedPdfLine(stream, boldFont, 12, certificate.getStudentName(), bodyX, infoTop - 21, bodyW);
                writeFittedPdfLine(stream, bodyFont, 10,
                        "Civil identification (CC/BI): " + certificate.getStudentEmail(),
                        bodyX, infoTop - 41, bodyW);

                float tableX = 76;
                float tableY = Math.min(560, infoTop - 78);
                float tableW = 470;
                float subjectWidth = 300;
                float ectsWidth = 80;
                float gradeWidth = tableW - subjectWidth - ectsWidth;
                float headerH = 50;
                float finalH = 38;
                int subjectRows = Math.max(1, certificate.getSubjectRows().size());
                float rowH = Math.min(42, Math.max(24, (tableY - 214 - headerH - finalH) / subjectRows));
                strokeRect(stream, tableX, tableY - headerH, tableW, headerH, Color.GRAY);
                strokeRect(stream, tableX + subjectWidth, tableY - headerH, ectsWidth, headerH, Color.GRAY);
                strokeRect(stream, tableX + subjectWidth + ectsWidth, tableY - headerH, gradeWidth, headerH, Color.GRAY);
                drawCenteredFittedText(stream, boldFont, 10,
                        "Subjects",
                        tableX + subjectWidth / 2, tableY - 29, subjectWidth - 18);
                drawCenteredFittedText(stream, boldFont, 10, "ECTS", tableX + subjectWidth + ectsWidth / 2,
                        tableY - 29, ectsWidth - 10);
                drawCenteredFittedText(stream, boldFont, 10, "Grade",
                        tableX + subjectWidth + ectsWidth + gradeWidth / 2, tableY - 22, gradeWidth - 12);
                drawCenteredFittedText(stream, boldFont, 9, "0 - " + certificateScale(certificate),
                        tableX + subjectWidth + ectsWidth + gradeWidth / 2, tableY - 36, gradeWidth - 12);
                float y = tableY - headerH;
                for (CertificateView.CertificateSubjectRowView row : certificate.getSubjectRows()) {
                    if (y - rowH < 204) {
                        break;
                    }
                    strokeRect(stream, tableX, y - rowH, tableW, rowH, Color.LIGHT_GRAY);
                    strokeRect(stream, tableX + subjectWidth, y - rowH, ectsWidth, rowH, Color.LIGHT_GRAY);
                    strokeRect(stream, tableX + subjectWidth + ectsWidth, y - rowH, gradeWidth, rowH, Color.LIGHT_GRAY);
                    float rowFont = rowH < 30 ? 9 : 11;
                    float rowBaseline = centeredBaseline(y, rowH, rowFont);
                    writeFittedPdfLine(stream, bodyFont, rowFont, row.getSubjectLabel(), tableX + 12,
                            rowBaseline, subjectWidth - 20);
                    drawCenteredFittedText(stream, bodyFont, rowFont, row.getEctsLabel(),
                            tableX + subjectWidth + ectsWidth / 2, rowBaseline, ectsWidth - 12);
                    drawCenteredFittedText(stream, bodyFont, rowFont, row.getGradeLabel(),
                            tableX + subjectWidth + ectsWidth + gradeWidth / 2, rowBaseline, gradeWidth - 12);
                    y -= rowH;
                }
                strokeRect(stream, tableX, y - finalH, tableW, finalH, Color.GRAY);
                strokeRect(stream, tableX + subjectWidth + ectsWidth, y - finalH, gradeWidth, finalH, Color.GRAY);
                drawCenteredFittedText(stream, boldFont, 11, "Final Grade", tableX + (subjectWidth + ectsWidth) / 2,
                        centeredBaseline(y, finalH, 11), subjectWidth + ectsWidth - 16);
                drawCenteredFittedText(stream, boldFont, 11, certificate.getFinalGrade() + " points",
                        tableX + subjectWidth + ectsWidth + gradeWidth / 2,
                        centeredBaseline(y, finalH, 11), gradeWidth - 12);
                float afterTableY = y - finalH;
                float statementY = afterTableY - 28;
                String statement = "This certificate confirms successful completion of the course "
                        + certificate.getCourseName() + ", with a total duration of "
                        + certificate.getCourseDurationLabel() + ", " + certificate.getCourseEctsLabel()
                        + " ECTS and " + certificate.getSubjectCountLabel() + ".";
                float statementBottom = writeWrappedPdfLines(stream, bodyFont, 11, statement, 76, statementY, 470, 17, 3);
                float signatureY = Math.max(76, statementBottom - 48);
                writePdfLine(stream, bodyFont, 10, "Responsible person", 76, signatureY);
                stream.moveTo(76, signatureY - 28);
                stream.lineTo(240, signatureY - 28);
                stream.stroke();
                drawCenteredFittedText(stream, bodyFont, 8, "Signature and official seal or stamp",
                        158, signatureY - 42, 164);
                writePdfLine(stream, bodyFont, 10, "Training entity, Ltd.", 390, signatureY);
                stream.moveTo(390, signatureY - 28);
                stream.lineTo(535, signatureY - 28);
                stream.stroke();
                if (!certificate.getValidationCode().isBlank()) {
                    stream.setNonStrokingColor(teal);
                    writeFittedPdfLine(stream, boldFont, 9,
                            "Certificate no. " + certificate.getValidationCode(), 76, 31, 240);
                    stream.setNonStrokingColor(Color.BLACK);
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static String gradeSheetExcelHtml(GradeSheetView sheet) {
        StringBuilder html = new StringBuilder();
        int assessmentColumns = Math.max(1, sheet.getAssessmentColumns().size());
        int totalColumns = sheet.getDocumentColumnCount();
        int centerSpan = Math.max(1, totalColumns - 3);
        html.append("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:x="urn:schemas-microsoft-com:office:excel"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><style>
                @page Section1{size:11.0in 8.5in;mso-page-orientation:landscape;margin:.25in .25in .25in .25in}
                div.Section1{page:Section1}
                body{font-family:Arial,sans-serif;color:#000;font-size:10pt}
                .sheet{border-collapse:collapse;width:auto;table-layout:auto}
                .sheet td,.sheet th{border:1px solid #000;padding:5px;vertical-align:middle;white-space:nowrap}
                .no-border{border:none!important;height:10px}
                .box-top{border-top:2px solid #000!important}
                .box-left{border-left:2px solid #000!important}
                .box-right{border-right:2px solid #000!important}
                .box-bottom{border-bottom:2px solid #000!important}
                .brand{font-size:18pt;font-weight:bold;text-align:center}
                .title{font-size:20pt;font-weight:bold;text-align:center}
                .name{background:#e9f7f7}
                .grade{background:#f8f4e8;text-align:center;font-weight:bold}
                .center{text-align:center}.right{text-align:right}.bold{font-weight:bold}.small{font-size:9pt}
                .notes{height:70px;vertical-align:top}
                </style>
                <!--[if gte mso 9]><xml>
                <x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Grade sheet</x:Name>
                <x:WorksheetOptions><x:PageSetup><x:Layout x:Orientation="Landscape"/></x:PageSetup>
                <x:FitToPage/><x:Print><x:FitHeight>1</x:FitHeight><x:FitWidth>1</x:FitWidth></x:Print>
                </x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook>
                </xml><![endif]--></head><body><div class="Section1">
                """);
        html.append("<table class=\"sheet\">");
        html.append("<col style=\"width:36pt\"><col style=\"width:")
                .append(gradeDocumentExcelNameColumnWidth(sheet.getDocument()))
                .append("pt\">");
        for (int index = 0; index < assessmentColumns; index++) {
            String label = index < sheet.getAssessmentColumns().size()
                    ? sheet.getAssessmentColumns().get(index).getHeaderLabel()
                    : "Assessments";
            html.append("<col style=\"width:")
                    .append(excelColumnWidthPt(label, assessmentColumns == 1 ? 300 : 112))
                    .append("pt\">");
        }
        html.append("<col style=\"width:120pt\">");
        html.append("<tr>")
                .append("<td colspan=\"2\" class=\"box-top box-left bold\">Posted on: ____ / ____ / ______</td>")
                .append("<td colspan=\"").append(centerSpan).append("\" class=\"box-top brand\" align=\"center\" style=\"text-align:center;font-size:18pt;font-weight:bold\">GAPE</td>")
                .append("<td class=\"box-top box-right bold center\" align=\"center\" style=\"text-align:center;white-space:nowrap\">Course: ").append(htmlSafe(sheet.getCourseAcronym())).append("</td>")
                .append("</tr>");
        html.append("<tr>")
                .append("<td colspan=\"2\" class=\"box-left\">&nbsp;</td>")
                .append("<td colspan=\"").append(centerSpan).append("\" class=\"center small\" align=\"center\" style=\"text-align:center\">Academic and Pedagogical Management</td>")
                .append("<td class=\"box-right bold center\" align=\"center\" style=\"text-align:center;white-space:nowrap\">Class group: ").append(htmlSafe(sheet.getClassGroupCode())).append("</td>")
                .append("</tr>");
        html.append("<tr>")
                .append("<td colspan=\"2\" class=\"box-left\">Subject: ").append(htmlSafe(sheet.getSubjectName())).append("</td>")
                .append("<td colspan=\"").append(centerSpan).append("\" class=\"center bold\" align=\"center\" style=\"text-align:center\">").append(htmlSafe(sheet.getCourseName())).append("</td>")
                .append("<td class=\"box-right bold center\" align=\"center\" style=\"text-align:center;white-space:nowrap\">Scale: 0-").append(htmlSafe(sheet.getMaxGrade())).append("</td>")
                .append("</tr>");
        html.append("<tr><td colspan=\"").append(totalColumns)
                .append("\" class=\"box-left box-right box-bottom title\" align=\"center\" style=\"text-align:center;font-size:20pt;font-weight:bold\">Class group grade sheet</td></tr>");
        html.append("<tr><td colspan=\"").append(totalColumns).append("\" class=\"no-border\">&nbsp;</td></tr>");
        html.append("<tr><th>N.</th><th>Student name</th>");
        if (sheet.getAssessmentColumns().isEmpty()) {
            html.append("<th>Assessments</th>");
        } else {
            for (GradeSheetView.AssessmentColumnView column : sheet.getAssessmentColumns()) {
                html.append("<th>").append(htmlSafe(column.getHeaderLabel())).append("</th>");
            }
        }
        html.append("<th>Final grade</th></tr>");
        for (GradeSheetView.StudentGradeRowView row : sheet.getStudentRows()) {
            html.append("<tr><td class=\"name center\">").append(row.getNumber()).append("</td>")
                    .append("<td class=\"name\">").append(htmlSafe(row.getStudentName())).append("</td>");
            int columns = Math.max(1, sheet.getAssessmentColumns().size());
            for (int index = 0; index < columns; index++) {
                String value = index < row.getAssessmentValues().size() ? row.getAssessmentValues().get(index) : "-";
                html.append("<td class=\"grade\">").append(htmlSafe(value)).append("</td>");
            }
            html.append("<td class=\"center bold\">").append(htmlSafe(row.getFinalGrade())).append("</td></tr>");
        }
        html.append("<tr><td colspan=\"").append(totalColumns).append("\" class=\"no-border\">&nbsp;</td></tr>");
        html.append("<tr><td colspan=\"2\" class=\"notes\"><span class=\"bold\">NOTES:</span><br>");
        if (sheet.getAssessmentColumns().isEmpty()) {
            html.append("No assessments configured.<br>");
        } else {
            for (GradeSheetView.AssessmentColumnView column : sheet.getAssessmentColumns()) {
                html.append(htmlSafe(column.getTitle())).append(" - ")
                        .append(htmlSafe(column.getWeightLabel())).append("% of the final grade weight<br>");
            }
        }
        html.append("</td><td colspan=\"").append(centerSpan).append("\" class=\"notes\"><span class=\"bold\">Remarks:</span><br>");
        if (sheet.isHasRemarks()) {
            html.append(htmlSafe(sheet.getRemarks()));
        }
        html.append("</td><td class=\"notes center\">Lisbon, ")
                .append(ApplicationDateTimeFormat.date(LocalDate.now(ApplicationClock.system())))
                .append("<br><br><span class=\"bold\">Class group teacher</span><br><br>____________________</td></tr>");
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private static String certificateExcelHtml(CertificateView certificate) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:x="urn:schemas-microsoft-com:office:excel"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><style>
                @page Section1{size:8.5in 11.0in;margin:.45in .45in .45in .45in}
                div.Section1{page:Section1}
                body{font-family:Arial,sans-serif;color:#000;font-size:12pt}
                .layout{border-collapse:collapse;width:100%;table-layout:fixed}
                .layout td{border:none;padding:6px;vertical-align:top}
                .rail{background:#006070;color:#006070}
                .title{color:#006070;font-size:26pt;font-weight:bold;line-height:1.15}
                .identity{line-height:1.45}
                .cert{border-collapse:collapse;width:100%;table-layout:fixed}
                .cert td,.cert th{border:1px solid #b7b7b7;padding:12px;font-size:12pt;vertical-align:middle;white-space:normal}
                .center{text-align:center}.bold{font-weight:bold}.head{color:#006070;font-weight:bold}
                .spacer{height:12px}.line{border-bottom:1px solid #000}
                </style>
                <!--[if gte mso 9]><xml>
                <x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Certificate</x:Name>
                <x:WorksheetOptions><x:PageSetup><x:Layout x:Orientation="Portrait"/></x:PageSetup>
                <x:FitToPage/><x:Print><x:FitHeight>0</x:FitHeight><x:FitWidth>1</x:FitWidth></x:Print>
                </x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook>
                </xml><![endif]--></head><body><div class="Section1">
                """);
        html.append("<table class=\"layout\">")
                .append("<col style=\"width:34pt\"><col style=\"width:18pt\"><col style=\"width:470pt\">")
                .append("<tr><td class=\"rail\" rowspan=\"8\">&nbsp;</td><td></td><td class=\"title\">Course Completion Certificate for ")
                .append(htmlSafe(certificate.getCourseName())).append("</td></tr>")
                .append("<tr><td></td><td class=\"spacer\">&nbsp;</td></tr>")
                .append("<tr><td></td><td class=\"identity\">This is to certify that <span class=\"bold\">")
                .append(htmlSafe(certificate.getStudentName()))
                .append("</span><br>Civil identification (CC/BI): ").append(htmlSafe(certificate.getStudentEmail()))
                .append("</td></tr>")
                .append("<tr><td></td><td>");
        html.append("<table class=\"cert\"><col style=\"width:59%\"><col style=\"width:17%\"><col style=\"width:24%\"><tr><th class=\"head center\">Subjects</th><th class=\"head center\">ECTS</th><th class=\"head center\">Grade<br>0 - ")
                .append(htmlSafe(certificateScale(certificate))).append("</th></tr>");
        for (CertificateView.CertificateSubjectRowView row : certificate.getSubjectRows()) {
            html.append("<tr><td>").append(htmlSafe(row.getSubjectLabel())).append("</td><td class=\"center\">")
                    .append(htmlSafe(row.getEctsLabel())).append("</td><td class=\"center\">")
                    .append(htmlSafe(row.getGradeLabel())).append("</td></tr>");
        }
        html.append("<tr><td colspan=\"2\" class=\"center bold\">Final Grade</td><td class=\"center bold\">")
                .append(htmlSafe(certificate.getFinalGrade())).append(" points</td></tr></table>");
        html.append("</td></tr>")
                .append("<tr><td></td><td>This certificate confirms successful completion of the course ")
                .append("<span class=\"bold\">")
                .append(htmlSafe(certificate.getCourseName()))
                .append("</span>, with a total duration of <span class=\"bold\">").append(htmlSafe(certificate.getCourseDurationLabel()))
                .append(", ").append(htmlSafe(certificate.getCourseEctsLabel()))
                .append(" ECTS and ").append(htmlSafe(certificate.getSubjectCountLabel())).append("</span>.</td></tr>")
                .append("<tr><td></td><td>Responsible person ____________________<br>Signature and official seal or stamp</td></tr>")
                .append("<tr><td></td><td>Training entity, Ltd. ____________________</td></tr>");
        if (!certificate.getValidationCode().isBlank()) {
            html.append("<tr><td></td><td class=\"head\">Certificate no. ")
                    .append(htmlSafe(certificate.getValidationCode())).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private static void fillRect(
            PDPageContentStream stream,
            float x,
            float y,
            float width,
            float height,
            Color color
    ) throws IOException {
        stream.setNonStrokingColor(color);
        stream.addRect(x, y, width, height);
        stream.fill();
        stream.setNonStrokingColor(Color.BLACK);
    }

    private static void strokeRect(
            PDPageContentStream stream,
            float x,
            float y,
            float width,
            float height,
            Color color
    ) throws IOException {
        stream.setStrokingColor(color);
        stream.addRect(x, y, width, height);
        stream.stroke();
        stream.setStrokingColor(Color.BLACK);
    }

    private static float centeredBaseline(float topY, float rowHeight, float fontSize) {
        return topY - rowHeight / 2 - fontSize / 3;
    }

    private static void writeFittedPdfLine(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            String text,
            float x,
            float y,
            float maxWidth
    ) throws IOException {
        writePdfLine(stream, font, fontSize, fitPdfText(font, fontSize, text, maxWidth), x, y);
    }

    private static void drawCenteredFittedText(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            String text,
            float centerX,
            float y,
            float maxWidth
    ) throws IOException {
        String safe = fitPdfText(font, fontSize, text, maxWidth);
        float width = pdfTextWidth(font, fontSize, safe);
        writePdfLine(stream, font, fontSize, safe, centerX - width / 2, y);
    }

    private static float writeWrappedPdfLines(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            String text,
            float x,
            float y,
            float maxWidth,
            float lineHeight,
            int maxLines
    ) throws IOException {
        List<String> lines = wrapText(text, font, fontSize, maxWidth, maxLines);
        float currentY = y;
        for (String line : lines) {
            writePdfLine(stream, font, fontSize, line, x, currentY);
            currentY -= lineHeight;
        }
        return currentY;
    }

    private static List<String> wrapText(
            String text,
            PDType1Font font,
            float fontSize,
            float maxWidth,
            int maxLines
    ) throws IOException {
        String safe = pdfSafe(text);
        if (safe.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] words = safe.split("\\s+");
        boolean truncated = false;
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (pdfTextWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            current.setLength(0);
            current.append(fitPdfText(font, fontSize, word, maxWidth));
            if (lines.size() == maxLines) {
                truncated = true;
                current.setLength(0);
                break;
            }
            if (lines.size() == maxLines - 1) {
                StringBuilder lastLine = new StringBuilder(current);
                while (index + 1 < words.length) {
                    String nextCandidate = lastLine + " " + words[index + 1];
                    if (pdfTextWidth(font, fontSize, nextCandidate) > maxWidth) {
                        truncated = true;
                        break;
                    }
                    lastLine.setLength(0);
                    lastLine.append(nextCandidate);
                    index++;
                }
                current.setLength(0);
                current.append(lastLine);
                break;
            }
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        if (truncated && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, fitPdfText(font, fontSize, lines.get(last) + "...", maxWidth));
        } else if (lines.size() == maxLines && pdfTextWidth(font, fontSize, lines.get(lines.size() - 1)) > maxWidth) {
            lines.set(lines.size() - 1, fitPdfText(font, fontSize, lines.get(lines.size() - 1), maxWidth));
        }
        return lines;
    }

    private static String fitPdfText(PDType1Font font, float fontSize, String text, float maxWidth) throws IOException {
        String safe = pdfSafe(text);
        if (pdfTextWidth(font, fontSize, safe) <= maxWidth) {
            return safe;
        }
        String ellipsis = "...";
        int length = safe.length();
        while (length > 0) {
            String candidate = safe.substring(0, length).stripTrailing() + ellipsis;
            if (pdfTextWidth(font, fontSize, candidate) <= maxWidth) {
                return candidate;
            }
            length--;
        }
        return ellipsis;
    }

    private static float pdfTextWidth(PDType1Font font, float fontSize, String text) throws IOException {
        return font.getStringWidth(pdfSafe(text)) / 1000 * fontSize;
    }

    private static void drawCenteredText(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            String text,
            float centerX,
            float y
    ) throws IOException {
        String safe = pdfSafe(text);
        float width = font.getStringWidth(safe) / 1000 * fontSize;
        writePdfLine(stream, font, fontSize, safe, centerX - width / 2, y);
    }

    private static void writePdfLine(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            String text,
            float x,
            float y
    ) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(pdfSafe(text));
        stream.endText();
    }

    private static String pdfSafe(String value) {
        String safe = value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        if (safe.length() > 110) {
            safe = safe.substring(0, 107) + "...";
        }
        return safe.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
    }

    private static String excelCell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value.replace("\t", " ").replace("\r", " ").replace("\n", " ");
        return safe.contains("\"") ? safe.replace("\"", "'") : safe;
    }

    private static String certificateScale(CertificateView certificate) {
        String scale = certificate.getCourseCertificateMaxGradeLabel();
        return scale == null || scale.isBlank() || "-".equals(scale) ? "20" : scale;
    }

    private static String htmlSafe(String value) {
        return GradeSheetView.escapeHtml(value);
    }

    private static boolean certificateComplete(Certificate certificate) {
        return certificate.state() == CertificateState.ISSUED
                && certificate.validationCode() != null
                && !certificate.validationCode().isBlank()
                && certificate.issuedAt() != null
                && certificate.finalGrade() != null;
    }

    private static String subjectLabel(Subject subject) {
        if (subject == null) {
            return null;
        }
        return subject.acronym() == null || subject.acronym().isBlank()
                ? subject.name()
                : subject.acronym() + " - " + subject.name();
    }

    private String courseLabel(Course course) {
        if (course == null) {
            return null;
        }
        return course.acronym() == null || course.acronym().isBlank()
                ? course.name()
                : course.acronym() + " - " + course.name();
    }

    private static <T> Map<Long, T> mapById(List<T> values, IdExtractor<T> extractor) {
        Map<Long, T> map = new LinkedHashMap<>();
        for (T value : values) {
            map.put(extractor.id(value), value);
        }
        return map;
    }

    @FunctionalInterface
    private interface IdExtractor<T> {
        long id(T value);
    }

    private record GradeSheetStudentSeed(long studentUserId, String studentName) {
    }

    private record SubjectOccurrenceKey(long subjectId, long courseOccurrenceId) {
    }

    public static final class GradeSheetCourseGroupView {

        private final List<GradeSheetSubjectGroupView> subjects;
        private final List<GradeSheetCourseOccurrenceGroupView> occurrences;

        private GradeSheetCourseGroupView(List<GradeSheetSubjectGroupView> subjects) {
            this.subjects = subjects;
            Map<Long, List<GradeSheetSubjectGroupView>> byOccurrence = new LinkedHashMap<>();
            subjects.stream()
                    .sorted(Comparator
                            .comparing(GradeSheetSubjectGroupView::getOccurrenceLabel, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(GradeSheetSubjectGroupView::getSubjectLabel, String.CASE_INSENSITIVE_ORDER))
                    .forEach(subject -> byOccurrence
                            .computeIfAbsent(subject.getOccurrenceId(), ignored -> new ArrayList<>())
                            .add(subject));
            this.occurrences = byOccurrence.values().stream()
                    .map(GradeSheetCourseOccurrenceGroupView::new)
                    .toList();
        }

        public long getCourseId() {
            return primarySheet().getCourseId();
        }

        public String getCourseLabel() {
            return primarySheet().getCourseLabel();
        }

        public String getCourseName() {
            return primarySheet().getCourseName();
        }

        public String getCourseAcronym() {
            return primarySheet().getCourseAcronym();
        }

        public String getCoursePhoto() {
            return primarySheet().getCoursePhoto();
        }

        public BigDecimal getCourseCertificateMaxGradeValue() {
            return primarySheet().getCourseCertificateMaxGradeValue();
        }

        public String getCourseContextHtml() {
            return primarySheet().getCourseContextHtml();
        }

        public String getCourseContextTitle() {
            return primarySheet().getCourseContextTitle();
        }

        public long getOrganizationId() {
            return primarySheet().getOrganizationId();
        }

        public String getOrganizationLabel() {
            return primarySheet().getOrganizationLabel();
        }

        public Long getOrganicUnitId() {
            return primarySheet().getOrganicUnitId();
        }

        public String getOrganicUnitLabel() {
            return primarySheet().getOrganicUnitLabel();
        }

        public Long getPrimaryClassGroupId() {
            return primarySheet().getPrimaryClassGroupId();
        }

        public String getPeriodLabel() {
            return primarySheet().getPeriodLabel();
        }

        public int getSubjectCount() {
            return subjects.size();
        }

        public int getSheetCount() {
            int count = 0;
            for (GradeSheetSubjectGroupView subject : subjects) {
                count += subject.getSheetCount();
            }
            return count;
        }

        public List<GradeSheetSubjectGroupView> getSubjects() {
            return subjects;
        }

        /** Subject grade sheets grouped by their course occurrence. */
        public List<GradeSheetCourseOccurrenceGroupView> getOccurrences() {
            return occurrences;
        }

        public List<GradeSheetCourseOccurrenceGroupView> getActiveOccurrences() {
            return occurrences.stream()
                    .filter(occurrence -> !occurrence.isCompleted())
                    .toList();
        }

        public List<GradeSheetCourseOccurrenceGroupView> getCompletedOccurrences() {
            return occurrences.stream()
                    .filter(GradeSheetCourseOccurrenceGroupView::isCompleted)
                    .toList();
        }

        public List<GradeSheetSubjectGroupView> getActiveSubjects() {
            return subjects.stream()
                    .filter(subject -> !subject.isOccurrenceCompleted())
                    .toList();
        }

        public List<GradeSheetSubjectGroupView> getCompletedSubjects() {
            return subjects.stream()
                    .filter(GradeSheetSubjectGroupView::isOccurrenceCompleted)
                    .toList();
        }

        public GradeSheetView getPrimarySheet() {
            return primarySheet();
        }

        public String getStateLabel() {
            for (GradeSheetSubjectGroupView subject : subjects) {
                if ("Draft".equals(subject.getStateLabel())) {
                    return "Draft";
                }
            }
            return subjects.stream().anyMatch(subject -> "Published".equals(subject.getStateLabel()))
                    ? "Published"
                    : primarySheet().getStateLabel();
        }

        public String getStateBadgeClass() {
            for (GradeSheetSubjectGroupView subject : subjects) {
                if ("Draft".equals(subject.getStateLabel())) {
                    return "bg-warning-50 text-warning-600";
                }
            }
            return subjects.stream().anyMatch(subject -> "Published".equals(subject.getStateLabel()))
                    ? "bg-success-50 text-success-600"
                    : primarySheet().getStateBadgeClass();
        }

        public boolean isPublished() {
            return "Published".equals(getStateLabel());
        }

        private GradeSheetView primarySheet() {
            return subjects.get(0).getSheets().get(0);
        }
    }

    /** A visual grouping layer for one course occurrence inside a course. */
    public static final class GradeSheetCourseOccurrenceGroupView {

        private final List<GradeSheetSubjectGroupView> subjects;

        private GradeSheetCourseOccurrenceGroupView(List<GradeSheetSubjectGroupView> subjects) {
            this.subjects = List.copyOf(subjects);
        }

        public long getOccurrenceId() {
            return primarySubject().getOccurrenceId();
        }

        public String getOccurrenceLabel() {
            return primarySubject().getOccurrenceLabel();
        }

        public String getOccurrenceDateRangeLabel() {
            return primarySubject().getOccurrenceDateRangeLabel();
        }

        public String getOccurrenceStateLabel() {
            return primarySubject().getPrimarySheet().getCourseOccurrenceStateValue();
        }

        public boolean isCompleted() {
            return primarySubject().isOccurrenceCompleted();
        }

        public int getSubjectCount() {
            return subjects.size();
        }

        public int getSheetCount() {
            return subjects.stream().mapToInt(GradeSheetSubjectGroupView::getSheetCount).sum();
        }

        public List<GradeSheetSubjectGroupView> getSubjects() {
            return subjects;
        }

        private GradeSheetSubjectGroupView primarySubject() {
            return subjects.get(0);
        }
    }

    public static final class GradeSheetSubjectGroupView {

        private final List<GradeSheetView> sheets;

        private GradeSheetSubjectGroupView(List<GradeSheetView> sheets) {
            this.sheets = sheets;
        }

        public long getSubjectId() {
            return primarySheet().getSubjectId();
        }

        public String getSubjectLabel() {
            return primarySheet().getSubjectLabel();
        }

        public String getSubjectName() {
            return primarySheet().getSubjectName();
        }

        public String getSubjectAcronym() {
            return primarySheet().getSubjectAcronym();
        }

        public String getCourseName() {
            return primarySheet().getCourseName();
        }

        public long getCourseId() {
            return primarySheet().getCourseId();
        }

        public String getCourseLabel() {
            return primarySheet().getCourseLabel();
        }

        public long getOccurrenceId() {
            return primarySheet().getCourseOccurrenceId();
        }

        public String getOccurrenceLabel() {
            return primarySheet().getCourseOccurrenceLabel();
        }

        public String getOccurrenceDateRangeLabel() {
            return primarySheet().getCourseOccurrenceDateRangeLabel();
        }

        public long getNewestSheetId() {
            return sheets.stream()
                    .mapToLong(GradeSheetView::getId)
                    .max()
                    .orElse(0L);
        }

        public boolean isClosed() {
            List<GradeSheetView> classGroupSheets = getClassGroupSheets();
            if (!classGroupSheets.isEmpty()) {
                return classGroupSheets.stream().allMatch(GradeSheetView::isPrimaryClassGroupCompleted);
            }
            return primarySheet().isCourseOccurrenceCompleted()
                    || "closed".equals(primarySheet().getStateValue());
        }

        public String getSubjectPhoto() {
            return primarySheet().getSubjectPhoto();
        }

        public String getSubjectEcts() {
            return primarySheet().getSubjectEcts();
        }

        public BigDecimal getSubjectEctsValue() {
            return primarySheet().getSubjectEctsValue();
        }

        public String getSubjectContextHtml() {
            return primarySheet().getSubjectContextHtml();
        }

        public String getSubjectContextTitle() {
            return primarySheet().getSubjectContextTitle();
        }

        public String getPeriodLabel() {
            return primarySheet().getPeriodLabel();
        }

        public String getWeightAlert() {
            if (hasSubjectGradeSheet()) {
                return primarySheet().isHasWeightAlert() ? primarySheet().getWeightAlert() : "";
            }
            for (GradeSheetView sheet : sheets) {
                if (sheet.isHasWeightAlert()) {
                    return sheet.getWeightAlert();
                }
            }
            return "";
        }

        public String getRemarks() {
            if (hasSubjectGradeSheet()) {
                return primarySheet().getRemarks();
            }
            for (GradeSheetView sheet : sheets) {
                if (sheet.isHasRemarks()) {
                    return sheet.getRemarks();
                }
            }
            return "";
        }

        public GradeDocumentView getSubjectDocument() {
            return subjectDocument(this);
        }

        public int getSheetCount() {
            return getClassGroupSheets().size();
        }

        public String getStateLabel() {
            if (hasSubjectGradeSheet()) {
                return primarySheet().getStateLabel();
            }
            boolean hasDraft = false;
            boolean hasPublished = false;
            for (GradeSheetView sheet : sheets) {
                hasDraft = hasDraft || sheet.isDraft();
                hasPublished = hasPublished || sheet.isPublished();
            }
            if (hasDraft) {
                return "Draft";
            }
            return hasPublished ? "Published" : primarySheet().getStateLabel();
        }

        public String getStateBadgeClass() {
            if (hasSubjectGradeSheet()) {
                return primarySheet().getStateBadgeClass();
            }
            boolean hasDraft = false;
            boolean hasPublished = false;
            for (GradeSheetView sheet : sheets) {
                hasDraft = hasDraft || sheet.isDraft();
                hasPublished = hasPublished || sheet.isPublished();
            }
            if (hasDraft) {
                return "bg-warning-50 text-warning-600";
            }
            return hasPublished ? "bg-success-50 text-success-600" : primarySheet().getStateBadgeClass();
        }

        public boolean isPublished() {
            return "Published".equals(getStateLabel());
        }

        public List<GradeSheetView> getSheets() {
            return sheets;
        }

        public List<GradeSheetView> getClassGroupSheets() {
            return sheets.stream()
                    // The consolidated subject-occurrence sheet is derived
                    // from the single final sheet of each class group.  Other
                    // valid class-group sheets (for example a supplementary
                    // exam sheet) must not inflate this occurrence's source
                    // count or affect its completed grouping.
                    .filter(sheet -> !sheet.getClassGroupIds().isEmpty()
                            && "final".equals(sheet.getTypeValue()))
                    .toList();
        }

        public List<GradeSheetView> getActiveClassGroupSheets() {
            return getClassGroupSheets().stream()
                    .filter(sheet -> !isCompletedClassGroupSheet(sheet))
                    .toList();
        }

        public List<GradeSheetView> getCompletedClassGroupSheets() {
            return getClassGroupSheets().stream()
                    .filter(this::isCompletedClassGroupSheet)
                    .toList();
        }

        private boolean isCompletedClassGroupSheet(GradeSheetView sheet) {
            return sheet.isPrimaryClassGroupCompleted()
                    || "closed".equals(sheet.getStateValue())
                    || "inactive".equals(sheet.getStateValue());
        }

        public boolean isOccurrenceCompleted() {
            return primarySheet().isCourseOccurrenceCompleted();
        }

        public GradeSheetView getPrimarySheet() {
            return primarySheet();
        }

        private GradeSheetView primarySheet() {
            return sheets.stream()
                    .filter(sheet -> sheet.getClassGroupIds().isEmpty())
                    .findFirst()
                    .orElse(sheets.get(0));
        }

        private boolean hasSubjectGradeSheet() {
            return sheets.stream().anyMatch(sheet -> sheet.getClassGroupIds().isEmpty());
        }
    }

    public static final class CertificateStudentGroupView {

        private final List<CertificateView> certificates;
        private final List<CertificateStateSummaryView> stateSummaries;

        private CertificateStudentGroupView(List<CertificateView> certificates) {
            this.certificates = certificates;
            this.stateSummaries = certificateStateSummaries(certificates);
        }

        public long getStudentUserId() {
            return primaryCertificate().getStudentUserId();
        }

        public String getStudentLabel() {
            return getStudentUserId() + " - " + getStudentName();
        }

        public String getStudentName() {
            return primaryCertificate().getStudentName();
        }

        public String getStudentEmail() {
            return primaryCertificate().getStudentEmail();
        }

        public int getCertificateCount() {
            return certificates.size();
        }

        public List<CertificateStateSummaryView> getStateSummaries() {
            return stateSummaries;
        }

        public CertificateView getPrimaryCertificate() {
            return primaryCertificate();
        }

        public List<CertificateView> getCertificates() {
            return certificates;
        }

        /** A student is published only when every certificate in this group is issued. */
        public boolean isPublished() {
            return !certificates.isEmpty() && certificates.stream().allMatch(CertificateView::isCompleted);
        }

        private CertificateView primaryCertificate() {
            return certificates.get(certificates.size() - 1);
        }
    }

    public static final class CertificateStateSummaryView {

        private final int count;
        private final String label;
        private final String badgeClass;

        private CertificateStateSummaryView(int count, String label, String badgeClass) {
            this.count = count;
            this.label = label;
            this.badgeClass = badgeClass;
        }

        public int getCount() {
            return count;
        }

        public String getLabel() {
            return label;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }

    private static List<CertificateStateSummaryView> certificateStateSummaries(List<CertificateView> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return List.of();
        }
        List<CertificateStateSummaryView> summaries = new ArrayList<>();
        for (CertificateState state : CertificateState.values()) {
            String value = state.toDatabaseValue();
            int count = 0;
            String badgeClass = null;
            for (CertificateView certificate : certificates) {
                if (value.equals(certificate.getStateValue())) {
                    count++;
                    badgeClass = certificate.getStatusBadgeClass();
                }
            }
            if (count > 0) {
                summaries.add(new CertificateStateSummaryView(count, certificateStateLabel(state), badgeClass));
            }
        }
        return List.copyOf(summaries);
    }

    private static String certificateStateLabel(CertificateState state) {
        return switch (state) {
            case ISSUED -> "Published";
            case ACTIVE -> "Active";
            case DRAFT -> "Draft";
        };
    }
}
