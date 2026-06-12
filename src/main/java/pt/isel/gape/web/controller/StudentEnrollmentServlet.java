package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectEnrollmentCommand;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentView;

@WebServlet(name = "studentEnrollmentServlet", urlPatterns = {"/student/enrollments", "/student/enrollments/*"})
public final class StudentEnrollmentServlet extends DashboardServletSupport {

    private static final String STUDENT_ENROLLMENTS_JSP = "/student/student-enrolled-courses.jsp";

    private final EnrollmentService enrollmentService;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final LearningViewFactory viewFactory;

    public StudentEnrollmentServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private StudentEnrollmentServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new EnrollmentService(connectionProvider, clock),
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
                new LearningViewFactory(
                        new OrganizationDAO(connectionProvider),
                        new OrganicUnitDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new CourseSubjectDAO(connectionProvider),
                        new EnrollmentDAO(connectionProvider)
                )
        );
    }

    StudentEnrollmentServlet(
            EnrollmentService enrollmentService,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            LearningViewFactory viewFactory
    ) {
        this.enrollmentService = enrollmentService;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.viewFactory = viewFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showEnrollments(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 2 && "courses".equals(segments[0])) {
                enrollCourse(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 3 && "courses".equals(segments[0]) && "withdraw".equals(segments[2])) {
                withdrawCourse(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 4 && "courses".equals(segments[0]) && "subjects".equals(segments[2])) {
                enrollSubject(request, response, Long.parseLong(segments[1]), Long.parseLong(segments[3]));
                return;
            }
            if (segments.length == 5
                    && "courses".equals(segments[0])
                    && "subjects".equals(segments[2])
                    && "withdraw".equals(segments[4])) {
                withdrawSubject(request, response, Long.parseLong(segments[1]), Long.parseLong(segments[3]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showEnrollments(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            SessionUser actor = requireCurrentUser(request);
            long studentUserId = actor.userId();

            List<EnrollmentView> courseEnrollments = new ArrayList<>();
            for (CourseEnrollment enrollment : enrollmentDAO.findCourseEnrollmentsByStudent(studentUserId)) {
                courseDAO.findById(enrollment.courseId())
                        .map(course -> viewFactory.courseView(course, enrollment))
                        .map(EnrollmentView::course)
                        .ifPresent(courseEnrollments::add);
            }

            List<EnrollmentView> subjectEnrollments = new ArrayList<>();
            for (SubjectEnrollment enrollment : enrollmentDAO.findSubjectEnrollmentsByStudent(studentUserId)) {
                Course course = courseDAO.findById(enrollment.courseId()).orElse(null);
                CourseSubjectAssociation association = courseSubjectDAO
                        .findByCourseAndSubject(enrollment.courseId(), enrollment.subjectId())
                        .orElse(null);
                if (course == null || association == null) {
                    continue;
                }
                CourseView courseView = viewFactory.courseView(
                        course,
                        enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
                );
                CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
                subjectEnrollments.add(EnrollmentView.subject(courseView, subjectView));
            }

            List<CourseView> availableCourses = new ArrayList<>();
            for (Course course : courseDAO.findCatalogCourses(null, null, null)) {
                availableCourses.add(viewFactory.courseView(
                        course,
                        enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
                ));
            }

            request.setAttribute("courseEnrollments", courseEnrollments);
            request.setAttribute("subjectEnrollments", subjectEnrollments);
            request.setAttribute("availableCourses", availableCourses);
            prepareDashboard(request, "courses", "My Courses");
            forward(request, response, STUDENT_ENROLLMENTS_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student enrollments", exception);
        }
    }

    private void enrollCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInCourse(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new CourseEnrollmentCommand(actor.userId(), courseId, optionalDate(request, "startDate"), optionalDate(request, "endDate")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course enrollment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void withdrawCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromCourse(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    actor.userId(),
                    courseId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course withdrawal completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void enrollSubject(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInSubject(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new SubjectEnrollmentCommand(actor.userId(), subjectId, courseId, optionalDate(request, "startDate"), optionalDate(request, "endDate")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject enrollment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void withdrawSubject(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromSubject(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    actor.userId(),
                    courseId,
                    subjectId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject withdrawal completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }
}
