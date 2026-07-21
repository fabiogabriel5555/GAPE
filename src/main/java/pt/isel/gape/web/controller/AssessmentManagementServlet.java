package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentEnrollmentCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.CorrectionResult;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.ManualCorrectionCommand;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionUpdateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.QuestionUpdateCommand;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.service.AssessmentEnrollmentService;
import pt.isel.gape.learning.service.AssessmentPdfService;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.CorrectionService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.learning.service.QuestionOptionService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.AssessmentEnrollmentView;
import pt.isel.gape.web.view.AssessmentFormData;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.AttemptView;
import pt.isel.gape.web.view.ResponseView;
import pt.isel.gape.web.view.SelectOptionView;
import pt.isel.gape.web.view.UserOptionView;

@WebServlet(name = "assessmentManagementServlet", urlPatterns = {
        "/learning/assessments",
        "/learning/assessments/*"
})
public final class AssessmentManagementServlet extends DashboardServletSupport {

    private static final String ASSESSMENT_LIST_JSP = "/WEB-INF/views/learning/assessment-list.jsp";
    private static final String ASSESSMENT_FORM_JSP = "/WEB-INF/views/learning/assessment-form.jsp";
    private static final String ASSESSMENT_DETAIL_JSP = "/WEB-INF/views/learning/assessment-builder.jsp";
    private static final String ADMIN_ASSESSMENT_LIST_JSP = "/admin/admin/assessment/admin-assessments.jsp";
    private static final String ADMIN_ASSESSMENT_FORM_JSP = "/admin/admin/assessment/admin-assessment-form.jsp";
    private static final String ADMIN_ASSESSMENT_DETAIL_JSP = "/admin/admin/assessment/admin-assessment-builder.jsp";
    private static final String COORDINATOR_ASSESSMENT_LIST_JSP = "/coordinator/coordinator/assessment/coordinator-assessments.jsp";
    private static final String COORDINATOR_ASSESSMENT_FORM_JSP = "/coordinator/coordinator/assessment/coordinator-assessment-form.jsp";
    private static final String COORDINATOR_ASSESSMENT_DETAIL_JSP = "/coordinator/coordinator/assessment/coordinator-assessment-builder.jsp";
    private static final String INSTRUCTOR_ASSESSMENT_LIST_JSP = "/instructor/instructor/assessment/instructor-assessments.jsp";
    private static final String INSTRUCTOR_ASSESSMENT_FORM_JSP = "/instructor/instructor/assessment/instructor-assessment-form.jsp";
    private static final String INSTRUCTOR_ASSESSMENT_DETAIL_JSP = "/instructor/instructor/assessment/instructor-assessment-builder.jsp";

    private final AssessmentService assessmentService;
    private final AssessmentEnrollmentService assessmentEnrollmentService;
    private final AssessmentPdfService assessmentPdfService;
    private final QuestionService questionService;
    private final QuestionOptionService optionService;
    private final CorrectionService correctionService;
    private final ApplicationReadService.Assessments assessmentDAO;
    private final ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO;
    private final ApplicationReadService.Questions questionDAO;
    private final ApplicationReadService.QuestionOptions optionDAO;
    private final ApplicationReadService.Attempts attemptDAO;
    private final ApplicationReadService.Responses responseDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.ContentBlocks contentBlockDAO;
    private final ApplicationReadService.ContentItems contentItemDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.PhysicalRooms physicalRoomDAO;
    private final ApplicationReadService.Organizations organizationDAO;
    private final ApplicationReadService.OrganicUnits organicUnitDAO;
    private final ApplicationReadService.Users userDAO;
    private final PdfUploadService pdfUploadService;
    private final AssessmentViewFactory viewFactory;

