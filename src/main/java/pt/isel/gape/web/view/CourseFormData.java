package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseType;

public final class CourseFormData {

    private final Long id;
    private final String organizationId;
    private final String organicUnitId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String description;
    private final String ects;
    private final String certificateMaxGrade;
    private final String duration;
    private final String type;
    private final String state;

    public CourseFormData(
            Long id,
            String organizationId,
            String organicUnitId,
            String name,
            String acronym,
            String photo,
            String description,
            String ects,
            String certificateMaxGrade,
            String duration,
            String type,
            String state
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.organicUnitId = organicUnitId;
        this.name = name;
        this.acronym = acronym;
        this.photo = photo;
        this.description = description;
        this.ects = ects;
        this.certificateMaxGrade = certificateMaxGrade;
        this.duration = duration;
        this.type = type;
        this.state = state;
    }

    public static CourseFormData blank(Long organizationId) {
        return new CourseFormData(
                null,
                organizationId == null ? "" : Long.toString(organizationId),
                "",
                "",
                "",
                "",
                "",
                "",
                "20",
                "",
                CourseType.DEGREE.name(),
                CourseState.ACTIVE.name()
        );
    }

    public static CourseFormData from(Course course) {
        return new CourseFormData(
                course.id(),
                Long.toString(course.organizationId()),
                course.organicUnitId() == null ? "" : Long.toString(course.organicUnitId()),
                course.name(),
                course.acronym() == null ? "" : course.acronym(),
                course.photo() == null ? "" : course.photo(),
                course.description() == null ? "" : course.description(),
                course.ects() == null ? "" : course.ects().stripTrailingZeros().toPlainString(),
                course.certificateMaxGrade() == null ? "" : course.certificateMaxGrade().stripTrailingZeros().toPlainString(),
                course.duration() == null ? "" : course.duration(),
                course.type().name(),
                course.state().name()
        );
    }

    public static CourseFormData from(HttpServletRequest request, Long id) {
        return new CourseFormData(
                id,
                value(request, "organizationId"),
                value(request, "organicUnitId"),
                value(request, "name"),
                value(request, "acronym"),
                value(request, "photo"),
                value(request, "description"),
                value(request, "ects"),
                value(request, "certificateMaxGrade"),
                value(request, "duration"),
                value(request, "type"),
                value(request, "state")
        );
    }

    public Long getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getOrganicUnitId() {
        return organicUnitId;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym;
    }

    public String getPhoto() {
        return photo;
    }

    public String getDescription() {
        return description;
    }

    public String getEcts() {
        return ects;
    }

    public String getCertificateMaxGrade() {
        return certificateMaxGrade;
    }

    public String getDuration() {
        return duration;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
