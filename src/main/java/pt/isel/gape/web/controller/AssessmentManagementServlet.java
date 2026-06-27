package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.ContentItemDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentEnrollmentCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.ManualCorrectionCommand;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionState;
import pt.isel.gape.learning.model.QuestionOptionUpdateCommand;
import pt.isel.gape.learning.model.QuestionState;
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
import pt.isel.gape.web.view.AssessmentEnrollmentView;
import pt.isel.gape.web.view.AssessmentFormData;
import pt.isel.gape.web.view.AssessmentView;
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
    private static final String ASSESSMENT_CORRECTION_JSP = "/WEB-INF/views/learning/assessment-correction.jsp";

    private final AssessmentService assessmentService;
    private final AssessmentEnrollmentService assessmentEnrollmentService;
    private final AssessmentPdfService assessmentPdfService;
    private final QuestionService questionService;
    private final QuestionOptionService optionService;
    private final CorrectionService correctionService;
    private final AssessmentDAO assessmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final AttemptDAO attemptDAO;
    private final ResponseDAO responseDAO;
    private final SubjectDAO subjectDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final ContentItemDAO contentItemDAO;
    private final ClassGroupDAO classGroupDAO;
    private final UserDAO userDAO;
    private final PdfUploadService pdfUploadService;
    private final AssessmentViewFactory viewFactory;

    public AssessmentManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private AssessmentManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new AssessmentService(connectionProvider, clock),
                new AssessmentEnrollmentService(connectionProvider, clock),
                new AssessmentPdfService(connectionProvider),
                new QuestionService(connectionProvider, clock),
                new QuestionOptionService(connectionProvider, clock),
                new CorrectionService(connectionProvider, clock),
                new AssessmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new ResponseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new ContentItemDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new UserDAO(connectionProvider),
                new PdfUploadService(),
                new AssessmentViewFactory(
                        new AssessmentDAO(connectionProvider),
                        new QuestionDAO(connectionProvider),
                        new QuestionOptionDAO(connectionProvider),
                        new AttemptDAO(connectionProvider),
                        new ResponseDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new ContentBlockDAO(connectionProvider),
                        new ClassGroupDAO(connectionProvider),
                        new UserDAO(connectionProvider)
                )
        );
    }

    AssessmentManagementServlet(
            AssessmentService assessmentService,
            AssessmentEnrollmentService assessmentEnrollmentService,
            AssessmentPdfService assessmentPdfService,
            QuestionService questionService,
            QuestionOptionService optionService,
            CorrectionService correctionService,
            AssessmentDAO assessmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            AttemptDAO attemptDAO,
            ResponseDAO responseDAO,
            SubjectDAO subjectDAO,
            ContentBlockDAO contentBlockDAO,
            ContentItemDAO contentItemDAO,
            ClassGroupDAO classGroupDAO,
            UserDAO userDAO,
            PdfUploadService pdfUploadService,
            AssessmentViewFactory viewFactory
    ) {
        this.assessmentService = assessmentService;
        this.assessmentEnrollmentService = assessmentEnrollmentService;
        this.assessmentPdfService = assessmentPdfService;
        this.questionService = questionService;
        this.optionService = optionService;
        this.correctionService = correctionService;
        this.assessmentDAO = assessmentDAO;
        this.assessmentEnrollmentDAO = assessmentEnrollmentDAO;
        this.questionDAO = questionDAO;
        this.optionDAO = optionDAO;
        this.attemptDAO = attemptDAO;
        this.responseDAO = responseDAO;
        this.subjectDAO = subjectDAO;
        this.contentBlockDAO = contentBlockDAO;
        this.contentItemDAO = contentItemDAO;
        this.classGroupDAO = classGroupDAO;
        this.userDAO = userDAO;
        this.pdfUploadService = pdfUploadService;
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
            if (segments.length == 2 && "attempts".equals(segments[1])) {
                redirect(request, response, "/learning/assessments/" + Long.parseLong(segments[0]) + "#enrollments-attempts");
                return;
            }
            if (segments.length == 3 && "attempts".equals(segments[1])) {
                showCorrection(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
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
                    case "update" -> updateEnrollment(request, response, assessmentId, studentUserId);
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
            request.setAttribute("pendingCorrectionCount",
                    assessments.stream().mapToInt(AssessmentView::getAttemptCount).sum());
            prepareDashboard(request, "assessments", "Assessments", "/learning/assessments/new", "New Assessment");
            forward(request, response, ASSESSMENT_LIST_JSP);
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
        forward(request, response, ASSESSMENT_FORM_JSP);
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
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Assessment assessment = assessmentService.getAssessment(assessmentId);
        AssessmentView assessmentView = viewFactory.assessmentView(assessment);
        boolean hasStartedAttempts = hasAnyAttempts(assessmentId);
        boolean canEditStructure = !assessmentView.isArchived() && !hasStartedAttempts;
        request.setAttribute("assessment", assessmentView);
        request.setAttribute("questions", viewFactory.questionViews(assessmentId));
        request.setAttribute("questionTypes", QuestionType.values());
        request.setAttribute("questionStates", QuestionState.values());
        request.setAttribute("optionStates", QuestionOptionState.values());
        request.setAttribute("nextQuestionOrder", nextQuestionOrder(assessmentId));
        request.setAttribute("attempts", viewFactory.attemptViewsByAssessment(assessmentId));
        prepareAssessmentEnrollmentAttributes(request, assessmentView);
        request.setAttribute("canEditAssessmentStructure", canEditStructure);
        request.setAttribute("hasSubmittedAttempts", hasStartedAttempts);
        request.setAttribute("assessmentStructureLockMessage", hasStartedAttempts
                ? "Assessment questions cannot change after attempts have started"
                : assessmentView.isArchived() ? "Completed assessments cannot be changed." : "");
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareAssessmentContext(request, assessmentView, "detail");
        prepareDashboard(request, "assessments", assessmentView.getTitle());
        forward(request, response, ASSESSMENT_DETAIL_JSP);
    }

    private void showCorrection(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long attemptId
    ) throws ServletException, IOException {
        if (!canManage(request, assessmentId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Attempt attempt;
        try {
            attempt = attemptDAO.findById(attemptId).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load attempt", exception);
        }
        if (attempt == null || attempt.assessmentId() != assessmentId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        AssessmentView assessment = viewFactory.assessmentView(assessmentService.getAssessment(assessmentId));
        request.setAttribute("assessment", assessment);
        request.setAttribute("attempt", viewFactory.attemptView(attempt));
        request.setAttribute("responses", viewFactory.responseViews(attemptId));
        prepareAssessmentContext(request, assessment, "correction");
        prepareDashboard(request, "assessments", "Correct Attempt");
        forward(request, response, ASSESSMENT_CORRECTION_JSP);
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
                writeAssessmentCreated(request, response, assessment.id());
                return;
            }
            flashSuccess(request, "Assessment created.");
            redirect(request, response, "/learning/assessments/" + assessment.id());
        } catch (RuntimeException exception) {
            if (modalCreate) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, messageFor(exception));
                return;
            }
            showForm(request, response, AssessmentFormData.from(request, null), true, messageFor(exception));
        }
    }

    private void updateAssessment(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws ServletException, IOException {
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
            redirect(request, response, "/learning/assessments/" + assessmentId);
        } catch (RuntimeException exception) {
            showForm(request, response, AssessmentFormData.from(request, assessmentId), false, messageFor(exception));
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
            throws IOException {
        try {
            assessmentEnrollmentService.updateEnrollmentMode(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    assessmentId,
                    enrollmentApprovalMode(text(request, "approvalMode")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment policy saved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void enrollStudent(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        try {
            assessmentEnrollmentService.enrollStudentInAssessment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    new AssessmentEnrollmentCommand(
                            longParameter(request, "studentUserId"),
                            assessmentId,
                            optionalDate(request, "startDate"),
                            optionalDate(request, "endDate")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Student enrolled in assessment.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void approveEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException {
        try {
            assessmentEnrollmentService.approveAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    optionalDate(request, "startDate"),
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment approved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void rejectEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException {
        try {
            assessmentEnrollmentService.rejectAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment rejected.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void withdrawEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException {
        try {
            assessmentEnrollmentService.withdrawAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment withdrawn.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void updateEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException {
        try {
            assessmentEnrollmentService.updateAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    enrollmentState(text(request, "state")),
                    optionalDate(request, "startDate"),
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment updated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private void deleteEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId,
            long studentUserId
    ) throws IOException {
        try {
            assessmentEnrollmentService.deleteAssessmentEnrollment(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    studentUserId,
                    assessmentId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/learning/assessments/" + assessmentId + "#enrollments-attempts");
    }

    private static boolean isModalCreate(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter("modalCreate"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static void writeAssessmentCreated(
            HttpServletRequest request,
            HttpServletResponse response,
            long assessmentId
    ) throws IOException {
        String location = request.getContextPath() + "/learning/assessments/" + assessmentId;
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("Location", location);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(location);
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
        try {
            Question question = questionService.getQuestion(questionId);
            questionService.updateQuestion(
                    actorUserId(request),
                    currentSessionId(request),
                    primaryProfile(requireCurrentUser(request)),
                    questionId,
                    new QuestionUpdateCommand(
                            question.code(),
                            question.statement(),
                            question.type(),
                            question.orderNo(),
                            question.required(),
                            question.score(),
                            question.expectedAnswer(),
                            QuestionState.INACTIVE
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Question deactivated.");
            redirect(request, response, "/learning/assessments/" + assessmentId);
        } catch (RuntimeException exception) {
            showDetail(request, response, assessmentId, messageFor(exception));
        }
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
            flashSuccess(request, "Automatic correction completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/assessments/" + assessmentId + "/attempts/" + attemptId);
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
            flashSuccess(request, "Response score saved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/assessments/" + assessmentId + "/attempts/" + attemptId);
    }

    private void prepareForm(
            HttpServletRequest request,
            AssessmentFormData form,
            boolean creating,
            String error
    ) throws ServletException {
        prepareAssessmentFormAttributes(request, form, creating, error);
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
        request.setAttribute("subjectOptions", subjectOptions(request, form.getSubjectId(), creating));
        request.setAttribute("contentBlockOptions", contentBlockOptions(request, form.getContentBlockId()));
        request.setAttribute("classGroupOptions", classGroupOptions(request, form.getClassGroupIds(), creating));
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
            if (assessment.isAutomaticEnrollment()) {
                assessmentEnrollmentDAO.syncAutomaticEnrollments(assessment.getId(), LocalDate.now());
            }
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
            Map<Long, Boolean> activeEnrollmentByStudent = new LinkedHashMap<>();
            for (AssessmentEnrollmentView enrollment : enrollments) {
                if (enrollment.isActive() || enrollment.isPending()) {
                    activeEnrollmentByStudent.put(enrollment.getStudentUserId(), true);
                }
            }
            List<UserOptionView> eligibleStudents = eligibleAssessmentStudentOptions(assessment.getId());
            request.setAttribute("assessmentEnrollments", enrollments);
            request.setAttribute("pendingAssessmentEnrollments", pendingEnrollments);
            request.setAttribute("activeAssessmentEnrollments", activeEnrollments);
            request.setAttribute("auditAssessmentEnrollments", auditEnrollments);
            request.setAttribute("assessmentStudentOptions", eligibleStudents);
            request.setAttribute("assessmentActiveEnrollmentByStudent", activeEnrollmentByStudent);
            request.setAttribute("canManageAssessmentEnrollments", true);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment enrollments", exception);
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

    private List<SelectOptionView> subjectOptions(
            HttpServletRequest request,
            String selectedSubjectId,
            boolean creating
    )
            throws ServletException {
        try {
            return subjectDAO.findAll().stream()
                    .filter(subject -> subject.state() == pt.isel.gape.learning.model.SubjectState.ACTIVE
                            || (!creating && Long.toString(subject.id()).equals(selectedSubjectId)))
                    .filter(subject -> (!creating && Long.toString(subject.id()).equals(selectedSubjectId))
                            || canManageContext(request, subject.id(), null))
                    .map(subject -> new SelectOptionView(
                            Long.toString(subject.id()),
                            subjectLabel(subject),
                            subject.name(),
                            Long.toString(subject.id()).equals(selectedSubjectId)
                    ))
                    .toList();
        } catch (SQLException exception) {
            throw new ServletException("Failed to load subject options", exception);
        }
    }

    private List<SelectOptionView> contentBlockOptions(HttpServletRequest request, String selectedContentBlockId)
            throws ServletException {
        try {
            return classGroupDAO.findAll().stream()
                    .filter(classGroup -> canManageContext(request, classGroup.subjectId(), classGroup.id()))
                    .sorted(Comparator.comparing(pt.isel.gape.learning.model.ClassGroup::code))
                    .flatMap(classGroup -> {
                        try {
                            return contentBlockDAO.findByClassGroup(classGroup.id()).stream()
                                    .filter(block -> block.state() != pt.isel.gape.learning.model.ContentBlockState.INACTIVE)
                                    .map(block -> new SelectOptionView(
                                            Long.toString(block.id()),
                                            classGroup.code() + " | " + block.name(),
                                            block.name(),
                                            Long.toString(block.id()).equals(selectedContentBlockId)
                                    ));
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw new ServletException("Failed to load content block options", exception);
        }
    }

    private List<SelectOptionView> classGroupOptions(
            HttpServletRequest request,
            List<String> selectedClassGroupIds,
            boolean creating
    )
            throws ServletException {
        List<String> selected = selectedClassGroupIds == null ? List.of() : selectedClassGroupIds;
        try {
            List<SelectOptionView> options = new ArrayList<>();
            for (pt.isel.gape.learning.model.ClassGroup classGroup : classGroupDAO.findAll().stream()
                    .filter(item -> item.state() == pt.isel.gape.learning.model.ClassGroupState.ACTIVE
                            || (!creating && selected.contains(Long.toString(item.id()))))
                    .filter(item -> (!creating && selected.contains(Long.toString(item.id())))
                            || canManageContext(request, item.subjectId(), item.id()))
                    .sorted(Comparator.comparing(pt.isel.gape.learning.model.ClassGroup::code))
                    .toList()) {
                Subject subject = subjectDAO.findById(classGroup.subjectId()).orElse(null);
                String subjectLabel = subject == null ? "Subject " + classGroup.subjectId() : subjectLabel(subject);
                String value = Long.toString(classGroup.id());
                options.add(new SelectOptionView(
                        value,
                        classGroup.code() + " | " + subjectLabel,
                        classGroup.code() + " | " + subjectLabel,
                        selected.contains(value)
                ));
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load class group options", exception);
        }
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
                text(request, "title"),
                text(request, "description"),
                assessmentType(text(request, "type")),
                mode,
                correctionMode,
                decimalParameter(request, "maxGrade"),
                decimalParameter(request, "passingGrade"),
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
                text(request, "title"),
                text(request, "description"),
                assessmentType(text(request, "type")),
                mode,
                correctionMode,
                decimalParameter(request, "maxGrade"),
                decimalParameter(request, "passingGrade"),
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
                expectedAnswerForRequest(request, type),
                QuestionState.ACTIVE
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
                expectedAnswerForRequest(request, current.type()),
                current.state()
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
                "new".equals(selectedSingleOption) || (selectedSingleOption == null && checkbox(request, "correct", false)),
                QuestionOptionState.ACTIVE
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
                        true,
                        QuestionOptionState.ACTIVE
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
                checkbox(request, "correct", false),
                current.state()
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
                            correct,
                            option.state()
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

    private static EnrollmentApprovalMode enrollmentApprovalMode(String value) {
        return value == null || value.isBlank()
                ? EnrollmentApprovalMode.AUTO_APPROVE
                : EnrollmentApprovalMode.parse(value);
    }

    private static EnrollmentState enrollmentState(String value) {
        return value == null || value.isBlank() ? EnrollmentState.ACTIVE : EnrollmentState.parse(value);
    }

    private static QuestionType questionType(String value) {
        return value == null ? QuestionType.SINGLE_CHOICE : QuestionType.parse(value);
    }

    private static QuestionState questionState(String value) {
        return value == null ? QuestionState.ACTIVE : QuestionState.parse(value);
    }

    private static QuestionOptionState optionState(String value) {
        return value == null ? QuestionOptionState.ACTIVE : QuestionOptionState.parse(value);
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

    private static String attachmentFileName(String attachment) {
        String normalized = attachment.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private static String headerSafeFilename(String filename) {
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
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
        return new BigDecimal(value);
    }

    private static BigDecimal optionalDecimal(HttpServletRequest request, String name, BigDecimal defaultValue) {
        String value = text(request, name);
        return value == null ? defaultValue : new BigDecimal(value);
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
        return value == null ? null : LocalDateTime.parse(value);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
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
}
