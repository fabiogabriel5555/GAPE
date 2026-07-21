package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationState;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.StudentClassGroupView;
import pt.isel.gape.web.view.SubjectView;

/**
 * Fast, database-free contract checks for the student presentation shell and
 * its navigation. Browser validation remains the final visual check, while
 * these assertions protect the shared routes and JSP composition.
 */
class StudentExperienceTemplateTest {

    @Test
    void userAdministrationKeepsStudentCourseEnrollmentOutsideTheUserForm() throws IOException {
        String userForm = source("src/main/webapp/admin/admin/user/admin-user-form.jsp");
        String userServlet = source("src/main/java/pt/isel/gape/web/controller/UserManagementServlet.java");
        String formData = source("src/main/java/pt/isel/gape/web/view/UserFormData.java");
        String userService = source("src/main/java/pt/isel/gape/access/service/UserService.java");

        assertFalse(userForm.contains("Student Course Context"));
        assertFalse(userServlet.contains("studentContextOptions("));
        assertFalse(userServlet.contains("findActiveCourseIdsByStudent"));
        assertTrue(formData.contains("profileType == AccessProfileType.STUDENT"));
        assertTrue(userService.contains("rejectStudentCourseContextAssignments"));
    }

    @Test
    void studentDashboardAndMessagesUseTheSharedStudentShell() throws IOException {
        String dashboardServlet = source("src/main/java/pt/isel/gape/web/controller/DashboardServlet.java");
        String studentDashboard = source("src/main/webapp/student/student/dashboard/student-dashboard.jsp");
        String sessionManager = source("src/main/java/pt/isel/gape/security/session/SessionManager.java");
        String studentStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");
        String studentSidebar = source("src/main/webapp/WEB-INF/fragments/student-dashboard-sidebar.jspf");
        String messages = source("src/main/webapp/WEB-INF/views/transversal/messages.jsp");

        assertTrue(dashboardServlet.contains("STUDENT_DASHBOARD_JSP"));
        assertTrue(dashboardServlet.contains("isStudentOnly(currentSession.user())"));
        assertTrue(dashboardServlet.contains("forward(request, response, STUDENT_DASHBOARD_JSP)"));
        assertTrue(sessionManager.contains("SESSION_USER_ID_ATTRIBUTE"));
        assertTrue(studentStart.contains("breadcrumb pt-80 pb-187 bg-neutral-900 position-relative z-1 overflow-hidden mb-0 z-n1 gape-student-hero-band"));
        assertTrue(studentStart.contains("overscroll-behavior-y: none;")
                && messages.contains("overscroll-behavior-y: none;"));
        assertTrue(dashboardServlet.contains("StudentDashboardService")
                && dashboardServlet.contains("studentDashboardService.load"));
        assertTrue(studentDashboard.contains("gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10")
                && studentDashboard.contains("student-activity-chart")
                && studentDashboard.contains("student-attendance-chart")
                && studentDashboard.contains("new ApexCharts"));
        assertTrue(studentSidebar.contains("#<c:out value=\"${studentId}\"/> -"));
        assertTrue(studentSidebar.contains("Lessons &amp; Assessments"));
        assertFalse(studentSidebar.contains("data-menu-key=\"assessments\""));
        assertTrue(messages.contains("<c:when test=\"${studentMessagesPage}\">"));
        assertTrue(messages.contains("messages-student-shell"));
        assertTrue(messages.contains("gape-student-hero-band"));
        assertTrue(messages.contains("/WEB-INF/fragments/student-dashboard-sidebar.jspf"));
    }