    public AssessmentManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private AssessmentManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new AssessmentService(connectionProvider, clock),
                new AssessmentEnrollmentService(connectionProvider, clock),
                new AssessmentPdfService(connectionProvider),
                new QuestionService(connectionProvider, clock),
                new QuestionOptionService(connectionProvider, clock),
                new CorrectionService(connectionProvider, clock),
                new PdfUploadService()
        );
    }

    AssessmentManagementServlet(
            ApplicationReadService readService,
            AssessmentService assessmentService,
            AssessmentEnrollmentService assessmentEnrollmentService,
            AssessmentPdfService assessmentPdfService,
            QuestionService questionService,
            QuestionOptionService optionService,
            CorrectionService correctionService,
            PdfUploadService pdfUploadService
    ) {
        this.assessmentService = assessmentService;
        this.assessmentEnrollmentService = assessmentEnrollmentService;
        this.assessmentPdfService = assessmentPdfService;
        this.questionService = questionService;
        this.optionService = optionService;
        this.correctionService = correctionService;
        this.assessmentDAO = readService.assessments();
        this.assessmentEnrollmentDAO = readService.assessmentEnrollments();
        this.questionDAO = readService.questions();
        this.optionDAO = readService.questionOptions();
        this.attemptDAO = readService.attempts();
        this.responseDAO = readService.responses();
        this.subjectDAO = readService.subjects();
        this.contentBlockDAO = readService.contentBlocks();
        this.contentItemDAO = readService.contentItems();
        this.classGroupDAO = readService.classGroups();
        this.courseDAO = readService.courses();
        this.courseSubjectDAO = readService.courseSubjects();
        this.physicalRoomDAO = readService.physicalRooms();
        this.organizationDAO = readService.organizations();
        this.organicUnitDAO = readService.organicUnits();
        this.userDAO = readService.users();
        this.pdfUploadService = pdfUploadService;
        this.viewFactory = new AssessmentViewFactory(
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 0) {
                redirect(request, response, "/learning/lessons#assessments");
                return;
            }
            if (segments.length == 1 && "new".equals(segments[0])) {
                showForm(request, response, AssessmentFormData.blank(
                        optionalLong(request, "subjectId"),
                        optionalLong(request, "contentBlockId"),
                        text(request, "type"),
                        text(request, "mode"),
                        text(request, "correctionMode")
                ), true, null);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            if (segments.length == 2 && "edit".equals(segments[1])) {
                showEditForm(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            if (segments.length == 2 && "pdf".equals(segments[1])) {
                downloadAssessmentPdf(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 4 && "attempts".equals(segments[1]) && "pdf".equals(segments[3])) {
                downloadAttemptPdf(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2])
                );
                return;
            }
            if (segments.length == 2 && "attempts".equals(segments[1])) {
                redirect(request, response, "/learning/assessments/" + Long.parseLong(segments[0]) + "#attempts");
                return;
            }
            if (segments.length == 3 && "attempts".equals(segments[1])) {
                redirect(request, response, "/learning/assessments/" + Long.parseLong(segments[0]) + "#attempts");
                return;
            }
            if (segments.length == 6
                    && "attempts".equals(segments[1])
                    && "responses".equals(segments[3])
                    && "attachment".equals(segments[5])) {
                downloadResponseAttachment(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2]),
                        Long.parseLong(segments[4])
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
                createAssessment(request, response);
                return;
            }
            if (segments.length == 1 && "repository-copy".equals(segments[0])) {
                copyRepositoryAssessment(request, response);
                return;
            }
            if (segments.length == 1) {
                updateAssessment(request, response, Long.parseLong(segments[0]));
                return;
            }
            long assessmentId = Long.parseLong(segments[0]);
            if (segments.length == 2) {
                switch (segments[1]) {
                    case "archive" -> archiveAssessment(request, response, assessmentId);
                    case "delete" -> deleteAssessment(request, response, assessmentId);
                    case "questions" -> createQuestion(request, response, assessmentId);
                    case "enrollment-policy" -> updateEnrollmentPolicy(request, response, assessmentId);
                    case "enrollments" -> enrollStudent(request, response, assessmentId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1])) {
                long studentUserId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "approve" -> approveEnrollment(request, response, assessmentId, studentUserId);
                    case "reject" -> rejectEnrollment(request, response, assessmentId, studentUserId);
                    case "withdraw" -> withdrawEnrollment(request, response, assessmentId, studentUserId);
                    case "delete" -> deleteEnrollment(request, response, assessmentId, studentUserId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 3 && "questions".equals(segments[1]) && "rebalance".equals(segments[2])) {
                rebalanceQuestionScores(request, response, assessmentId);
                return;
            }
            if (segments.length == 3 && "questions".equals(segments[1]) && "reorder".equals(segments[2])) {
                reorderQuestions(request, response, assessmentId);
                return;
            }
            if (segments.length == 3 && "questions".equals(segments[1])) {
                updateQuestion(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "questions".equals(segments[1]) && "archive".equals(segments[3])) {
                archiveQuestion(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "questions".equals(segments[1]) && "delete".equals(segments[3])) {
                deleteQuestion(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "questions".equals(segments[1]) && "options".equals(segments[3])) {
                createOption(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4
                    && "attempts".equals(segments[1])
                    && "auto-correct".equals(segments[3])) {
                autoCorrectAttempt(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4
                    && "attempts".equals(segments[1])
                    && "auto-correct-eligible".equals(segments[3])) {
                autoCorrectEligibleResponses(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4
                    && "attempts".equals(segments[1])
                    && "manual-correct".equals(segments[3])) {
                manualCorrectAttempt(request, response, assessmentId, Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 6
                    && "attempts".equals(segments[1])
                    && "responses".equals(segments[3])
                    && "auto-correct".equals(segments[5])) {
                autoCorrectResponse(
                        request,
                        response,
                        assessmentId,
                        Long.parseLong(segments[2]),
                        Long.parseLong(segments[4])
                );
                return;
            }
            if (segments.length == 5
                    && "questions".equals(segments[1])
                    && "options".equals(segments[3])) {
                updateOption(request, response, assessmentId, Long.parseLong(segments[2]), Long.parseLong(segments[4]));
                return;
            }
            if (segments.length == 6
                    && "questions".equals(segments[1])
                    && "options".equals(segments[3])
                    && "archive".equals(segments[5])) {
                archiveOption(request, response, assessmentId, Long.parseLong(segments[2]), Long.parseLong(segments[4]));
                return;
            }
            if (segments.length == 5
                    && "attempts".equals(segments[1])
                    && "responses".equals(segments[3])) {
                correctResponse(request, response, assessmentId, Long.parseLong(segments[2]), Long.parseLong(segments[4]));
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
        try {
            assessmentService.synchronizeTemporalStates();
            List<AssessmentView> assessments = viewFactory.assessmentViews(
                    assessmentDAO.findAll().stream()
                            .filter(assessment -> assessmentService.canManageAssessment(
                                    actor.userId(),
                                    currentSessionId(request),
                                    primaryProfile(actor),
                                    assessment.id(),
                                    request.getRemoteAddr()
                            ))
                            .toList()
            );
            request.setAttribute("assessments", assessments);
            request.setAttribute("assessmentCount", assessments.size());
            request.setAttribute("formCount", assessments.stream().filter(AssessmentView::isForm).count());
            request.setAttribute("testCount", assessments.stream().filter(AssessmentView::isTest).count());
            request.setAttribute("examCount", assessments.stream().filter(AssessmentView::isExam).count());
            request.setAttribute("pendingCorrectionCount", attemptDAO.countByAssessmentIdsAndState(
                    assessments.stream().map(AssessmentView::getId).toList(),
                    AttemptState.SUBMITTED
            ).values().stream().mapToInt(Integer::intValue).sum());
            prepareDashboard(request, "assessments", "Assessments", "/learning/assessments/new", "New Assessment");
            forward(request, response, assessmentListJsp(request));
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessments", exception);
        }
    }

    private void showForm(
            HttpServletRequest request,
            HttpServletResponse response,
            AssessmentFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareForm(request, form, creating, error);
        forward(request, response, assessmentFormJsp(request));
    }

    private void showEditForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            String error
    ) throws ServletException, IOException {
        Assessment assessment = assessmentService.getAssessment(assessmentId);
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        AssessmentFormData form = error == null
                ? assessmentFormData(assessment)
                : AssessmentFormData.from(request, assessmentId);
        prepareAssessmentContext(request, viewFactory.assessmentView(assessment), "edit");
        showForm(request, response, form, false, error);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long assessmentId, String error)
            throws ServletException, IOException {
        showDetail(request, response, assessmentId, error, null);
    }

    private void showDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            String error,
            String forcedLazyPanel
    ) throws ServletException, IOException {
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Assessment assessment = assessmentService.getAssessment(assessmentId);
        markLearningEventsReadForCurrentUser(request, "/learning/assessments/" + assessment.id(), true);
        AssessmentView assessmentView = viewFactory.assessmentView(assessment);
        synchronizeAutomaticAssessmentEnrollments(assessmentView);
        String lazyPanel = forcedLazyPanel == null ? requestedAssessmentLazyPanel(request) : forcedLazyPanel;
        boolean enrollmentsLoaded = "enrollments".equals(lazyPanel);
        boolean attemptsLoaded = "attempts".equals(lazyPanel);
        boolean hasStartedAttempts = hasAnyAttempts(assessmentId);
        boolean canEditStructure = !assessmentView.isCompleted() && !hasStartedAttempts;
        request.setAttribute("assessment", assessmentView);
        request.setAttribute("questions", viewFactory.questionViews(assessmentId));
        request.setAttribute("questionTypes", QuestionType.values());
        request.setAttribute("nextQuestionOrder", nextQuestionOrder(assessmentId));
        List<AttemptView> attempts = attemptsLoaded ? viewFactory.attemptViewsByAssessment(assessmentId) : List.of();
        request.setAttribute("attempts", attempts);
        request.setAttribute("responsesByAttemptId", attemptsLoaded ? responseViewsByAttempt(attempts) : Map.of());
        request.setAttribute("assessmentDetailEnrollmentsLoaded", enrollmentsLoaded);
        request.setAttribute("assessmentDetailAttemptsLoaded", attemptsLoaded);
        if (enrollmentsLoaded) {
            prepareAssessmentEnrollmentAttributes(request, assessmentView);
        } else {
            prepareAssessmentEnrollmentCounts(request, assessmentView.getId());
            request.setAttribute("assessmentEnrollments", List.of());
            request.setAttribute("pendingAssessmentEnrollments", List.of());
            request.setAttribute("activeAssessmentEnrollments", List.of());
            request.setAttribute("auditAssessmentEnrollments", List.of());
            request.setAttribute("managedAssessmentEnrollments", List.of());
            request.setAttribute("assessmentStudentOptions", List.of());
            request.setAttribute("assessmentActiveEnrollmentByStudent", Map.of());
            request.setAttribute("showAssessmentEnrollmentRequests", Boolean.FALSE);
            request.setAttribute("showAssessmentEnrollmentCreate", Boolean.FALSE);
            request.setAttribute("canManageAssessmentEnrollments", !assessmentView.isCompleted());
        }
        request.setAttribute("assessmentDetailPendingCorrectionCount", pendingCorrectionCount(assessmentId));
        request.setAttribute("canEditAssessmentStructure", canEditStructure);
        request.setAttribute("hasSubmittedAttempts", hasStartedAttempts);
        request.setAttribute("assessmentStructureLockMessage", hasStartedAttempts
                ? "Assessment questions cannot change after attempts have started"
                : assessmentView.isCompleted() ? "Completed assessments cannot be changed." : "");
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareAssessmentContext(request, assessmentView, "detail");
        prepareDashboard(request, "assessments", "Assessment Details");
        forward(request, response, assessmentDetailJsp(request));
    }

    private static String requestedAssessmentLazyPanel(HttpServletRequest request) {
        String value = text(request, "lazyPanel");
        return "enrollments".equals(value) || "attempts".equals(value) ? value : "builder";
    }

    private void prepareAssessmentEnrollmentCounts(HttpServletRequest request, long assessmentId)
            throws ServletException {
        try {
            request.setAttribute(
                    "assessmentPendingEnrollmentCount",
                    assessmentEnrollmentDAO.countByAssessmentAndState(assessmentId, EnrollmentState.PENDING)
            );
            request.setAttribute(
                    "assessmentActiveEnrollmentCount",
                    assessmentEnrollmentDAO.countByAssessmentAndState(assessmentId, EnrollmentState.ACTIVE)
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment enrollment counts", exception);
        }
    }

    private int pendingCorrectionCount(long assessmentId) throws ServletException {
        try {
            return attemptDAO.countByAssessmentAndState(assessmentId, AttemptState.SUBMITTED);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load pending assessment corrections", exception);
        }
    }

    private void createAssessment(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean modalCreate = isModalCreate(request);
        try {
            Assessment assessment = assessmentService.createAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentCreateCommand(request),
                    request.getRemoteAddr()
            );
            if (modalCreate) {
                flashSuccess(request, "Assessment created.");
                writeAssessmentCreated(request, response, assessment.id());
                return;
            }
            flashSuccess(request, "Assessment created.");
            redirect(request, response, "/learning/assessments/" + assessment.id());
        } catch (RuntimeException exception) {
            if (modalCreate) {
                String message = messageFor(exception);
                flashError(request, message);
                writeAssessmentModalResult(response, false, message);
                return;
            }
            showForm(request, response, AssessmentFormData.from(request, null), true, messageFor(exception));
        }
    }

    private void copyRepositoryAssessment(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Assessment assessment = assessmentService.cloneAssessmentToBlock(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    longParameter(request, "sourceAssessmentId"),
                    longParameter(request, "targetContentBlockId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment copied from repository.");
            redirect(request, response, "/learning/assessments/" + assessment.id());
        } catch (RuntimeException exception) {
            showForm(request, response, AssessmentFormData.from(request, null), true, messageFor(exception));
        }
    }

    private void updateAssessment(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws ServletException, IOException {
        boolean modalRequest = isAssessmentModalRequest(request);
        try {
            assessmentService.updateAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    assessmentUpdateCommand(request),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment updated.");
            if (modalRequest) {
                writeAssessmentModalResult(response, true, "Assessment updated.");
                return;
            }
            redirect(request, response, "/learning/assessments/" + assessmentId);
        } catch (RuntimeException exception) {
            String message = messageFor(exception);
            if (modalRequest) {
                writeAssessmentModalResult(response, false, message);
                return;
            }
            showForm(request, response, AssessmentFormData.from(request, assessmentId), false, message);
        }
    }

    private void downloadAssessmentPdf(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        boolean inline = "inline".equalsIgnoreCase(request.getParameter("disposition"));
        if (!canAccessAssessmentPdf(request, assessmentId, inline)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Assessment assessment = assessmentService.getAssessment(assessmentId);
        byte[] pdf = assessmentPdfService.renderAssessmentPdf(assessmentId);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                (inline ? "inline" : "attachment")
                        + "; filename=\"" + pdfFilename(assessment.title(), assessmentId) + "\""
        );
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private void downloadAttemptPdf(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId
    ) throws IOException {
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Attempt attempt;
        try {
            attempt = attemptDAO.findById(attemptId).orElse(null);
        } catch (SQLException exception) {
            throw new IOException("Failed to load attempt PDF context", exception);
        }
        if (attempt == null || attempt.assessmentId() != assessmentId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Assessment assessment = assessmentService.getAssessment(assessmentId);
        byte[] pdf = assessmentPdfService.renderAttemptResponsesPdf(assessmentId, attemptId);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + headerSafeFilename(attemptPdfFilename(assessment.title(), attempt)) + "\""
        );
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private boolean canAccessAssessmentPdf(HttpServletRequest request, long assessmentId, boolean inline) {
        if (canManage(request, assessmentId)) {
            return true;
        }
        if (!inline || primaryProfile(requireCurrentUser(request)) == AccessProfileType.STUDENT) {
            return false;
        }
        try {
            return contentItemDAO.hasActiveAssessmentRepositoryReference(assessmentId);
        } catch (SQLException exception) {
            return false;
        }
    }

    private void downloadResponseAttachment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId,
            long responseId
    ) throws IOException, ServletException {
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Attempt attempt;
        Response storedResponse;
        try {
            attempt = attemptDAO.findById(attemptId).orElse(null);
            storedResponse = responseDAO.findById(responseId).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load response attachment", exception);
        }
        if (attempt == null
                || attempt.assessmentId() != assessmentId
                || storedResponse == null
                || storedResponse.attemptId() != attemptId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (storedResponse.attachment() == null || storedResponse.attachment().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path storedFile;
        try {
            storedFile = pdfUploadService.resolveStoredContentFile(storedResponse.attachment());
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!Files.isRegularFile(storedFile)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String filename = attachmentFileName(storedResponse.attachment());
        String contentType = getServletContext().getMimeType(filename);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + headerSafeFilename(filename) + "\"");
        response.setHeader("Cache-Control", "private, no-store");
        response.setContentLengthLong(Files.size(storedFile));
        Files.copy(storedFile, response.getOutputStream());
    }

    private void updateEnrollmentPolicy(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.updateEnrollmentMode(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    enrollmentApprovalMode(text(request, "approvalMode")),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Assessment enrollment policy saved.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private void enrollStudent(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.enrollStudentInAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    new AssessmentEnrollmentCommand(
                            longParameter(request, "studentUserId"),
                            assessmentId
                    ),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Student enrolled in assessment.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private void approveEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.approveAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Assessment enrollment approved.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private void rejectEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.rejectAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Assessment enrollment rejected.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private void withdrawEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.withdrawAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Assessment enrollment withdrawn.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private void deleteEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            assessmentEnrollmentService.deleteAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "enrollments");
                return;
            }
            flashSuccess(request, "Assessment enrollment deleted.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments");
    }

    private static boolean isModalCreate(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter("modalCreate"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static boolean isAssessmentModalRequest(HttpServletRequest request) {
        return "1".equals(text(request, "modal"));
    }

    private static void writeAssessmentCreated(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId
    ) throws IOException {
        String location = request.getContextPath() + "/learning/assessments/" + assessmentId;
        boolean modalRequest = isAssessmentModalRequest(request);
        // A modal submission must finish in the iframe response itself.  A
        // Location header on a 201 response makes the browser navigate the
        // iframe to the assessment detail page before the parent postMessage
        // can run; that detail page is intentionally not frameable.  Keep the
        // redirect metadata for the regular full-page flow only.
        response.setStatus(modalRequest ? HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED);
        if (!modalRequest) {
            response.setHeader("Location", location);
        }
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (modalRequest) {
            writeAssessmentModalResult(response, true, "Assessment created.");
        } else {
            response.getWriter().write(location);
        }
    }

    private static void writeAssessmentModalResult(
            HttpServletResponse response,
            boolean success,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("<!doctype html><html><body><script>"
                + "window.parent.postMessage({type:'gape:assessment:result',success:" + success
                + ",message:" + jsonString(message) + "},window.location.origin);"
                + "</script></body></html>");
    }

    private static String jsonString(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("</", "<\\/")
                + "\"";
    }

    private static void writePlainError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message == null || message.isBlank() ? "Assessment could not be created." : message);
    }

    private static void writeQuestionOptionCreated(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId,
            QuestionOption option
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(questionOptionJson(request, assessmentId, questionId, option));
    }

    private static void writeQuestionOptionsState(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId,
            List<QuestionOption> options
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder json = new StringBuilder("{\"options\":[");
        for (int index = 0; index < options.size(); index += 1) {
            if (index > 0) {
                json.append(',');
            }
            json.append(questionOptionJson(request, assessmentId, questionId, options.get(index)));
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    private static void writeQuestionOrderState(HttpServletResponse response, List<Question> questions)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder json = new StringBuilder("{\"questions\":[");
        for (int index = 0; index < questions.size(); index += 1) {
            Question question = questions.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":")
                    .append(question.id())
                    .append(",\"orderNo\":")
                    .append(question.orderNo())
                    .append('}');
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    private static String questionOptionJson(
            HttpServletRequest request,
            long assessmentId,
            long questionId,
            QuestionOption option
    ) {
        String archiveUrl = request.getContextPath()
                + "/learning/assessments/" + assessmentId
                + "/questions/" + questionId
                + "/options/" + option.id()
                + "/archive";
        return "{"
                + "\"id\":" + option.id()
                + ",\"orderNo\":" + option.orderNo()
                + ",\"text\":\"" + jsonEscape(option.text()) + "\""
                + ",\"correct\":" + Boolean.TRUE.equals(option.correct())
                + ",\"archiveUrl\":\"" + jsonEscape(archiveUrl) + "\""
                + "}";
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index += 1) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u");
                        String hex = Integer.toHexString(character);
                        for (int pad = hex.length(); pad < 4; pad += 1) {
                            escaped.append('0');
                        }
                        escaped.append(hex);
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private void archiveAssessment(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        try {
            assessmentService.archiveAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/assessments");
    }

    private void deleteAssessment(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        try {
            assessmentService.deleteAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments");
    }

    private void createQuestion(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws ServletException, IOException {
        try {
            long actorUserId = actorUserId(request);
            Long sessionId = currentSessionId(request);
            AccessProfileType profileType = primaryProfile(requireCurrentUser(request));
            String sourceIp = request.getRemoteAddr();
            Question question = questionService.createQuestion(
                    actorUserId,
                    sessionId,
                    profileType,
                    questionCreateCommand(request, assessmentId),
                    sourceIp
            );
            createDefaultOptions(actorUserId, sessionId, profileType, question, sourceIp);
            redirect(request, response, "/learning/assessments/" + assessmentId + "#question-" + question.id());
        } catch (RuntimeException exception) {
            showDetail(request, response, assessmentId, messageFor(exception));
        }
    }

    private void updateQuestion(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId
    ) throws ServletException, IOException {
        boolean async = isAsyncRequest(request);
        try {
            Question question = questionService.getQuestion(questionId);
            if (question.assessmentId() != assessmentId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            long actorUserId = actorUserId(request);
            Long sessionId = currentSessionId(request);
            AccessProfileType profileType = primaryProfile(requireCurrentUser(request));
            String sourceIp = request.getRemoteAddr();
            updateSubmittedQuestion(request, actorUserId, sessionId, profileType, question, sourceIp);
            if (async) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            flashSuccess(request, "Question updated.");
            redirect(request, response, "/learning/assessments/" + assessmentId + "#question-" + questionId);
        } catch (RuntimeException | SQLException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageForRuntime(exception));
                return;
            }
            showDetail(request, response, assessmentId, messageForRuntime(exception));
        }
    }

    private void rebalanceQuestionScores(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        try {
            questionService.rebalanceActiveQuestionScores(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    request.getRemoteAddr()
            );
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (RuntimeException exception) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
        }
    }

    private void reorderQuestions(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws ServletException, IOException {
        boolean async = isAsyncRequest(request);
        try {
            List<Question> questions = questionService.reorderQuestions(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    orderedQuestionIds(request),
                    request.getRemoteAddr()
            );
            if (async) {
                writeQuestionOrderState(response, questions);
                return;
            }
            flashSuccess(request, "Questions reordered.");
            redirect(request, response, "/learning/assessments/" + assessmentId);
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            showDetail(request, response, assessmentId, messageFor(exception));
        }
    }

    private void archiveQuestion(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId
    ) throws ServletException, IOException {
        deleteQuestion(request, response, assessmentId, questionId);
    }

    private void deleteQuestion(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId
    ) throws ServletException, IOException {
        try {
            Question question = questionService.getQuestion(questionId);
            if (question.assessmentId() != assessmentId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            questionService.deleteQuestion(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    questionId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Question deleted.");
            redirect(request, response, "/learning/assessments/" + assessmentId);
        } catch (RuntimeException exception) {
            showDetail(request, response, assessmentId, messageFor(exception));
        }
    }

    private void createOption(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId
    ) throws ServletException, IOException {
        try {
            Question question = questionService.getQuestion(questionId);
            if (question.assessmentId() != assessmentId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            long actorUserId = actorUserId(request);
            Long sessionId = currentSessionId(request);
            AccessProfileType profileType = primaryProfile(requireCurrentUser(request));
            String sourceIp = request.getRemoteAddr();
            updateSubmittedQuestion(request, actorUserId, sessionId, profileType, question, sourceIp);
            QuestionOption option = optionService.createOption(
                    actorUserId,
                    sessionId,
                    profileType,
                    optionCreateCommand(request, questionId),
                    sourceIp
            );
            if (isAsyncRequest(request)) {
                writeQuestionOptionCreated(request, response, assessmentId, questionId, option);
                return;
            }
            flashSuccess(request, "Option created.");
            redirect(request, response, "/learning/assessments/" + assessmentId + "#question-" + questionId);
        } catch (RuntimeException | SQLException exception) {
            if (isAsyncRequest(request)) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageForRuntime(exception));
                return;
            }
            showDetail(request, response, assessmentId, messageForRuntime(exception));
        }
    }

    private void updateOption(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId,
            long optionId
    ) throws ServletException, IOException {
        try {
            QuestionOption option = optionDAO.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
            if (option.questionId() != questionId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            optionService.updateOption(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    optionId,
                    optionUpdateCommand(request, option),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Option updated.");
            redirect(request, response, "/learning/assessments/" + assessmentId + "#question-" + questionId);
        } catch (RuntimeException | SQLException exception) {
            showDetail(request, response, assessmentId, messageForRuntime(exception));
        }
    }

    private void archiveOption(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long questionId,
            long optionId
    ) throws ServletException, IOException {
        boolean async = isAsyncRequest(request);
        try {
            Question question = questionService.getQuestion(questionId);
            QuestionOption option = optionDAO.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
            if (question.assessmentId() != assessmentId || option.questionId() != questionId) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<QuestionOption> activeOptions = optionService.archiveOption(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    optionId,
                    request.getRemoteAddr()
            );
            if (async) {
                writeQuestionOptionsState(request, response, assessmentId, questionId, activeOptions);
                return;
            }
            flashSuccess(request, "Option deactivated.");
            redirect(request, response, "/learning/assessments/" + assessmentId + "#question-" + questionId);
        } catch (RuntimeException | SQLException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageForRuntime(exception));
                return;
            }
            showDetail(request, response, assessmentId, messageForRuntime(exception));
        }
    }

    private void autoCorrectAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId
    ) throws IOException {
        try {
            correctionService.autoCorrectAttempt(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    attemptId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Automatic correction completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#attempts");
    }

    private void autoCorrectEligibleResponses(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId
    ) throws IOException, ServletException {
        if (!attemptBelongsToAssessment(attemptId, assessmentId)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean async = isAsyncRequest(request);
        try {
            CorrectionResult result = correctionService.autoCorrectEligibleResponses(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    attemptId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                writeCorrectionResult(response, assessmentId, attemptId, result);
                return;
            }
            flashSuccess(request, "Eligible responses automatically corrected.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#attempts");
    }

    private void manualCorrectAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        try {
            correctionService.correctAttemptManually(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    attemptId,
                    responseScoreParameters(request),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                showDetail(request, response, assessmentId, null, "attempts");
                return;
            }
            flashSuccess(request, "Attempt correction submitted.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#attempts");
    }

    private void correctResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId,
            long responseId
    ) throws IOException {
        try {
            correctionService.correctResponseManually(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    new ManualCorrectionCommand(responseId, decimalParameter(request, "score")),
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            flashSuccess(request, "Response score saved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#attempts");
    }

    private void autoCorrectResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId,
            long responseId
    ) throws IOException, ServletException {
        boolean async = isAsyncRequest(request);
        Attempt attempt;
        Response storedResponse;
        try {
            attempt = attemptDAO.findById(attemptId).orElse(null);
            storedResponse = responseDAO.findById(responseId).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load response for correction", exception);
        }
        if (attempt == null
                || attempt.assessmentId() != assessmentId
                || storedResponse == null
                || storedResponse.attemptId() != attemptId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            CorrectionResult result = correctionService.autoCorrectResponse(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    responseId,
                    request.getRemoteAddr()
            );
            refreshLearningEvents();
            if (async) {
                writeCorrectionResult(response, assessmentId, attemptId, result);
                return;
            }
            flashSuccess(request, "Response automatically corrected.");
        } catch (RuntimeException exception) {
            if (async) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#attempts");
    }

    private boolean attemptBelongsToAssessment(long attemptId, long assessmentId) throws ServletException {
        try {
            Attempt attempt = attemptDAO.findById(attemptId).orElse(null);
            return attempt != null && attempt.assessmentId() == assessmentId;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load attempt", exception);
        }
    }

    private void writeCorrectionResult(
            HttpServletResponse response,
            long assessmentId,
            long attemptId,
            CorrectionResult result
    ) throws IOException, ServletException {
        try {
            Assessment assessment = assessmentService.getAssessment(assessmentId);
            Attempt updatedAttempt = attemptDAO.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
            List<ResponseView> responses = viewFactory.responseViews(attemptId);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            StringBuilder payload = new StringBuilder();
            payload.append('{');
            payload.append("\"attemptId\":").append(result.attemptId()).append(',');
            BigDecimal visibleScore = visibleAttemptScore(updatedAttempt);
            payload.append("\"score\":\"")
                    .append(jsonEscape(visibleScore == null ? "" : grade(visibleScore)))
                    .append("\",");
            payload.append("\"scoreOverMax\":\"")
                    .append(jsonEscape(attemptScoreOverMaxLabel(updatedAttempt, assessment)))
                    .append("\",");
            payload.append("\"state\":\"").append(jsonEscape(attemptStateLabel(updatedAttempt))).append("\",");
            payload.append("\"stateBadgeClass\":\"").append(jsonEscape(attemptStateBadgeClass(updatedAttempt))).append("\",");
            payload.append("\"automaticallyCorrectedResponses\":")
                    .append(result.automaticallyCorrectedResponses())
                    .append(',');
            payload.append("\"pendingManualResponses\":")
                    .append(result.pendingManualResponses())
                    .append(',');
            payload.append("\"responses\":[");
            for (int index = 0; index < responses.size(); index += 1) {
                ResponseView item = responses.get(index);
                if (index > 0) {
                    payload.append(',');
                }
                payload.append('{');
                payload.append("\"id\":").append(item.getId()).append(',');
                payload.append("\"score\":\"").append(jsonEscape(item.getScore())).append("\",");
                payload.append("\"scoreLabel\":\"").append(jsonEscape(item.getScoreLabel())).append("\"");
                payload.append('}');
            }
            payload.append(']');
            payload.append('}');
            response.getWriter().write(payload.toString());
        } catch (RuntimeException | SQLException exception) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageForRuntime(exception));
        }
    }

    private Map<Long, BigDecimal> responseScoreParameters(HttpServletRequest request) {
        Map<Long, BigDecimal> scores = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            if (name != null && name.startsWith("score_") && name.length() > "score_".length()) {
                long responseId = Long.parseLong(name.substring("score_".length()));
                scores.put(responseId, decimalParameter(request, name));
            }
        }
        return scores;
    }

    private Map<Long, List<ResponseView>> responseViewsByAttempt(List<AttemptView> attempts) {
        Map<Long, List<ResponseView>> responses = new LinkedHashMap<>();
        for (AttemptView attempt : attempts) {
            if (attempt.isCorrectionOpen()) {
                responses.put(attempt.getId(), viewFactory.responseViews(attempt.getId()));
            }
        }
        return responses;
    }

    private void prepareForm(
            HttpServletRequest request,
            AssessmentFormData form,
            boolean creating,
            String error
    ) throws ServletException {
        prepareAssessmentFormAttributes(request, form, creating, error);
        request.setAttribute("assessmentModal", "1".equals(text(request, "modal")));
        request.setAttribute("learningAssessmentActiveChild", creating ? "new" : "edit");
        prepareDashboard(request, "assessments", creating ? "Create Assessment" : "Edit Assessment");
    }

    private void prepareAssessmentFormAttributes(
            HttpServletRequest request,
            AssessmentFormData form,
            boolean creating,
            String error
    ) throws ServletException {
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        List<ContextSelectOptionView> subjectOptions = subjectOptions(request, form.getSubjectId(), creating);
        List<ContextSelectOptionView> contentBlockOptions = contentBlockOptions(request, form.getContentBlockId());
        List<ContextSelectOptionView> classGroupOptions = classGroupOptions(request, form.getClassGroupIds(), creating);
        List<SelectOptionView> organizationOptions = organizationOptions(subjectOptions, contentBlockOptions, classGroupOptions);
        List<ContextSelectOptionView> courseOptions = courseOptions(subjectOptions, contentBlockOptions, classGroupOptions);
        request.setAttribute("subjectOptions", subjectOptions);
        request.setAttribute("contentBlockOptions", contentBlockOptions);
        request.setAttribute("classGroupOptions", classGroupOptions);
        request.setAttribute("physicalRoomOptions", physicalRoomOptions(organizationOptions, form.getPhysicalRoomCode()));
        request.setAttribute("organizationOptions", organizationOptions);
        request.setAttribute("organicUnitOptions", organicUnitOptions(subjectOptions, contentBlockOptions, classGroupOptions));
        request.setAttribute("courseOptions", courseOptions);
        request.setAttribute("repositoryAssessments", repositoryAssessmentOptions(request));
        request.setAttribute("assessmentTypes", AssessmentType.values());
        request.setAttribute("assessmentModes", AssessmentMode.values());
        request.setAttribute("assessmentCorrectionModes", AssessmentCorrectionMode.values());
        request.setAttribute("assessmentStates", AssessmentState.values());
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
    }

    private void prepareAssessmentEnrollmentAttributes(HttpServletRequest request, AssessmentView assessment)
            throws ServletException {
        try {
            List<AssessmentEnrollmentView> enrollments = assessmentEnrollmentDAO.findByAssessment(assessment.getId())
                    .stream()
                    .map(this::assessmentEnrollmentView)
                    .toList();
            List<AssessmentEnrollmentView> pendingEnrollments = enrollments.stream()
                    .filter(AssessmentEnrollmentView::isPending)
                    .toList();
            List<AssessmentEnrollmentView> activeEnrollments = enrollments.stream()
                    .filter(AssessmentEnrollmentView::isActive)
                    .toList();
            List<AssessmentEnrollmentView> auditEnrollments = enrollments.stream()
                    .filter(enrollment -> !enrollment.isPending() && !enrollment.isActive())
                    .toList();
            List<AssessmentEnrollmentView> managedEnrollments = enrollments.stream()
                    .filter(enrollment -> !enrollment.isPending())
                    .toList();
            Map<Long, Boolean> existingEnrollmentByStudent = new LinkedHashMap<>();
            for (AssessmentEnrollmentView enrollment : enrollments) {
                existingEnrollmentByStudent.put(enrollment.getStudentUserId(), true);
            }
            List<UserOptionView> eligibleStudents = eligibleAssessmentStudentOptions(assessment.getId());
            List<UserOptionView> availableStudents = eligibleStudents.stream()
                    .filter(student -> !existingEnrollmentByStudent.containsKey(student.getId()))
                    .toList();
            request.setAttribute("assessmentEnrollments", enrollments);
            request.setAttribute("pendingAssessmentEnrollments", pendingEnrollments);
            request.setAttribute("activeAssessmentEnrollments", activeEnrollments);
            request.setAttribute("auditAssessmentEnrollments", auditEnrollments);
            request.setAttribute("managedAssessmentEnrollments", managedEnrollments);
            request.setAttribute("showAssessmentEnrollmentRequests",
                    !assessment.isAutomaticEnrollment() && !pendingEnrollments.isEmpty());
            request.setAttribute("assessmentStudentOptions", availableStudents);
            request.setAttribute("assessmentActiveEnrollmentByStudent", existingEnrollmentByStudent);
            request.setAttribute("showAssessmentEnrollmentCreate",
                    !assessment.isAutomaticEnrollment() && !availableStudents.isEmpty());
            request.setAttribute("canManageAssessmentEnrollments", !assessment.isCompleted());
            request.setAttribute("assessmentPendingEnrollmentCount", pendingEnrollments.size());
            request.setAttribute("assessmentActiveEnrollmentCount", activeEnrollments.size());
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment enrollments", exception);
        }
    }

    private void synchronizeAutomaticAssessmentEnrollments(AssessmentView assessment) throws ServletException {
        if (assessment.isCompleted() || !assessment.isAutomaticEnrollment()) {
            return;
        }
        try {
            assessmentEnrollmentDAO.syncAutomaticEnrollments(assessment.getId());
        } catch (SQLException exception) {
            throw new ServletException("Failed to synchronize automatic assessment enrollments", exception);
        }
    }

    private List<UserOptionView> eligibleAssessmentStudentOptions(long assessmentId) throws SQLException {
        List<UserOptionView> students = new ArrayList<>();
        for (Long studentUserId : assessmentEnrollmentDAO.findEligibleStudentIds(assessmentId)) {
            User user = userDAO.findById(studentUserId).orElse(null);
            if (user != null) {
                students.add(UserOptionView.from(user, false));
            }
        }
        return students;
    }

    private AssessmentEnrollmentView assessmentEnrollmentView(AssessmentEnrollment enrollment) {
        try {
            User user = userDAO.findById(enrollment.studentUserId()).orElse(null);
            return AssessmentEnrollmentView.from(
                    enrollment,
                    user == null ? "Unknown student" : user.name(),
                    user == null ? "" : user.email()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load assessment enrollment user", exception);
        }
    }

    private List<ContextSelectOptionView> subjectOptions(
            HttpServletRequest request,
            String selectedSubjectId,
            boolean creating
    )
            throws ServletException {
        try {
            List<ContextSelectOptionView> options = new ArrayList<>();
            for (Subject subject : subjectDAO.findAll().stream()
                    .filter(item -> item.state() == pt.isel.gape.learning.model.SubjectState.ACTIVE
                            || (!creating && Long.toString(item.id()).equals(selectedSubjectId)))
                    .filter(item -> (!creating && Long.toString(item.id()).equals(selectedSubjectId))
                            || canManageContext(request, item.id(), null))
                    .toList()) {
                options.add(new ContextSelectOptionView(
                        Long.toString(subject.id()),
                        subjectLabel(subject),
                        subject.name(),
                        Long.toString(subject.id()).equals(selectedSubjectId),
                        subject.organizationId(),
                        subjectOrganicUnitIds(subject.id()),
                        subjectCourseIds(subject.id()),
                        Long.toString(subject.id()),
                        ""
                ));
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load subject options", exception);
        }
    }

    private List<ContextSelectOptionView> contentBlockOptions(HttpServletRequest request, String selectedContentBlockId)
            throws ServletException {
        try {
            List<ContextSelectOptionView> options = new ArrayList<>();
            for (pt.isel.gape.learning.model.ClassGroup classGroup : classGroupDAO.findAll().stream()
                    .filter(item -> canManageContext(request, item.subjectId(), item.id()))
                    .sorted(Comparator.comparing(pt.isel.gape.learning.model.ClassGroup::code))
                    .toList()) {
                Course course = courseDAO.findById(classGroup.courseId()).orElse(null);
                for (ContentBlock block : contentBlockDAO.findByClassGroup(classGroup.id())) {
                    options.add(new ContextSelectOptionView(
                            Long.toString(block.id()),
                            classGroup.code() + " | " + block.name(),
                            block.name(),
                            Long.toString(block.id()).equals(selectedContentBlockId),
                            contextOrganizationId(course, classGroup.subjectId()),
                            contextOrganicUnitIds(course),
                            course == null ? List.of() : List.of(course.id()),
                            Long.toString(classGroup.subjectId()),
                            Long.toString(classGroup.id()),
                            classGroup.startsAt() == null ? "" : classGroup.startsAt().toString(),
                            classGroup.endsAt() == null ? "" : classGroup.endsAt().toString()
                    ));
                }
            }
            return options;
        } catch (RuntimeException | SQLException exception) {
            throw new ServletException("Failed to load content block options", exception);
        }
    }

    private List<ContextSelectOptionView> classGroupOptions(
            HttpServletRequest request,
            List<String> selectedClassGroupIds,
            boolean creating
    )
            throws ServletException {
        List<String> selected = selectedClassGroupIds == null ? List.of() : selectedClassGroupIds;
        try {
            List<ContextSelectOptionView> options = new ArrayList<>();
            for (pt.isel.gape.learning.model.ClassGroup classGroup : classGroupDAO.findAll().stream()
                    .filter(item -> item.state() == pt.isel.gape.learning.model.ClassGroupState.ACTIVE
                            || (!creating && selected.contains(Long.toString(item.id()))))
                    .filter(item -> (!creating && selected.contains(Long.toString(item.id())))
                            || canManageContext(request, item.subjectId(), item.id()))
                    .sorted(Comparator.comparing(pt.isel.gape.learning.model.ClassGroup::code))
                    .toList()) {
                Subject subject = subjectDAO.findById(classGroup.subjectId()).orElse(null);
                Course course = courseDAO.findById(classGroup.courseId()).orElse(null);
                String subjectLabel = subject == null ? "Subject " + classGroup.subjectId() : subjectLabel(subject);
                String value = Long.toString(classGroup.id());
                options.add(new ContextSelectOptionView(
                        value,
                        classGroup.code() + " | " + subjectLabel,
                        classGroup.code() + " | " + subjectLabel,
                        selected.contains(value),
                        contextOrganizationId(course, classGroup.subjectId()),
                        contextOrganicUnitIds(course),
                        course == null ? List.of() : List.of(course.id()),
                        Long.toString(classGroup.subjectId()),
                        Long.toString(classGroup.id()),
                        classGroup.startsAt() == null ? "" : classGroup.startsAt().toString(),
                        classGroup.endsAt() == null ? "" : classGroup.endsAt().toString()
                ));
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load class group options", exception);
        }
    }

    private List<ContextSelectOptionView> physicalRoomOptions(
            List<SelectOptionView> organizationOptions,
            String selectedPhysicalRoomCode
    ) throws ServletException {
        String selectedCode = selectedPhysicalRoomCode == null ? "" : selectedPhysicalRoomCode.trim();
        List<Long> organizationIds = organizationOptions.stream()
                .map(SelectOptionView::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(Long::parseLong)
                .distinct()
                .toList();
        try {
            List<ContextSelectOptionView> options = new ArrayList<>();
            for (Long organizationId : organizationIds) {
                for (PhysicalRoom room : physicalRoomDAO.findByOrganization(organizationId).stream()
                        .filter(item -> item.state() == PhysicalRoomState.ACTIVE
                                || item.code().equals(selectedCode))
                        .sorted(Comparator.comparing(PhysicalRoom::code))
                        .toList()) {
                    options.add(new ContextSelectOptionView(
                            room.code(),
                            room.code() + " | " + room.name(),
                            room.location() == null || room.location().isBlank() ? room.name() : room.location(),
                            room.code().equals(selectedCode),
                            room.organizationId(),
                            room.organicUnitId() == null ? List.of() : List.of(room.organicUnitId())
                    ));
                }
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load physical room options", exception);
        }
    }

    private List<SelectOptionView> organizationOptions(
            List<ContextSelectOptionView> subjectOptions,
            List<ContextSelectOptionView> contentBlockOptions,
            List<ContextSelectOptionView> classGroupOptions
    )
            throws ServletException {
        Map<String, Boolean> selectedById = new LinkedHashMap<>();
        collectOrganizationSelections(selectedById, subjectOptions);
        collectOrganizationSelections(selectedById, contentBlockOptions);
        collectOrganizationSelections(selectedById, classGroupOptions);
        List<SelectOptionView> options = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedById.entrySet()) {
            options.add(new SelectOptionView(entry.getKey(), organizationLabel(entry.getKey()), entry.getValue()));
        }
        return options;
    }

    private List<ContextSelectOptionView> organicUnitOptions(
            List<ContextSelectOptionView> subjectOptions,
            List<ContextSelectOptionView> contentBlockOptions,
            List<ContextSelectOptionView> classGroupOptions
    )
            throws ServletException {
        Map<String, Boolean> selectedById = new LinkedHashMap<>();
        collectOrganicUnitSelections(selectedById, subjectOptions);
        collectOrganicUnitSelections(selectedById, contentBlockOptions);
        collectOrganicUnitSelections(selectedById, classGroupOptions);
        List<ContextSelectOptionView> options = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedById.entrySet()) {
            OrganicUnit unit = organicUnit(entry.getKey());
            options.add(new ContextSelectOptionView(
                    entry.getKey(),
                    unit == null ? "Organic unit " + entry.getKey() : organicUnitLabel(unit),
                    unit == null ? "Organic unit " + entry.getKey() : unit.name(),
                    entry.getValue(),
                    unit == null ? "" : Long.toString(unit.organizationId()),
                    entry.getKey()
            ));
        }
        return options;
    }

    private List<ContextSelectOptionView> courseOptions(
            List<ContextSelectOptionView> subjectOptions,
            List<ContextSelectOptionView> contentBlockOptions,
            List<ContextSelectOptionView> classGroupOptions
    )
            throws ServletException {
        Map<String, Boolean> selectedById = new LinkedHashMap<>();
        collectCourseSelections(selectedById, subjectOptions);
        collectCourseSelections(selectedById, contentBlockOptions);
        collectCourseSelections(selectedById, classGroupOptions);
        List<ContextSelectOptionView> options = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedById.entrySet()) {
            Course course = course(entry.getKey());
            options.add(new ContextSelectOptionView(
                    entry.getKey(),
                    course == null ? "Course " + entry.getKey() : courseOptionLabel(course),
                    course == null ? "Course " + entry.getKey() : courseOptionTitle(course),
                    entry.getValue(),
                    course == null ? "" : Long.toString(course.organizationId()),
                    course == null ? "" : ContextSelectOptionView.joinContextIds(contextOrganicUnitIds(course))
            ));
        }
        return options;
    }

    private List<AssessmentView> repositoryAssessmentOptions(HttpServletRequest request) throws ServletException {
        SessionUser actor = requireCurrentUser(request);
        try {
            assessmentService.synchronizeTemporalStates();
            List<Assessment> reusable = assessmentDAO.findAll().stream()
                    .filter(assessment -> {
                        try {
                            return contentItemDAO.hasActiveAssessmentRepositoryReference(assessment.id())
                                    && assessmentService.canManageAssessment(
                                    actor.userId(),
                                    currentSessionId(request),
                                    primaryProfile(actor),
                                    assessment.id(),
                                    request.getRemoteAddr()
                            );
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .sorted(Comparator.comparing(Assessment::title))
                    .toList();
            return viewFactory.assessmentViews(reusable);
        } catch (RuntimeException | SQLException exception) {
            throw new ServletException("Failed to load repository assessments", exception);
        }
    }

    private Long contextOrganizationId(Course course, long fallbackSubjectId) throws SQLException {
        if (course != null) {
            return course.organizationId();
        }
        return subjectDAO.findById(fallbackSubjectId)
                .map(Subject::organizationId)
                .orElse(null);
    }

    private List<Long> contextOrganicUnitIds(Course course) {
        return course == null || course.organicUnitId() == null ? List.of() : List.of(course.organicUnitId());
    }

    private List<Long> subjectOrganicUnitIds(long subjectId) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        for (CourseSubjectAssociation association : courseSubjectDAO.findBySubject(subjectId)) {
            Course course = courseDAO.findById(association.courseId()).orElse(null);
            if (course != null && course.organicUnitId() != null) {
                ids.add(course.organicUnitId());
            }
        }
        return List.copyOf(ids);
    }

    private List<Long> subjectCourseIds(long subjectId) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        for (CourseSubjectAssociation association : courseSubjectDAO.findBySubject(subjectId)) {
            ids.add(association.courseId());
        }
        return List.copyOf(ids);
    }

    private void collectOrganizationSelections(Map<String, Boolean> selectedById, List<ContextSelectOptionView> options) {
        for (ContextSelectOptionView option : options) {
            if (!option.getOrganizationId().isBlank()) {
                selectedById.merge(option.getOrganizationId(), option.isSelected(), Boolean::logicalOr);
            }
        }
    }

    private void collectOrganicUnitSelections(Map<String, Boolean> selectedById, List<ContextSelectOptionView> options) {
        for (ContextSelectOptionView option : options) {
            for (String id : splitContextIds(option.getOrganicUnitIds())) {
                selectedById.merge(id, option.isSelected(), Boolean::logicalOr);
            }
        }
    }

    private void collectCourseSelections(Map<String, Boolean> selectedById, List<ContextSelectOptionView> options) {
        for (ContextSelectOptionView option : options) {
            for (String id : splitContextIds(option.getCourseIds())) {
                selectedById.merge(id, option.isSelected(), Boolean::logicalOr);
            }
        }
    }

    private String organizationLabel(String organizationId) throws ServletException {
        try {
            Organization organization = organizationDAO.findById(Long.parseLong(organizationId)).orElse(null);
            if (organization == null) {
                return "Organization " + organizationId;
            }
            return organization.acronym() == null || organization.acronym().isBlank()
                    ? organization.name()
                    : organization.acronym() + " | " + organization.name();
        } catch (NumberFormatException | SQLException exception) {
            throw new ServletException("Failed to load organization label", exception);
        }
    }

    private OrganicUnit organicUnit(String organicUnitId) throws ServletException {
        try {
            return organicUnitDAO.findById(Long.parseLong(organicUnitId)).orElse(null);
        } catch (NumberFormatException | SQLException exception) {
            throw new ServletException("Failed to load organic unit label", exception);
        }
    }

    private Course course(String courseId) throws ServletException {
        try {
            return courseDAO.findById(Long.parseLong(courseId)).orElse(null);
        } catch (NumberFormatException | SQLException exception) {
            throw new ServletException("Failed to load course label", exception);
        }
    }

    private static String organicUnitLabel(OrganicUnit unit) {
        return unit.acronym() == null || unit.acronym().isBlank()
                ? unit.name()
                : unit.acronym() + " | " + unit.name();
    }

    private static String courseLabel(Course course) {
        return course.acronym() == null || course.acronym().isBlank()
                ? course.name()
                : course.acronym() + " | " + course.name();
    }

    private String courseOptionLabel(Course course) throws ServletException {
        String context = courseContextLabel(course);
        return context.isBlank() ? courseLabel(course) : courseLabel(course) + " | " + context;
    }

    private String courseOptionTitle(Course course) throws ServletException {
        String context = courseContextTitle(course);
        return context.isBlank() ? course.name() : course.name() + " | " + context;
    }

    private String courseContextLabel(Course course) throws ServletException {
        try {
            List<String> parts = new ArrayList<>();
            if (course.organicUnitId() != null) {
                organicUnitDAO.findById(course.organicUnitId())
                        .map(unit -> compactPart(unit.acronym(), unit.name()))
                        .ifPresent(parts::add);
            }
            organizationDAO.findById(course.organizationId())
                    .map(organization -> compactPart(organization.acronym(), organization.name()))
                    .ifPresent(parts::add);
            return String.join(" | ", parts);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load course context label", exception);
        }
    }

    private String courseContextTitle(Course course) throws ServletException {
        try {
            List<String> parts = new ArrayList<>();
            if (course.organicUnitId() != null) {
                organicUnitDAO.findById(course.organicUnitId())
                        .map(OrganicUnit::name)
                        .filter(name -> name != null && !name.isBlank())
                        .ifPresent(parts::add);
            }
            organizationDAO.findById(course.organizationId())
                    .map(Organization::name)
                    .filter(name -> name != null && !name.isBlank())
                    .ifPresent(parts::add);
            return String.join(" | ", parts);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load course context title", exception);
        }
    }

    private static String compactPart(String acronym, String name) {
        if (acronym != null && !acronym.isBlank() && !"-".equals(acronym)) {
            return acronym;
        }
        return name == null || name.isBlank() ? "-" : name;
    }

    private static List<String> splitContextIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(ids.split("\\|"))
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    private boolean canManageContext(HttpServletRequest request, Long subjectId, Long classGroupId) {
        SessionUser actor = requireCurrentUser(request);
        return assessmentService.canManageContext(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                classGroupId,
                request.getRemoteAddr()
        );
    }

    private boolean canManage(HttpServletRequest request, long assessmentId) {
        SessionUser actor = requireCurrentUser(request);
        return assessmentService.canManageAssessment(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                assessmentId,
                request.getRemoteAddr()
        );
    }

    private AssessmentFormData assessmentFormData(Assessment assessment) throws ServletException {
        try {
            return AssessmentFormData.from(
                    assessment,
                    assessmentDAO.findApplicableClassGroupIds(assessment.id())
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment class group context", exception);
        }
    }

    private AssessmentCreateCommand assessmentCreateCommand(HttpServletRequest request) {
        AssessmentMode mode = assessmentMode(text(request, "mode"));
        AssessmentCorrectionMode correctionMode = correctionModeFor(mode, text(request, "correctionMode"));
        return new AssessmentCreateCommand(
                optionalLong(request, "subjectId"),
                optionalLong(request, "contentBlockId"),
                text(request, "physicalRoomCode"),
                text(request, "title"),
                text(request, "description"),
                assessmentType(text(request, "type")),
                mode,
                correctionMode,
                decimalParameter(request, "maxGrade"),
                decimalParameter(request, "passingGrade"),
                decimalParameter(request, "finalGradeWeight"),
                optionalInteger(request, "attemptsLimit"),
                enrollmentApprovalMode(text(request, "enrollmentMode")),
                assessmentState(text(request, "state")),
                optionalDateTime(request, "availableFrom"),
                optionalDateTime(request, "availableUntil"),
                classGroupIds(request)
        );
    }

    private pt.isel.gape.learning.model.AssessmentUpdateCommand assessmentUpdateCommand(HttpServletRequest request) {
        AssessmentMode mode = assessmentMode(text(request, "mode"));
        AssessmentCorrectionMode correctionMode = correctionModeFor(mode, text(request, "correctionMode"));
        return new pt.isel.gape.learning.model.AssessmentUpdateCommand(
                optionalLong(request, "subjectId"),
                optionalLong(request, "contentBlockId"),
                text(request, "physicalRoomCode"),
                text(request, "title"),
                text(request, "description"),
                assessmentType(text(request, "type")),
                mode,
                correctionMode,
                decimalParameter(request, "maxGrade"),
                decimalParameter(request, "passingGrade"),
                decimalParameter(request, "finalGradeWeight"),
                optionalInteger(request, "attemptsLimit"),
                enrollmentApprovalMode(text(request, "enrollmentMode")),
                assessmentState(text(request, "state")),
                optionalDateTime(request, "availableFrom"),
                optionalDateTime(request, "availableUntil"),
                classGroupIds(request)
        );
    }

    private QuestionCreateCommand questionCreateCommand(HttpServletRequest request, long assessmentId) {
        QuestionType type = questionType(text(request, "type"));
        if (!type.isCreatable()) {
            throw new IllegalArgumentException("Question type is no longer available");
        }
        int orderNo = nextQuestionOrder(assessmentId);
        return new QuestionCreateCommand(
                assessmentId,
                "Q" + orderNo,
                defaultText(text(request, "statement"), defaultQuestionStatement(type)),
                type,
                orderNo,
                checkbox(request, "required", true),
                optionalDecimal(request, "score", BigDecimal.ONE),
                expectedAnswerForRequest(request, type)
        );
    }

    private QuestionUpdateCommand questionUpdateCommand(HttpServletRequest request, Question current) {
        return new QuestionUpdateCommand(
                current.code(),
                text(request, "statement"),
                current.type(),
                current.orderNo(),
                checkbox(request, "required", false),
                decimalParameter(request, "score"),
                expectedAnswerForRequest(request, current.type())
        );
    }

    private static List<Long> orderedQuestionIds(HttpServletRequest request) {
        String[] values = request.getParameterValues("questionId");
        List<Long> ids = new ArrayList<>();
        if (values == null) {
            return ids;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            ids.add(Long.parseLong(value));
        }
        return ids;
    }

    private static List<Long> classGroupIds(HttpServletRequest request) {
        String[] values = request.getParameterValues("classGroupIds");
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            long id = Long.parseLong(value);
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private QuestionOptionCreateCommand optionCreateCommand(HttpServletRequest request, long questionId) {
        String selectedSingleOption = request.getParameter("singleCorrect_" + questionId);
        return new QuestionOptionCreateCommand(
                questionId,
                nextOptionOrder(questionId),
                text(request, "text"),
                "new".equals(selectedSingleOption) || (selectedSingleOption == null && checkbox(request, "correct", false))
        );
    }

    private void createDefaultOptions(
            long actorUserId,
            Long sessionId,
            AccessProfileType profileType,
            Question question,
            String sourceIp
    ) {
        if (!question.type().allowsOptions()) {
            return;
        }
        optionService.createOption(
                actorUserId,
                sessionId,
                profileType,
                new QuestionOptionCreateCommand(
                        question.id(),
                        1,
                        "Option 1",
                        true
                ),
                sourceIp
        );
    }

    private static String expectedAnswerForRequest(HttpServletRequest request, QuestionType type) {
        if (type == QuestionType.FILE_UPLOAD) {
            return QuestionConfiguration.fileExpectedAnswer(request.getParameterValues("acceptedFormat"));
        }
        if (type == QuestionType.RATING) {
            return QuestionConfiguration.ratingExpectedAnswer(
                    text(request, "ratingDesign"),
                    text(request, "ratingMax"),
                    text(request, "expectedRatingValue")
            );
        }
        return text(request, "expectedAnswer");
    }

    private QuestionOptionUpdateCommand optionUpdateCommand(HttpServletRequest request, QuestionOption current) {
        return new QuestionOptionUpdateCommand(
                current.orderNo(),
                text(request, "text"),
                checkbox(request, "correct", false)
        );
    }

    private void updateSubmittedQuestion(
            HttpServletRequest request,
            long actorUserId,
            Long sessionId,
            AccessProfileType profileType,
            Question question,
            String sourceIp
    ) throws SQLException {
        questionService.updateQuestion(
                actorUserId,
                sessionId,
                profileType,
                question.id(),
                questionUpdateCommand(request, question),
                sourceIp
        );
        updateSubmittedOptions(request, actorUserId, sessionId, profileType, question.id(), sourceIp);
    }

    private void updateSubmittedOptions(
            HttpServletRequest request,
            long actorUserId,
            Long sessionId,
            AccessProfileType profileType,
            long questionId,
            String sourceIp
    ) throws SQLException {
        String selectedSingleOption = request.getParameter("singleCorrect_" + questionId);
        List<QuestionOption> options = optionDAO.findByQuestion(questionId);
        Map<Long, QuestionOptionUpdateCommand> submittedUpdates = new LinkedHashMap<>();
        for (QuestionOption option : options) {
            String submittedText = request.getParameter("optionText_" + option.id());
            if (submittedText == null) {
                continue;
            }
            boolean correct = selectedSingleOption == null
                    ? checkbox(request, "optionCorrect_" + option.id(), false)
                    : Long.toString(option.id()).equals(selectedSingleOption);
            int orderNo = optionalInteger(request, "optionOrder_" + option.id(), option.orderNo());
            submittedUpdates.put(
                    option.id(),
                    new QuestionOptionUpdateCommand(
                            orderNo,
                            submittedText,
                            correct
                    )
            );
        }
        if (!submittedUpdates.isEmpty()) {
            optionService.updateOptions(
                    actorUserId,
                    sessionId,
                    profileType,
                    questionId,
                    submittedUpdates,
                    sourceIp
            );
        }
    }

    private int nextQuestionOrder(long assessmentId) {
        try {
            return questionDAO.findByAssessment(assessmentId).stream()
                    .mapToInt(Question::orderNo)
                    .max()
                    .orElse(0) + 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to calculate question order", exception);
        }
    }

    private int nextOptionOrder(long questionId) {
        try {
            return optionDAO.findByQuestion(questionId).stream()
                    .mapToInt(QuestionOption::orderNo)
                    .max()
                    .orElse(0) + 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to calculate option order", exception);
        }
    }

    private boolean hasAnyAttempts(long assessmentId) {
        try {
            return assessmentDAO.hasAnyAttempts(assessmentId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check assessment attempts", exception);
        }
    }

    private long actorUserId(HttpServletRequest request) {
        return requireCurrentUser(request).userId();
    }

    private static void prepareAssessmentContext(
            HttpServletRequest request,
            AssessmentView assessment,
            String activeChild
    ) {
        request.setAttribute("learningAssessmentContextId", assessment.getId());
        request.setAttribute("learningAssessmentContextName", assessment.getTitle());
        request.setAttribute("learningAssessmentActiveChild", activeChild);
    }

    private static AssessmentType assessmentType(String value) {
        return value == null ? AssessmentType.FORM : AssessmentType.parse(value);
    }

    private static AssessmentMode assessmentMode(String value) {
        return value == null ? AssessmentMode.ONLINE : AssessmentMode.parse(value);
    }

    private static AssessmentCorrectionMode correctionMode(String value) {
        return value == null ? AssessmentCorrectionMode.AUTOMATIC : AssessmentCorrectionMode.parse(value);
    }

    private static AssessmentCorrectionMode correctionModeFor(AssessmentMode mode, String value) {
        return mode == AssessmentMode.ONSITE ? AssessmentCorrectionMode.MANUAL : correctionMode(value);
    }

    private static AssessmentState assessmentState(String value) {
        return value == null ? AssessmentState.DRAFT : AssessmentState.parse(value);
    }

    private String assessmentListJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_ASSESSMENT_LIST_JSP;
            case TEACHER -> INSTRUCTOR_ASSESSMENT_LIST_JSP;
            default -> ADMIN_ASSESSMENT_LIST_JSP;
        };
    }

    private String assessmentFormJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_ASSESSMENT_FORM_JSP;
            case TEACHER -> INSTRUCTOR_ASSESSMENT_FORM_JSP;
            default -> ADMIN_ASSESSMENT_FORM_JSP;
        };
    }

    private String assessmentDetailJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_ASSESSMENT_DETAIL_JSP;
            case TEACHER -> INSTRUCTOR_ASSESSMENT_DETAIL_JSP;
            default -> ADMIN_ASSESSMENT_DETAIL_JSP;
        };
    }

    private static EnrollmentApprovalMode enrollmentApprovalMode(String value) {
        return value == null || value.isBlank()
                ? EnrollmentApprovalMode.AUTO_APPROVE
                : EnrollmentApprovalMode.parse(value);
    }

    private static QuestionType questionType(String value) {
        return value == null ? QuestionType.SINGLE_CHOICE : QuestionType.parse(value);
    }

    private static String subjectLabel(Subject subject) {
        String acronym = subject.acronym() == null || subject.acronym().isBlank() ? "" : subject.acronym() + " | ";
        return acronym + subject.name();
    }

    private static String defaultQuestionStatement(QuestionType type) {
        return switch (type) {
            case SINGLE_CHOICE -> "New single choice question";
            case MULTIPLE_CHOICE -> "New multiple choice question";
            case SHORT_TEXT -> "New short text question";
            case PARAGRAPH -> "New paragraph question";
            case FILE_UPLOAD -> "New file upload question";
            case RATING -> "New rating question";
        };
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String pdfFilename(String title, long assessmentId) {
        String normalized = title == null || title.isBlank()
                ? "assessment-" + assessmentId
                : title.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) {
            normalized = "assessment-" + assessmentId;
        }
        return normalized + ".pdf";
    }

    private static String attemptPdfFilename(String title, Attempt attempt) {
        String base = pdfFilename(title, attempt.assessmentId()).replaceFirst("\\.pdf$", "");
        return base + "-attempt-" + attempt.attemptNumber() + "-student-" + attempt.studentUserId() + ".pdf";
    }

    private static String attachmentFileName(String attachment) {
        String normalized = attachment.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private static String headerSafeFilename(String filename) {
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private static String grade(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static String attemptScoreOverMaxLabel(Attempt attempt, Assessment assessment) {
        BigDecimal score = visibleAttemptScore(attempt);
        return score == null
                ? "-"
                : grade(score) + " / " + grade(assessment.maxGrade());
    }

    private static BigDecimal visibleAttemptScore(Attempt attempt) {
        return attempt.state() == AttemptState.CORRECTED ? attempt.score() : null;
    }

    private static String attemptStateLabel(Attempt attempt) {
        return switch (attempt.state()) {
            case IN_PROGRESS -> "In progress";
            case SUBMITTED -> "Submitted";
            case CORRECTED -> "Corrected";
            case EXPIRED -> "Expired";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String attemptStateBadgeClass(Attempt attempt) {
        return switch (attempt.state()) {
            case IN_PROGRESS -> "bg-main-50 text-main-600";
            case SUBMITTED -> "bg-warning-30 text-warning-600";
            case CORRECTED -> "bg-success-50 text-success-600";
            case EXPIRED -> "bg-neutral-30 text-neutral-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    private static boolean isAsyncRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "true".equalsIgnoreCase(request.getParameter("autosave"));
    }

    private static BigDecimal decimalParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return parseDecimal(value, name);
    }

    private static BigDecimal optionalDecimal(HttpServletRequest request, String name, BigDecimal defaultValue) {
        String value = text(request, name);
        return value == null ? defaultValue : parseDecimal(value, name);
    }

    private static BigDecimal parseDecimal(String value, String name) {
        String normalized = value == null ? "" : value.trim().replace(',', '.');
        if (normalized.isBlank() || !normalized.matches("\\d+(?:\\.\\d+)?")) {
            throw new IllegalArgumentException(name + " must be a non-negative number");
        }
        return new BigDecimal(normalized);
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static int optionalInteger(HttpServletRequest request, String name, int defaultValue) {
        String value = text(request, name);
        return value == null ? defaultValue : Integer.parseInt(value);
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

    private static LocalDateTime optionalDateTime(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDateTime(value);
    }

    private static boolean checkbox(HttpServletRequest request, String name, boolean defaultValue) {
        String value = request.getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String messageForRuntime(Exception exception) {
        return exception instanceof RuntimeException runtimeException
                ? messageFor(runtimeException)
                : "The operation could not be completed.";
    }

    public static final class ContextSelectOptionView {
        private final String value;
        private final String label;
        private final String title;
        private final boolean selected;
        private final String organizationId;
        private final String organicUnitIds;
        private final String courseIds;
        private final String subjectId;
        private final String classGroupId;
        private final String startsAt;
        private final String endsAt;

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                Long organizationId,
                List<Long> organicUnitIds
        ) {
            this(
                    value,
                    label,
                    title,
                    selected,
                    organizationId == null ? "" : Long.toString(organizationId),
                    joinContextIds(organicUnitIds),
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                Long organizationId,
                List<Long> organicUnitIds,
                List<Long> courseIds,
                String subjectId,
                String classGroupId
        ) {
            this(value, label, title, selected, organizationId, organicUnitIds, courseIds, subjectId, classGroupId, "", "");
        }

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                Long organizationId,
                List<Long> organicUnitIds,
                List<Long> courseIds,
                String subjectId,
                String classGroupId,
                String startsAt,
                String endsAt
        ) {
            this(
                    value,
                    label,
                    title,
                    selected,
                    organizationId == null ? "" : Long.toString(organizationId),
                    joinContextIds(organicUnitIds),
                    joinContextIds(courseIds),
                    subjectId,
                    classGroupId,
                    startsAt,
                    endsAt
            );
        }

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                String organizationId,
                String organicUnitIds
        ) {
            this.value = value;
            this.label = label;
            this.title = title;
            this.selected = selected;
            this.organizationId = organizationId == null ? "" : organizationId;
            this.organicUnitIds = organicUnitIds == null ? "" : organicUnitIds;
            this.courseIds = "";
            this.subjectId = "";
            this.classGroupId = "";
            this.startsAt = "";
            this.endsAt = "";
        }

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                String organizationId,
                String organicUnitIds,
                String courseIds,
                String subjectId,
                String classGroupId
        ) {
            this(value, label, title, selected, organizationId, organicUnitIds, courseIds, subjectId, classGroupId, "", "");
        }

        ContextSelectOptionView(
                String value,
                String label,
                String title,
                boolean selected,
                String organizationId,
                String organicUnitIds,
                String courseIds,
                String subjectId,
                String classGroupId,
                String startsAt,
                String endsAt
        ) {
            this.value = value;
            this.label = label;
            this.title = title;
            this.selected = selected;
            this.organizationId = organizationId == null ? "" : organizationId;
            this.organicUnitIds = organicUnitIds == null ? "" : organicUnitIds;
            this.courseIds = courseIds == null ? "" : courseIds;
            this.subjectId = subjectId == null ? "" : subjectId;
            this.classGroupId = classGroupId == null ? "" : classGroupId;
            this.startsAt = startsAt == null ? "" : startsAt;
            this.endsAt = endsAt == null ? "" : endsAt;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getTitle() {
            return title;
        }

        public boolean isSelected() {
            return selected;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public String getOrganicUnitIds() {
            return organicUnitIds;
        }

        public String getCourseIds() {
            return courseIds;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public String getClassGroupId() {
            return classGroupId;
        }

        public String getStartsAt() {
            return startsAt;
        }

        public String getEndsAt() {
            return endsAt;
        }

        private static String joinContextIds(List<Long> ids) {
            if (ids == null || ids.isEmpty()) {
                return "";
            }
            return ids.stream()
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .map(id -> Long.toString(id))
                    .collect(java.util.stream.Collectors.joining("|", "|", "|"));
        }
    }
}
