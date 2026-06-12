package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.List;

import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.SubjectView;

final class LearningViewFactory {

    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;

    LearningViewFactory(
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO
    ) {
        this.organizationDAO = organizationDAO;
        this.organicUnitDAO = organicUnitDAO;
        this.subjectDAO = subjectDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    CourseView courseView(Course course) {
        return courseView(course, null);
    }

    CourseView courseView(Course course, CourseEnrollment enrollment) {
        try {
            OrganizationContext organization = organizationDAO.findById(course.organizationId())
                    .map(LearningViewFactory::organizationContext)
                    .orElse(new OrganizationContext("Unknown organization", ""));
            OrganicUnitContext organicUnit = course.organicUnitId() == null
                    ? null
                    : organicUnitDAO.findById(course.organicUnitId())
                            .map(LearningViewFactory::organicUnitContext)
                            .orElse(new OrganicUnitContext("Unknown unit", ""));
            int subjectCount = courseSubjectDAO.findByCourse(course.id()).size();
            return CourseView.from(
                    course,
                    organization.name(),
                    organization.acronym(),
                    organicUnit == null ? null : organicUnit.name(),
                    organicUnit == null ? null : organicUnit.acronym(),
                    subjectCount,
                    enrollment
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course view", exception);
        }
    }

    SubjectView subjectView(Subject subject) {
        try {
            OrganizationContext organization = organizationDAO.findById(subject.organizationId())
                    .map(LearningViewFactory::organizationContext)
                    .orElse(new OrganizationContext("Unknown organization", ""));
            return SubjectView.from(subject, organization.name(), organization.acronym());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build subject view", exception);
        }
    }

    List<CourseSubjectView> courseSubjects(long courseId, boolean activeOnly, Long studentUserId) {
        try {
            List<CourseSubjectAssociation> associations = activeOnly
                    ? courseSubjectDAO.findActiveByCourse(courseId)
                    : courseSubjectDAO.findByCourse(courseId);
            return associations.stream()
                    .map(association -> courseSubjectView(association, studentUserId))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course-subject views", exception);
        }
    }

    CourseSubjectView courseSubjectView(CourseSubjectAssociation association, Long studentUserId) {
        try {
            Subject subject = subjectDAO.findById(association.subjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + association.subjectId()));
            SubjectEnrollment enrollment = null;
            if (studentUserId != null) {
                enrollment = enrollmentDAO
                        .findSubjectEnrollment(studentUserId, association.courseId(), association.subjectId())
                        .orElse(null);
            }
            return CourseSubjectView.from(association, subjectView(subject), enrollment);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build course-subject view", exception);
        }
    }

    private static OrganizationContext organizationContext(Organization organization) {
        return new OrganizationContext(organization.name(), organization.acronym());
    }

    private static OrganicUnitContext organicUnitContext(OrganicUnit unit) {
        return new OrganicUnitContext(unit.name(), unit.acronym());
    }

    private record OrganizationContext(String name, String acronym) {
    }

    private record OrganicUnitContext(String name, String acronym) {
    }
}