    @Test
    void lessonsAndAssessmentsAreOneStudentPageWithCompatibilityRedirect() throws IOException {
        String lessonServlet = source("src/main/java/pt/isel/gape/web/controller/StudentLessonServlet.java");
        String assessmentServlet = source("src/main/java/pt/isel/gape/web/controller/StudentAssessmentServlet.java");
        String lessonPage = source("src/main/webapp/student/student/lesson/student-lessons.jsp");
        String assessmentCatalog = source("src/main/webapp/WEB-INF/fragments/student-assessment-catalog.jspf");
        String assessmentGroups = source("src/main/webapp/WEB-INF/fragments/student-assessment-catalog-groups.jspf");
        String attendanceGroups = source("src/main/webapp/WEB-INF/fragments/student-attendance-catalog-groups.jspf");
        String studentDashboardStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");

        assertTrue(lessonServlet.contains("assessmentCatalogSupport.prepare(request, actor.userId())"));
        assertTrue(lessonServlet.contains("Lessons & Assessments"));
        assertTrue(assessmentServlet.contains("redirect(request, response, \"/student/lessons#assessments\")"));
        assertTrue(lessonPage.contains("id=\"lessons\""));
        assertTrue(lessonPage.contains("/WEB-INF/fragments/student-assessment-catalog.jspf"));
        assertTrue(lessonPage.contains("window.location.pathname + window.location.search + '#' + name"));
        assertTrue(lessonPage.contains("data-student-lessons-modern")
                && lessonPage.contains("occurrenceGroup.classGroups")
                && lessonPage.contains("gape-student-lesson-class-group")
                && lessonPage.contains("studentLessonModernDetail")
                && lessonPage.contains("View lesson details"));
        assertTrue(lessonServlet.contains("StudentLessonOccurrenceGroupView")
                && lessonServlet.contains("getCourseOccurrenceId()")
                && lessonServlet.contains("addOccurrenceGroup")
                && lessonServlet.contains("lessonCompletedCourseGroups"));
        assertTrue(lessonPage.contains("data-student-completed-lessons")
                && lessonPage.contains("Completed Lessons")
                && lessonPage.contains("completedLessonCount")
                && lessonPage.contains("data-student-completed-lessons-toggle")
                && lessonPage.contains("data-student-completed-lessons-panel")
                && lessonPage.contains("Hide completed lessons"));
        assertTrue(assessmentCatalog.contains("<section id=\"assessments\"")
                && assessmentCatalog.contains("data-student-assessments-modern")
                && assessmentCatalog.contains("data-student-completed-assessments")
                && assessmentCatalog.contains("assessmentCompletedCourseGroups")
                && assessmentCatalog.contains("Open events")
                && assessmentGroups.contains("occurrenceGroup.classGroups")
                && assessmentGroups.contains("gape-student-assessment-class-group")
                && assessmentGroups.contains("studentAssessmentModernDetail")
                && assessmentGroups.contains("View assessment details")
                && assessmentGroups.contains("gape-student-enrollment-period"));
        assertTrue(lessonServlet.contains("StudentAttendanceOccurrenceGroupView"), "attendance occurrence view");
        assertTrue(lessonServlet.contains("occurrenceGroup.addClassGroup(created)"), "attendance occurrence class grouping");
        assertTrue(lessonServlet.contains("attendanceCompletedCourseGroups"), "completed attendance groups");
        assertTrue(lessonServlet.contains("completedAttendanceRecords"), "completed attendance records");
        assertTrue(lessonServlet.contains("isCompletedAttendanceStatus"), "completed attendance predicate");
        assertTrue(lessonServlet.contains("AbsenceJustificationService"), "justification service");
        assertTrue(lessonServlet.contains("listOwnJustifications"), "own justifications");
        assertTrue(lessonServlet.contains("AbsenceJustificationState"), "justification states");
        assertTrue(lessonServlet.contains("justificationStates"), "justification state map");
        assertTrue(lessonServlet.contains("justified"), "justified status");
        assertTrue(lessonPage.contains("data-student-attendance-modern")
                && lessonPage.contains("student-attendance-catalog-groups.jspf")
                && lessonPage.contains("Presence, absence and permanence records grouped by course")
                && attendanceGroups.contains("gape-student-attendance-modal")
                && attendanceGroups.contains("View attendance")
                && attendanceGroups.contains("Request attendance justification")
                && attendanceGroups.contains("student/attendance/justifications")
                && attendanceGroups.contains("enctype=\"multipart/form-data\"")
                && attendanceGroups.contains("attachmentFile")
                && attendanceGroups.contains("record.canSubmitJustification")
                && attendanceGroups.contains("record.hasJustification")
                && attendanceGroups.contains("data-justification-pending")
                && attendanceGroups.contains("Attendance justification pending")
                && attendanceGroups.contains("record.justification.stateLabel")
                && attendanceGroups.contains("record.justification.reason")
                && attendanceGroups.contains("record.justification.decisionNotes")
                && attendanceGroups.contains("occurrenceGroup.classGroups")
                && lessonPage.contains("Completed Attendance")
                && lessonPage.contains("Present and justified attendance records")
                && lessonPage.contains("Present / Justified")
                && lessonPage.contains("data-student-completed-attendance")
                && lessonPage.contains("data-student-completed-attendance-toggle")
                && lessonPage.contains("data-student-completed-attendance-panel")
                && studentDashboardStart.contains("gape-student-dashboard-main > .alert.alert-success")
                && studentDashboardStart.contains("data-student-completed-attendance")
                && studentDashboardStart.contains("background: #fff !important"));
    }

