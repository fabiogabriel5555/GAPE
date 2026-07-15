package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;

public final class SubjectFormData {

    private final Long id;
    private final String organizationId;
    private final String organicUnitId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String description;
    private final String ects;
    private final String finalGradeMax;
    private final String workloadHours;
    private final String state;

    public SubjectFormData(
            Long id,
            String organizationId,
            String organicUnitId,
            String name,
            String acronym,
            String photo,
            String description,
            String ects,
            String finalGradeMax,
            String workloadHours,
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
        this.finalGradeMax = finalGradeMax;
        this.workloadHours = workloadHours;
        this.state = state;
    }

    public static SubjectFormData blank(Long organizationId) {
        return new SubjectFormData(
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
                SubjectState.ACTIVE.name()
        );
    }

    public static SubjectFormData from(Subject subject) {
        return new SubjectFormData(
                subject.id(),
                Long.toString(subject.organizationId()),
                subject.organicUnitId() == null ? "" : Long.toString(subject.organicUnitId()),
                subject.name(),
                subject.acronym() == null ? "" : subject.acronym(),
                subject.photo() == null ? "" : subject.photo(),
                subject.description() == null ? "" : subject.description(),
                subject.ects() == null ? "" : subject.ects().stripTrailingZeros().toPlainString(),
                subject.finalGradeMax() == null ? "" : subject.finalGradeMax().stripTrailingZeros().toPlainString(),
                subject.workloadHours() == null ? "" : Integer.toString(subject.workloadHours()),
                subject.state().name()
        );
    }

    public static SubjectFormData from(HttpServletRequest request, Long id) {
        return new SubjectFormData(
                id,
                value(request, "organizationId"),
                value(request, "organicUnitId"),
                value(request, "name"),
                value(request, "acronym"),
                value(request, "photo"),
                value(request, "description"),
                value(request, "ects"),
                value(request, "finalGradeMax"),
                value(request, "workloadHours"),
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

    public String getFinalGradeMax() {
        return finalGradeMax;
    }

    public String getWorkloadHours() {
        return workloadHours;
    }

    public String getState() {
        return state;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

}
