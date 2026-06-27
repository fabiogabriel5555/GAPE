package pt.isel.gape.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentEnrollmentCommand;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.ResponseCommand;
import pt.isel.gape.learning.model.UploadedContentFile;
import pt.isel.gape.learning.service.AssessmentEnrollmentService;
import pt.isel.gape.learning.service.AttemptService;
import pt.isel.gape.learning.service.PdfUploadService;
import pt.isel.gape.learning.service.ResponseService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.AttemptView;
import pt.isel.gape.web.view.QuestionOptionView;
import pt.isel.gape.web.view.QuestionView;
import pt.isel.gape.web.view.ResponseView;

@WebServlet(name = "studentAssessmentServlet", urlPatterns = {
        "/student/assessments",
        "/student/assessments/*"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 2L * 1024L * 1024L * 1024L,
        maxRequestSize = 2L * 1024L * 1024L * 1024L + 16L * 1024L * 1024L
)
public final class StudentAssessmentServlet extends DashboardServletSupport {

    private static final String STUDENT_ASSESSMENTS_JSP = "/WEB-INF/views/student/assessment-list.jsp";
    private static final String STUDENT_ATTEMPT_JSP = "/WEB-INF/views/student/assessment-attempt.jsp";
    private static final String STUDENT_RESULT_JSP = "/WEB-INF/views/student/assessment-result.jsp";

    private final AttemptService attemptService;
    private final AssessmentEnrollmentService assessmentEnrollmentService;
    private final ResponseService responseService;
    private final PdfUploadService pdfUploadService;
    private final AssessmentDAO assessmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final AttemptDAO attemptDAO;
    private final ResponseDAO responseDAO;
    private final AssessmentViewFactory viewFactory;

    public StudentAssessmentServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private StudentAssessmentServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new AttemptService(connectionProvider, clock),
                new AssessmentEnrollmentService(connectionProvider, clock),
                new ResponseService(connectionProvider, clock),
                new PdfUploadService(),
                new AssessmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new ResponseDAO(connectionProvider),
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

    StudentAssessmentServlet(
            AttemptService attemptService,
            ResponseService responseService,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            AssessmentViewFactory viewFactory
    ) {
        this(
                attemptService,
                new AssessmentEnrollmentService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                responseService,
                new PdfUploadService(),
                assessmentDAO,
                new AssessmentEnrollmentDAO(ConnectionProvider.defaultProvider()),
                attemptDAO,
                new ResponseDAO(ConnectionProvider.defaultProvider()),
                viewFactory
        );
    }

    StudentAssessmentServlet(
            AttemptService attemptService,
            AssessmentEnrollmentService assessmentEnrollmentService,
            ResponseService responseService,
            PdfUploadService pdfUploadService,
            AssessmentDAO assessmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            AttemptDAO attemptDAO,
            ResponseDAO responseDAO,
            AssessmentViewFactory viewFactory
    ) {
        this.attemptService = attemptService;
        this.assessmentEnrollmentService = assessmentEnrollmentService;
        this.responseService = responseService;
        this.pdfUploadService = pdfUploadService;
        this.assessmentDAO = assessmentDAO;
        this.assessmentEnrollmentDAO = assessmentEnrollmentDAO;
        this.attemptDAO = attemptDAO;
        this.responseDAO = responseDAO;
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
            if (segments.length == 2 && "attempts".equals(segments[0])) {
                showAttempt(request, response, Long.parseLong(segments[1]), null);
                return;
            }
            if (segments.length == 3 && "attempts".equals(segments[0]) && "result".equals(segments[2])) {
                showResult(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 3 && "responses".equals(segments[0]) && "attachment".equals(segments[2])) {
                downloadResponseAttachment(request, response, Long.parseLong(segments[1]));
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
            if (segments.length == 2 && "enroll".equals(segments[1])) {
                requestEnrollment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "start".equals(segments[1])) {
                startAttempt(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 3 && "attempts".equals(segments[0]) && "responses".equals(segments[2])) {
                saveResponses(request, response, Long.parseLong(segments[1]), false);
                return;
            }
            if (segments.length == 3 && "attempts".equals(segments[0]) && "submit".equals(segments[2])) {
                saveResponses(request, response, Long.parseLong(segments[1]), true);
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
            assessmentEnrollmentDAO.syncAllAutomaticEnrollments(LocalDate.now());
            List<AssessmentView> assessments = viewFactory.assessmentViews(
                    assessmentDAO.findActiveAccessibleByStudent(actor.userId())
            );
            Map<Long, AttemptView> latestAttemptByAssessment = latestAttemptByAssessment(actor.userId());
            Map<Long, AssessmentEnrollment> enrollmentByAssessment = assessmentEnrollmentByAssessment(actor.userId(), assessments);
            request.setAttribute("assessments", assessments);
            request.setAttribute("latestAttemptByAssessment", latestAttemptByAssessment);
            request.setAttribute("assessmentEnrollmentByAssessment", enrollmentByAssessment);
            request.setAttribute("assessmentEnrollmentStateByAssessment", assessmentEnrollmentStateByAssessment(enrollmentByAssessment));
            request.setAttribute("assessmentCount", assessments.size());
            request.setAttribute("availableForms", assessments.stream().filter(AssessmentView::isForm).count());
            request.setAttribute("availableTests", assessments.stream().filter(AssessmentView::isTest).count());
            request.setAttribute("availableExams", assessments.stream().filter(AssessmentView::isExam).count());
            request.setAttribute("studentPageTitle", "Assessments");
            request.setAttribute("studentPageDescription", "Forms, tests, exams, attempts and results available to your profile.");
            prepareDashboard(request, "assessments", "Assessments");
            forward(request, response, STUDENT_ASSESSMENTS_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student assessments", exception);
        }
    }

    private void requestEnrollment(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            assessmentEnrollmentService.requestStudentInAssessment(
                    actor.userId(),
                    currentSessionId(request),
                    new AssessmentEnrollmentCommand(
                            actor.userId(),
                            assessmentId,
                            null,
                            null
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Assessment enrollment requested.");
        } catch (RuntimeException exception) {
            flashError(request, messageForRuntime(exception));
        }
        redirect(request, response, "/student/assessments");
    }

    private void startAttempt(HttpServletRequest request, HttpServletResponse response, long assessmentId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Attempt existing = attemptDAO.findLatestInProgress(actor.userId(), assessmentId).orElse(null);
            if (existing != null) {
                redirect(request, response, "/student/assessments/attempts/" + existing.id());
                return;
            }
            Attempt attempt = attemptService.startAttempt(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    assessmentId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Attempt started.");
            redirect(request, response, "/student/assessments/attempts/" + attempt.id());
        } catch (RuntimeException | SQLException exception) {
            flashError(request, messageForRuntime(exception));
            redirect(request, response, "/student/assessments");
        }
    }

    private void showAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            long attemptId,
            String error
    ) throws ServletException, IOException {
        Attempt attempt;
        try {
            SessionUser actor = requireCurrentUser(request);
            attempt = attemptService.validateInProgressAttemptAccess(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    attemptId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            flashError(request, messageForRuntime(exception));
            redirect(request, response, "/student/assessments");
            return;
        }
        if (attempt.state() != AttemptState.IN_PROGRESS) {
            redirect(request, response, "/student/assessments/attempts/" + attempt.id() + "/result");
            return;
        }
        AssessmentView assessment = assessmentView(attempt.assessmentId());
        List<QuestionView> questions = viewFactory.questionViews(attempt.assessmentId()).stream()
                .filter(QuestionView::isActive)
                .toList();
        List<ResponseView> responses = viewFactory.responseViews(attempt.id());
        request.setAttribute("assessment", assessment);
        request.setAttribute("attempt", viewFactory.attemptView(attempt));
        request.setAttribute("questions", questions);
        request.setAttribute("responses", responses);
        request.setAttribute("responseByQuestionId", responseByQuestionId(responses));
        request.setAttribute("selectedOptionIdsByQuestionId", selectedOptionIdsByQuestionId(responses));
        request.setAttribute("uploadLimitLabelByQuestionId", uploadLimitLabelsByQuestionId(questions));
        request.setAttribute("studentPageTitle", assessment.getTitle());
        request.setAttribute("studentPageDescription", "Answer and submit your assessment attempt.");
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "assessments", assessment.getTitle());
        forward(request, response, STUDENT_ATTEMPT_JSP);
    }

    private void showResult(HttpServletRequest request, HttpServletResponse response, long attemptId)
            throws ServletException, IOException {
        Attempt attempt = loadOwnedAttempt(request, response, attemptId);
        if (attempt == null) {
            return;
        }
        AssessmentView assessment = assessmentView(attempt.assessmentId());
        request.setAttribute("assessment", assessment);
        request.setAttribute("attempt", viewFactory.attemptView(attempt));
        request.setAttribute("responses", viewFactory.responseViews(attempt.id()));
        request.setAttribute("studentPageTitle", "Assessment Result");
        request.setAttribute("studentPageDescription", "Attempt status, answers and correction result.");
        prepareDashboard(request, "assessments", "Assessment Result");
        forward(request, response, STUDENT_RESULT_JSP);
    }

    private void saveResponses(
            HttpServletRequest request,
            HttpServletResponse response,
            long attemptId,
            boolean submit
    ) throws ServletException, IOException {
        Attempt attempt;
        try {
            SessionUser actor = requireCurrentUser(request);
            attempt = attemptService.validateInProgressAttemptAccess(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    attemptId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            showAttempt(request, response, attemptId, messageForResponseSave(exception));
            return;
        }
        try {
            for (QuestionView question : viewFactory.questionViews(attempt.assessmentId()).stream()
                    .filter(QuestionView::isActive)
                    .toList()) {
                ResponsePayload payload = responsePayload(request, question);
                if (!payload.hasAnyValue() && !question.isRequired()) {
                    continue;
                }
                responseService.saveResponse(
                        requireCurrentUser(request).userId(),
                        currentSessionId(request),
                        AccessProfileType.STUDENT,
                        attemptId,
                        new ResponseCommand(
                                question.getId(),
                                payload.answer(),
                                payload.attachment(),
                                payload.optionIds(),
                                payload.uploadedAttachment()
                        ),
                        request.getRemoteAddr()
                );
            }
            if (submit) {
                attemptService.submitAttempt(
                        requireCurrentUser(request).userId(),
                        currentSessionId(request),
                        AccessProfileType.STUDENT,
                        attemptId,
                        request.getRemoteAddr()
                );
                flashSuccess(request, "Attempt submitted.");
                redirect(request, response, "/student/assessments/attempts/" + attemptId + "/result");
                return;
            }
            flashSuccess(request, "Responses saved.");
            redirect(request, response, "/student/assessments/attempts/" + attemptId);
        } catch (RuntimeException | IOException | ServletException exception) {
            showAttempt(request, response, attemptId, messageForResponseSave(exception));
        }
    }

    private Attempt loadOwnedAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            long attemptId
    ) throws IOException, ServletException {
        try {
            Attempt attempt = attemptDAO.findById(attemptId).orElse(null);
            if (attempt == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
            if (attempt.studentUserId() != requireCurrentUser(request).userId()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            return attempt;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load attempt", exception);
        }
    }

    private AssessmentView assessmentView(long assessmentId) throws ServletException {
        try {
            Assessment assessment = assessmentDAO.findById(assessmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
            return viewFactory.assessmentView(assessment);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment", exception);
        }
    }

    private Map<Long, AttemptView> latestAttemptByAssessment(long studentUserId) throws ServletException {
        try {
            Map<Long, AttemptView> result = new LinkedHashMap<>();
            for (Attempt attempt : attemptDAO.findByStudent(studentUserId)) {
                result.putIfAbsent(attempt.assessmentId(), viewFactory.attemptView(attempt));
            }
            return result;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student attempts", exception);
        }
    }

    private Map<Long, AssessmentEnrollment> assessmentEnrollmentByAssessment(
            long studentUserId,
            List<AssessmentView> assessments
    ) throws ServletException {
        try {
            Map<Long, AssessmentEnrollment> result = new LinkedHashMap<>();
            for (AssessmentView assessment : assessments) {
                assessmentEnrollmentDAO.findEnrollment(studentUserId, assessment.getId())
                        .ifPresent(enrollment -> result.put(assessment.getId(), enrollment));
            }
            return result;
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student assessment enrollments", exception);
        }
    }

    private static Map<Long, String> assessmentEnrollmentStateByAssessment(
            Map<Long, AssessmentEnrollment> enrollmentByAssessment
    ) {
        Map<Long, String> result = new LinkedHashMap<>();
        enrollmentByAssessment.forEach((assessmentId, enrollment) -> result.put(assessmentId, enrollment.state().name()));
        return result;
    }

    private static Map<Long, ResponseView> responseByQuestionId(List<ResponseView> responses) {
        Map<Long, ResponseView> result = new HashMap<>();
        for (ResponseView response : responses) {
            result.put(response.getQuestionId(), response);
        }
        return result;
    }

    private static Map<Long, Set<Long>> selectedOptionIdsByQuestionId(List<ResponseView> responses) {
        Map<Long, Set<Long>> result = new HashMap<>();
        for (ResponseView response : responses) {
            Set<Long> optionIds = new HashSet<>();
            for (QuestionOptionView option : response.getSelectedOptions()) {
                optionIds.add(option.getId());
            }
            result.put(response.getQuestionId(), optionIds);
        }
        return result;
    }

    private ResponsePayload responsePayload(HttpServletRequest request, QuestionView question)
            throws IOException, ServletException {
        if (question.isAllowsOptions()) {
            String[] selectedValues = request.getParameterValues("question_" + question.getId() + "_option");
            List<Long> optionIds = selectedValues == null
                    ? List.of()
                    : java.util.Arrays.stream(selectedValues)
                            .filter(value -> value != null && !value.isBlank())
                            .map(Long::parseLong)
                            .toList();
            return new ResponsePayload(null, null, optionIds, false);
        }
        if (question.isFileUpload()) {
            String inputName = "question_" + question.getId() + "_file";
            Part filePart = submittedFilePart(request, inputName);
            if (filePart == null) {
                return new ResponsePayload(
                        null,
                        text(request, "question_" + question.getId() + "_attachment_existing"),
                        List.of(),
                        false
                );
            }
            ContentFormat format = detectAcceptedFormat(question, filePart);
            UploadedContentFile uploadedFile = pdfUploadService.saveContentFile(format, filePart);
            return new ResponsePayload(null, uploadedFile.relativePath(), List.of(), true);
        }
        String answer = text(request, "question_" + question.getId() + "_answer");
        return new ResponsePayload(answer, null, List.of(), false);
    }

    private void downloadResponseAttachment(
            HttpServletRequest request,
            HttpServletResponse response,
            long responseId
    ) throws IOException, ServletException {
        Response storedResponse;
        Attempt attempt;
        try {
            storedResponse = responseDAO.findById(responseId).orElse(null);
            if (storedResponse == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            attempt = attemptDAO.findById(storedResponse.attemptId()).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load response attachment", exception);
        }
        if (attempt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (attempt.studentUserId() != requireCurrentUser(request).userId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
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

    private static String messageForRuntime(Exception exception) {
        return exception instanceof RuntimeException runtimeException
                ? messageFor(runtimeException)
                : "The operation could not be completed.";
    }

    private static String messageForResponseSave(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return messageFor(runtimeException);
        }
        return "The uploaded file could not be processed. Please check the file type and try again.";
    }

    private static Part submittedFilePart(HttpServletRequest request, String name) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return null;
        }
        Part part = request.getPart(name);
        if (part == null || part.getSize() <= 0) {
            return null;
        }
        return part;
    }

    private static ContentFormat detectAcceptedFormat(QuestionView question, Part part) {
        Set<ContentFormat> acceptedFormats = Set.copyOf(question.getAcceptedFileFormats());
        String fileName = part.getSubmittedFileName() == null
                ? ""
                : part.getSubmittedFileName().toLowerCase(Locale.ROOT);
        String contentType = part.getContentType() == null
                ? ""
                : part.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        for (ContentFormat format : QuestionConfiguration.responseFileFormats()) {
            if (acceptedFormats.contains(format) && matchesFormat(format, fileName, contentType)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Uploaded file type is not accepted by this question");
    }

    private static boolean matchesFormat(ContentFormat format, String fileName, String contentType) {
        return switch (format) {
            case PDF -> fileName.endsWith(".pdf") || "application/pdf".equals(contentType);
            case TEXT -> fileName.endsWith(".txt") || "text/plain".equals(contentType);
            case IMAGE -> endsWithAny(fileName, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
                    || contentType.startsWith("image/");
            case VIDEO -> endsWithAny(fileName, ".mp4", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".mpeg", ".mpg", ".3gp", ".3gpp")
                    || contentType.startsWith("video/");
            case AUDIO -> endsWithAny(fileName, ".mp3", ".m4a", ".wav", ".ogg", ".flac", ".aac", ".weba", ".opus")
                    || contentType.startsWith("audio/")
                    || "application/ogg".equals(contentType)
                    || "application/x-ogg".equals(contentType);
            case ARCHIVE -> endsWithAny(fileName, ".zip", ".rar", ".7z", ".tar", ".tar.gz", ".tgz", ".tar.bz2",
                    ".tbz2", ".tar.xz", ".txz", ".gz", ".bz2", ".xz")
                    || isArchiveContentType(contentType);
            default -> false;
        };
    }

    private Map<Long, String> uploadLimitLabelsByQuestionId(List<QuestionView> questions) {
        Map<Long, String> labels = new HashMap<>();
        for (QuestionView question : questions) {
            if (question.isFileUpload()) {
                labels.put(question.getId(), uploadLimitLabel(question.getAcceptedFileFormats()));
            }
        }
        return labels;
    }

    private String uploadLimitLabel(List<ContentFormat> formats) {
        List<String> labels = new ArrayList<>();
        for (ContentFormat format : formats) {
            labels.add(QuestionConfiguration.fileFormatLabel(format) + " " + readableBytes(pdfUploadService.maxBytes(format)));
        }
        return String.join(", ", labels);
    }

    private static String readableBytes(long bytes) {
        long mb = 1024L * 1024L;
        long gb = 1024L * mb;
        if (bytes % gb == 0) {
            return (bytes / gb) + " GB";
        }
        if (bytes % mb == 0) {
            return (bytes / mb) + " MB";
        }
        return bytes + " bytes";
    }

    private static boolean isArchiveContentType(String contentType) {
        return "application/zip".equals(contentType)
                || "application/x-zip-compressed".equals(contentType)
                || "application/vnd.rar".equals(contentType)
                || "application/x-rar-compressed".equals(contentType)
                || "application/x-7z-compressed".equals(contentType)
                || "application/x-tar".equals(contentType)
                || "application/gzip".equals(contentType)
                || "application/x-gzip".equals(contentType)
                || "application/x-bzip2".equals(contentType)
                || "application/x-xz".equals(contentType);
    }

    private static boolean endsWithAny(String value, String... suffixes) {
        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String attachmentFileName(String attachment) {
        String normalized = attachment.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private static String headerSafeFilename(String filename) {
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private record ResponsePayload(String answer, String attachment, List<Long> optionIds, boolean uploadedAttachment) {
        boolean hasAnyValue() {
            return (answer != null && !answer.isBlank())
                    || (attachment != null && !attachment.isBlank())
                    || (optionIds != null && !optionIds.isEmpty());
        }
    }
}