    @Test
    void studentClassGroupGradeSheetAndActionEventsUseTheSharedReadReceiptFlow() throws IOException {
        String classGroupDetail = source("src/main/webapp/student/student/class-group/student-class-group-detail.jsp");
        String classGroupCard = source("src/main/webapp/WEB-INF/fragments/student-class-group-card.jspf");
        String calendar = source("src/main/webapp/student/student/calendar/student-calendar.jsp");
        String attendance = source("src/main/webapp/student/student/attendance/student-attendance.jsp");
        String certificateCard = source("src/main/webapp/WEB-INF/fragments/student-certificate-card.jspf");
        String studentDashboardStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");
        String sidebar = source("src/main/webapp/WEB-INF/fragments/student-dashboard-sidebar.jspf");
        String studentFooter = source("src/main/webapp/WEB-INF/fragments/student-template-footer.jspf");
        String eventReadScript = source("src/main/webapp/WEB-INF/fragments/event-read-receipt-script.jspf");
        String events = source("src/main/java/pt/isel/gape/learning/dao/LearningEventDAO.java");
        String filter = source("src/main/java/pt/isel/gape/web/filter/DashboardEventUnreadFilter.java");
        String enrollmentServlet = source("src/main/java/pt/isel/gape/web/controller/StudentEnrollmentServlet.java");

        assertTrue(classGroupDetail.contains("studentClassGroupGradeSheetModal")
                && classGroupDetail.contains("View class group grade sheet")
                && classGroupDetail.contains("student-class-group-grade-sheet")
                && classGroupDetail.contains("student-class-group-overview")
                && !classGroupDetail.contains("Open structure"));
        assertTrue(classGroupDetail.contains("student-lesson-${lesson.id}")
                && classGroupDetail.contains("student-assessment-${assessmentItem.id}")
                && classGroupDetail.contains("data-student-event-marker")
                && classGroupDetail.contains("Enroll in assessment")
                && classGroupDetail.contains("studentAssessmentEnrollmentStateByAssessment"));
        assertTrue(enrollmentServlet.contains("studentAssessmentEnrollmentStates(actor.userId(), classGroupItem)")
                && !enrollmentServlet.contains(".filter(assessment -> hasCurrentStudentAssessmentAccess"));
        assertTrue(classGroupCard.contains("studentEventUnreadCountByClassGroupId"));
        assertTrue(calendar.contains("Calendar")
                && calendar.contains("gape-calendar-grid")
                && calendar.contains("eventCalendarNotifications")
                && !calendar.contains("Unread events")
                && !calendar.contains("studentEventSummary.unreadEvents"));
        assertTrue(attendance.contains("studentCertificateEventCount")
                && certificateCard.contains("data-student-event-read-href"));
        assertTrue(attendance.contains("[id^=\"studentCertificateDetail\"]")
                && attendance.contains("document.body.appendChild(modal)"));
        assertTrue(attendance.contains("student-certificate-card.jspf")
                && certificateCard.contains("text-line-1")
                && certificateCard.contains("data-bs-target=\"#studentCertificateDetail${certificate.id}\"")
                && certificateCard.contains("aria-label=\"View certificate\"")
                && certificateCard.contains("aria-label=\"Download certificate\"")
                && certificateCard.contains("gape-student-certificate-issued")
                && certificateCard.contains("gape-student-certificate-doc")
                && certificateCard.contains("${certificate.subjectRows}")
                && !certificateCard.contains("Verify certificate"));
        assertTrue(studentDashboardStart.contains(".gape-student-certificate-modal")
                && studentDashboardStart.contains("pointer-events: auto")
                && studentDashboardStart.contains("z-index: 1060"));
        assertTrue(sidebar.contains("data-student-class-group-event-badge")
                && sidebar.contains("data-student-certificate-event-badge"));
        assertTrue(studentFooter.contains("event-read-receipt-script.jspf")
                && eventReadScript.contains("markEventRead")
                && eventReadScript.contains("data-student-event-read-href")
                && eventReadScript.contains("clearStudentEventMarkers")
                && eventReadScript.contains("sessionUrlSuffix")
                && eventReadScript.contains("Event read receipt was not confirmed"));
        assertTrue(filter.contains("StudentEventSummary.load"));
        assertTrue(events.contains("student_class_group_enrollment_accepted")
                && events.contains("#student-class-group-overview")
                && events.contains("student_lesson_running")
                && events.contains("student_assessment_enrollment_required")
                && events.contains("student_assessment_attempt_required")
                && events.contains("student_grade_sheet_grade_available")
                && events.contains("student_certificate_published"));
    }

