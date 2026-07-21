package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.OrganizationView;

@WebServlet(name = "courseCatalogServlet", urlPatterns = {"/courses", "/courses/*"})
public final class CourseCatalogServlet extends DashboardServletSupport {

    private static final String COURSE_LIST_JSP = "/WEB-INF/views/public/course-catalog.jsp";
    private static final String COURSE_DETAIL_JSP = "/WEB-INF/views/public/course-detail.jsp";

    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.Enrollments enrollmentDAO;
    private final ApplicationReadService.Organizations organizationDAO;
    private final LearningViewFactory viewFactory;

    public CourseCatalogServlet() {
        this(ConnectionProvider.defaultProvider());
    }

    private CourseCatalogServlet(ConnectionProvider connectionProvider) {
        this(new ApplicationReadService(connectionProvider));
    }

    CourseCatalogServlet(ApplicationReadService readService) {
        this.courseDAO = readService.courses();
        this.courseSubjectDAO = readService.courseSubjects();
        this.enrollmentDAO = readService.enrollments();
        this.organizationDAO = readService.organizations();
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
        try {
            if (segments.length == 0) {
                showCatalog(request, response);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showCatalog(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Long organizationId = optionalLong(request, "organizationId");
            CourseType type = courseType(text(request, "type"));
            String query = text(request, "q");
            Long studentUserId = studentUserId(request).orElse(null);

            List<CourseView> courses = courseDAO.findCatalogCourses(organizationId, type, query).stream()
                    .map(course -> viewFactory.courseView(course, courseEnrollment(studentUserId, course.id())))
                    .toList();
            List<OrganizationView> organizations = organizationDAO.findActive().stream()
                    .map(organization -> OrganizationView.from(organization, 0))
                    .toList();

            request.setAttribute("catalogCourses", courses);
            request.setAttribute("catalogOrganizations", organizations);
            request.setAttribute("selectedOrganizationId", organizationId == null ? "" : Long.toString(organizationId));
            request.setAttribute("selectedType", type == null ? "" : type.name());
            request.setAttribute("selectedQuery", query == null ? "" : query);
            request.setAttribute("courseCount", courses.size());
            request.setAttribute("canUseStudentActions", studentUserId != null);
            prepareDashboard(request, "courses", "Courses");
            forward(request, response, COURSE_LIST_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load course catalog", exception);
        }
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws ServletException, IOException {
        try {
            Course course = courseDAO.findActiveById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
            Long studentUserId = studentUserId(request).orElse(null);
            CourseEnrollment enrollment = courseEnrollment(studentUserId, courseId);
            CourseView courseView = viewFactory.courseView(course, enrollment);

            request.setAttribute("course", courseView);
            request.setAttribute("courseSubjects", viewFactory.courseSubjects(courseId, studentUserId));
            request.setAttribute("canUseStudentActions", studentUserId != null);
            request.setAttribute("returnTo", "/courses/" + courseId);
            prepareDashboard(request, "courses", course.name());
            forward(request, response, COURSE_DETAIL_JSP);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load course", exception);
        }
    }

    private CourseEnrollment courseEnrollment(Long studentUserId, long courseId) {
        if (studentUserId == null) {
            return null;
        }
        try {
            return enrollmentDAO.findCourseEnrollment(studentUserId, courseId).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course enrollment", exception);
        }
    }

    private Optional<Long> studentUserId(HttpServletRequest request) {
        return sessionManager.getSessionUser(request)
                .filter(user -> user.profileTypes().contains(AccessProfileType.STUDENT))
                .map(SessionUser::userId);
    }

    private static CourseType courseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CourseType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }
}