    @Test
    void studentClassGroupActionsKeepTerminalAndAttemptStatesUnambiguous() throws IOException {
        String detail = source("src/main/webapp/student/student/class-group/student-class-group-detail.jsp");
        String lessonView = source("src/main/java/pt/isel/gape/web/view/LessonView.java");
        String studentStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");

        assertTrue(lessonView.contains("case COMPLETED -> \"bg-danger-50 text-danger-600\""));
        assertTrue(detail.contains("aria-label=\"Lesson pending\"")
                && detail.contains("lesson.scheduled")
                && !detail.contains("lesson.completed and not lesson.cancelled"));
        assertTrue(detail.contains("aria-label=\"Start another attempt\"")
                && detail.contains("ph ph-repeat")
                && detail.contains("studentAssessmentLatestSubmittedAttempt"));
        assertTrue(detail.contains("gradeSheetModal.parentElement !== document.body")
                && detail.contains("document.body.appendChild(gradeSheetModal)")
                && detail.contains("modal-backdrop"));
        assertTrue(studentStart.contains(".gape-student-grade-sheet-modal")
                && studentStart.contains("z-index: 1060;")
                && studentStart.contains("pointer-events: auto;"));
    }

    @Test
    void studentAttemptAndResultPagesKeepLearningNavigationAndNoAttemptFlash() throws IOException {
        String servlet = source("src/main/java/pt/isel/gape/web/controller/StudentAssessmentServlet.java");
        String attempt = source("src/main/webapp/WEB-INF/views/student/assessment-attempt.jsp");
        String result = source("src/main/webapp/WEB-INF/views/student/assessment-result.jsp");

        assertFalse(servlet.contains("flashSuccess(request, \"Attempt started.\")"));
        assertTrue(servlet.contains("prepareDashboard(request, \"learning\", \"Assessment\")")
                && servlet.contains("prepareDashboard(request, \"learning\", \"Assessment Result\")"));
        assertTrue(attempt.contains("request.setAttribute(\"activeMenu\", \"learning\")")
                && attempt.contains("request.setAttribute(\"pageTitle\", \"Assessment\")"));
        assertTrue(result.contains("request.setAttribute(\"activeMenu\", \"learning\")")
                && result.contains("request.setAttribute(\"pageTitle\", \"Assessment Result\")"));
    }

    @Test
    void studentEnrollmentGuidesAndCompletedArchiveUseTheSharedSecondaryPattern() throws IOException {
        String attendance = source("src/main/webapp/student/student/attendance/student-attendance.jsp");
        String studentStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");
        String attendanceServlet = source("src/main/java/pt/isel/gape/web/controller/AttendanceManagementServlet.java");
        String courseEnrollmentCard = source("src/main/webapp/WEB-INF/fragments/student-course-enrollment-card.jspf");
        String classEnrollmentCard = source("src/main/webapp/WEB-INF/fragments/student-class-enrollment-card.jspf");
        String assessmentEnrollmentCard = source("src/main/webapp/WEB-INF/fragments/student-assessment-enrollment-card.jspf");
        String gradeServlet = source("src/main/java/pt/isel/gape/web/controller/GradeCertificateServlet.java");
        String gradeSheetModal = source("src/main/webapp/WEB-INF/fragments/student-grade-sheet-modal.jspf");

        assertTrue(attendance.contains("gape-student-mode-card--secondary")
                && attendance.contains("data-enrollment-kind=\"courses\"")
                && attendance.contains("data-enrollment-kind=\"classes\"")
                && attendance.contains("data-enrollment-kind=\"assessments\"")
                && attendance.contains("data-student-class-enrollment-modern")
                && attendance.contains("data-student-assessment-enrollment-modern")
                && attendance.contains("assessmentOccurrences")
                && attendance.contains("classGroups")
                && attendance.contains("gape-student-assessment-class-group")
                && attendance.contains("classOccurrences")
                && attendance.contains("studentCompletedClassEnrollmentGroups")
                && attendance.contains("studentCompletedAssessmentEnrollmentGroups")
                && classEnrollmentCard.contains("data-enrollment-state=\"${enrollment.state}\"")
                && classEnrollmentCard.contains("Class-group enrollment details")
                && classEnrollmentCard.contains("gape-student-enrollment-period")
                && classEnrollmentCard.contains("Modality:")
                && classEnrollmentCard.contains("Shift:")
                && classEnrollmentCard.contains("${enrollment.periodLabel}")
                && assessmentEnrollmentCard.contains("Assessment enrollment details")
                && assessmentEnrollmentCard.contains("Class group:")
                && assessmentEnrollmentCard.contains("data-enrollment-state=\"${enrollment.state}\"")
                && attendance.contains("Course, class-group and assessment enrollments."));
        assertTrue(attendance.contains("data-student-completed-enrollments")
                && attendance.contains("data-student-completed-toggle")
                && attendance.contains("data-student-completed-panel")
                && attendance.contains("gape-deferred-management-archive-row")
                && attendance.contains("gape-student-completed-occurrence")
                && attendance.contains("Newest occurrence first"));
        assertTrue(studentStart.contains(".gape-student-mode-card--secondary")
                && studentStart.contains("min-height: 84px")
                && studentStart.contains("gape-student-completed-enrollments")
                && studentStart.contains("-webkit-line-clamp: 1;")
                 && studentStart.contains("[data-student-completed-course-cards]")
                 && studentStart.contains("[data-student-completed-class-cards]")
                 && studentStart.contains("[data-student-assessment-enrollment-modern]")
                 && studentStart.contains("[data-student-completed-assessment-cards]")
                 && studentStart.contains(".gape-student-mode-grid--grades")
                 && studentStart.contains("[data-student-completed-class-cards].d-none")
                 && studentStart.contains("[id^=\"studentCompletedAssessmentEnrollment\"]")
                && studentStart.contains("[id^=\"studentActiveClassEnrollment\"]")
                && studentStart.contains(".modal-backdrop.show")
                && studentStart.contains(".gape-student-class-occurrence-group > .border")
                && studentStart.contains("same course > subject > occurrence tree")
                && studentStart.contains("background: #fff;"));
        assertTrue(attendance.contains("data-student-completed-course-count")
                && attendance.contains("data-student-completed-class-count")
                && attendance.contains("data-student-completed-assessment-count")
                 && attendance.contains("data-student-completed-assessment-cards")
                 && attendance.contains("[id^=\"studentCompletedAssessmentEnrollment\"]")
                 && !attendance.contains("Completed assessments</h5>")
                 && !attendance.contains("Completed class groups</h5>")
                 && attendance.contains("gape-student-mode-grid--grades")
                && attendance.contains("data-grade-kind=\"subjects\"")
                && attendance.contains("data-grade-kind=\"classes\"")
                && attendance.contains("${fn:length(courseGroup.activeOccurrences)} active occurrences")
                && attendance.contains("data-student-completed-subject-occurrences")
                && attendance.contains("Completed Subject Occurrences")
                && attendance.contains("data-student-completed-grades")
                && attendance.contains("Completed Grades")
                && attendance.contains("activeOccurrences")
                && attendance.contains("completedOccurrences")
                && attendance.contains("activeClassGroupSheets")
                && attendance.contains("completedClassGroupSheets")
                && attendance.contains("normaliseGradeTree")
                 && attendance.contains("normaliseCompletedClassGradeTree")
                 && attendance.contains("normaliseSubjectGradeTree")
                 && attendance.contains("normaliseActiveGradeEmptyStates")
                 && attendance.contains("No active subject grade sheets for this course.")
                 && attendance.contains("No active class-group grade sheets for this course.")
                 && attendance.contains("data-grade-active-empty")
                 && attendance.contains("justify-content-start")
                && attendance.contains("normaliseGradeArchiveShell")
                && attendance.contains("gape-learning-management-completed-divider")
                && attendance.contains("data-grade-completed-count")
                && attendance.contains("gape-student-class-subject-group")
                && attendance.contains("activateEnrollmentKind")
                && attendance.contains("Past class-group enrollments")
                && attendance.contains("document.body.appendChild(modal)")
                && attendance.contains("stableUrl=window.location.pathname+window.location.search+'#'+name")
                && attendance.contains("data-student-grade-sheet-full")
                && attendance.contains("student-grade-sheet-modal.jspf")
                 && studentStart.contains(".gape-student-grade-course-group > .row")
                 && studentStart.contains("max-width: 100%")
                 && studentStart.contains("gape-student-completed-tool .gape-learning-management-completed-panel > .gape-student-empty")
                 && studentStart.contains("[data-grade-active-empty]")
                 && studentStart.contains("overflow-wrap: anywhere;")
                 && studentStart.contains(".gape-student-completed-tool > :first-child"));
        int archiveStart = attendanceServlet.indexOf("studentCompletedCourseEnrollments");
        int archiveEnd = attendanceServlet.indexOf("Map<Long, ClassGroupView>", archiveStart);
        String archiveSource = archiveStart >= 0 && archiveEnd > archiveStart
                ? attendanceServlet.substring(archiveStart, archiveEnd)
                : "";
        assertTrue(attendance.contains("studentCompletedCourseEnrollments")
                && attendance.contains("studentActiveCourseEnrollments")
                && courseEnrollmentCard.contains("text-line-1")
                && courseEnrollmentCard.contains("Course enrollment details")
                && courseEnrollmentCard.contains("data-bs-dismiss=\"modal\">Close")
                && archiveSource.contains("EnrollmentState.COMPLETED")
                && archiveSource.contains("EnrollmentState.INACTIVE")
                && !archiveSource.contains("EnrollmentState.WITHDRAWN")
                && attendanceServlet.contains("studentActiveCourseEnrollments")
                && attendanceServlet.contains("studentActiveClassEnrollments")
                && attendanceServlet.contains("studentCompletedClassEnrollments")
                && attendanceServlet.contains("studentCompletedAssessmentEnrollments")
                && attendanceServlet.contains("findApplicableClassGroupIds")
                && attendanceServlet.contains("StudentEnrollmentOccurrenceGroupView")
                && attendanceServlet.contains("enrollment.getState() == EnrollmentState.INACTIVE")
                && attendanceServlet.contains("enrollment.getState() != EnrollmentState.INACTIVE")
                && attendanceServlet.contains("enrollment.getState() != EnrollmentState.COMPLETED")
                && attendanceServlet.contains("findCourseEnrollmentsByStudent(actor.userId())")
                && attendanceServlet.contains("courseDAO.findByIds"),
                "Student course archive must load every own enrollment while limiting the archive to inactive and completed rows");
        assertTrue(gradeServlet.contains("GradeSheetCourseOccurrenceGroupView")
                && gradeServlet.contains("getActiveOccurrences")
                && gradeServlet.contains("getCompletedOccurrences")
                && gradeServlet.contains("getActiveClassGroupSheets")
                && gradeServlet.contains("getCompletedClassGroupSheets"),
                "Student grades must expose course-occurrence and completed-grade groupings");
        assertTrue(gradeSheetModal.contains("data-student-grade-sheet-full")
                && gradeSheetModal.contains("Download")
                && gradeSheetModal.contains("data-bs-dismiss=\"modal\">Close")
                && gradeSheetModal.contains("aac-grade-doc cd-grade-sheet-doc"),
                "Every student grade card must use the full Grade sheet document modal");
    }

    @Test
    void studentPagesUseCurrentContextsAndTheCompactDetailHeroes() throws IOException {
        String enrollmentServlet = source("src/main/java/pt/isel/gape/web/controller/StudentEnrollmentServlet.java");
        String courseView = source("src/main/java/pt/isel/gape/web/view/CourseView.java");
        String courseSubjectView = source("src/main/java/pt/isel/gape/web/view/CourseSubjectView.java");
        String studentClassGroupView = source("src/main/java/pt/isel/gape/web/view/StudentClassGroupView.java");
        String studentStart = source("src/main/webapp/WEB-INF/fragments/student-dashboard-start.jspf");
        String studentSidebar = source("src/main/webapp/WEB-INF/fragments/student-dashboard-sidebar.jspf");
        String messages = source("src/main/webapp/WEB-INF/views/transversal/messages.jsp");
        String courses = source("src/main/webapp/student/student/course/student-courses.jsp");
        String courseDetail = source("src/main/webapp/student/student/course/student-course-detail.jsp");
        String subjects = source("src/main/webapp/student/student/subject/student-subjects.jsp");
        String subjectDetail = source("src/main/webapp/student/student/subject/student-subject-detail.jsp");
        String subjectClassGroupCard = source("src/main/webapp/WEB-INF/fragments/student-subject-class-group-card.jspf");
        String classGroups = source("src/main/webapp/student/student/class-group/student-class-groups.jsp");
        String classGroupCard = source("src/main/webapp/WEB-INF/fragments/student-class-group-card.jspf");
        String classGroupDetail = source("src/main/webapp/student/student/class-group/student-class-group-detail.jsp");

        assertTrue(courseView.contains("getEnrollmentStartDate"));
        assertTrue(courseView.contains("getEnrollmentEndDate"));
        assertTrue(studentStart.contains("min-height: 97px;")
                && studentStart.contains("align-items: flex-start;")
                && studentStart.contains("margin-block-end: 5px !important;")
                && studentStart.contains("padding-block-start: 3px;")
                && studentStart.contains("<section class=\"bg-main-25 pb-80 w-100 h-100\">"));
        assertTrue(courses.contains("col-xxl-3 col-xl-4 col-md-6")
                && courses.contains("gape-student-structure-card")
                && courses.contains("courseOccurrencesByCourseId[course.id]")
                && courses.contains("Occurrence:")
                && courses.contains("My Courses")
                && courses.contains("text-line-2")
                && !courses.contains("course.typeLabel"));
        assertTrue(subjects.contains("col-xxl-3 col-xl-4 col-md-6")
                && subjects.contains("gape-student-structure-card")
                && subjects.contains("gape-student-subject-period-divider")
                && subjects.contains("row gy-4 mb-24")
                && subjects.contains("My Subjects"));
        assertTrue(courseDetail.contains("gape-student-course-visual-stack gape-student-class-group-detail-hero")
                && courseDetail.contains("Current enrollment period")
                && courseDetail.contains("gape-student-subject-period-divider")
                && courseDetail.contains("gape-student-mandatory-badge")
                && !courseDetail.contains("Back to Courses"));
        assertTrue(subjectDetail.contains("gape-student-course-visual-stack gape-student-class-group-detail-hero")
                && subjectDetail.contains("hideUnenrolledSubjectClassGroups")
                && subjectDetail.contains("data-student-show-subject-unenrolled")
                && subjectDetail.contains("gape-student-card-icon-button--muted")
                && subjectDetail.contains("item.currentOrPendingEnrollment"));
        assertTrue(subjectClassGroupCard.contains("data-student-subject-unenrolled-card")
                && subjectClassGroupCard.contains("item.enrollmentStateLabel")
                && subjectClassGroupCard.contains("not item.currentOrPendingEnrollment"));
        assertTrue(studentClassGroupView.contains("isCurrentOrPendingEnrollment")
                && studentClassGroupView.contains("enrollment.isActive() || enrollment.isPending()"));
        assertTrue(classGroups.contains("Review the class groups in which you are currently enrolled.")
                && !classGroups.contains("data-student-show-unenrolled")
                && classGroups.contains("row gy-4 mb-24")
                && classGroups.contains("My Class Groups")
                && classGroupCard.contains("col-xxl-3 col-xl-4 col-md-6"));
        assertTrue(!studentSidebar.contains("Welcome <c:out value=\"${studentFirstName}\"/>,")
                && messages.contains("min-height: 97px;")
                && messages.contains("margin-block: 0 5px !important;"));
        assertTrue(classGroupDetail.contains("gape-student-course-visual-stack gape-student-class-group-detail-hero")
                && classGroupDetail.contains("Schedule")
                && !classGroupDetail.contains("Capacity")
                && !classGroupDetail.contains("Occupancy")
                && classGroupDetail.contains("gape-student-structure-block__header px-22 py-22")
                && classGroupDetail.contains("text-20 fw-semibold text-neutral-800")
                && studentStart.contains("font-size: 42px;")
                && studentStart.contains(".gape-student-structure-board")
                && studentStart.contains("background: #f8fbff;")
                && !studentStart.contains("#fff9ef"));
        assertTrue(courseDetail.contains("${course.enrollmentStartDate}"));
        assertTrue(courseDetail.contains("${course.enrollmentEndDate}"));
        assertTrue(courseSubjectView.contains("getCurricularPeriodOrder")
                && enrollmentServlet.contains("STUDENT_CURRICULAR_SUBJECT_ORDER")
                && enrollmentServlet.contains("Class Group Details")
                && enrollmentServlet.contains("Course Details")
                && enrollmentServlet.contains("Subject Details"));
    }

    @Test
    void currentCourseSelectionExcludesFutureOccurrencesAndKeepsOneCurrentEnrollmentPerCourse() {
        CourseEnrollment currentMathematics = enrollment(4L, 34L, 345L, "2025-09-01", "2026-08-01");
        CourseEnrollment futureMathematics = enrollment(4L, 34L, 346L, "2026-01-01", "2027-08-01");
        CourseEnrollment currentProject = enrollment(4L, 30L, 300L, "2026-01-01", "2026-12-31");
        CourseEnrollment completedCourse = new CourseEnrollment(
                4L, 31L, 310L, EnrollmentState.COMPLETED,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)
        );

        List<CourseEnrollment> selected = StudentEnrollmentServlet.selectCurrentCourseEnrollments(
                List.of(currentMathematics, futureMathematics, completedCourse, currentProject),
                Map.of(
                        345L, occurrence(345L, 34L, "2025-09-01", "2026-08-01"),
                        346L, occurrence(346L, 34L, "2026-09-01", "2027-08-01"),
                        300L, occurrence(300L, 30L, "2026-01-01", "2026-12-31"),
                        310L, occurrence(310L, 31L, "2025-01-01", "2025-12-31")
                ),
                LocalDate.of(2026, 7, 16)
        );

        assertEquals(2, selected.size());
        assertIterableEquals(List.of(currentMathematics, currentProject), selected);
    }

    @Test
    void curricularSubjectsKeepMandatorySubjectsBeforeOptionalSubjectsInEachPeriod() {
        CourseSubjectView optionalFirstSemester = subject(43L, "Algorithms", 1, CurricularTerm.SEMESTER_1, false);
        CourseSubjectView mandatoryFirstSemester = subject(42L, "Architecture", 1, CurricularTerm.SEMESTER_1, true);
        CourseSubjectView mandatorySecondSemester = subject(44L, "Databases", 1, CurricularTerm.SEMESTER_2, true);

        List<CourseSubjectView> sorted = StudentEnrollmentServlet.sortStudentCurricularSubjects(
                List.of(optionalFirstSemester, mandatorySecondSemester, mandatoryFirstSemester)
        );

        assertIterableEquals(
                List.of(mandatoryFirstSemester, optionalFirstSemester, mandatorySecondSemester),
                sorted
        );
    }

    @Test
    void withdrawnClassGroupsAreGroupedAsWithoutEnrollmentWhilePendingRequestsStayVisible() {
        StudentClassGroupView active = studentClassGroup(EnrollmentState.ACTIVE);
        StudentClassGroupView pending = studentClassGroup(EnrollmentState.PENDING);
        StudentClassGroupView withdrawn = studentClassGroup(EnrollmentState.WITHDRAWN);

        assertTrue(active.isCurrentOrPendingEnrollment());
        assertTrue(pending.isCurrentOrPendingEnrollment());
        assertFalse(withdrawn.isCurrentOrPendingEnrollment());
    }

    private static CourseEnrollment enrollment(
            long studentId,
            long courseId,
            long occurrenceId,
            String startsAt,
            String endsAt
    ) {
        return new CourseEnrollment(
                studentId,
                courseId,
                occurrenceId,
                EnrollmentState.ACTIVE,
                LocalDate.parse(startsAt),
                LocalDate.parse(endsAt)
        );
    }

    private static StudentClassGroupView studentClassGroup(EnrollmentState state) {
        return StudentClassGroupView.of(
                null,
                ClassGroupEnrollmentView.from(
                        new ClassGroupEnrollment(4L, 50L, state, LocalDate.of(2026, 1, 1), null),
                        "Student",
                        "student@gape.local"
                ),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                true
        );
    }

    private static CourseOccurrence occurrence(
            long occurrenceId,
            long courseId,
            String startsAt,
            String endsAt
    ) {
        return new CourseOccurrence(
                occurrenceId,
                courseId,
                LocalDate.parse(startsAt).getYear(),
                startsAt + " to " + endsAt,
                LocalDate.parse(startsAt),
                LocalDate.parse(endsAt),
                CourseOccurrenceState.ACTIVE
        );
    }

    private static CourseSubjectView subject(
            long subjectId,
            String name,
            int curricularYear,
            CurricularTerm term,
            boolean mandatory
    ) {
        Subject subject = new Subject(
                subjectId,
                10L,
                null,
                name,
                name.substring(0, 2).toUpperCase(),
                null,
                "Test subject",
                BigDecimal.valueOf(6),
                BigDecimal.valueOf(20),
                60,
                SubjectState.ACTIVE
        );
        return CourseSubjectView.from(
                new CourseSubjectAssociation(
                        34L,
                        subjectId,
                        curricularYear,
                        term,
                        mandatory,
                        CourseSubjectAssociationState.ACTIVE,
                        null
                ),
                SubjectView.from(subject, "ISG", "ISG")
        );
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
