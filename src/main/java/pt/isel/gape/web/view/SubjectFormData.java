package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;

public final class SubjectFormData {

    private final Long id;
    private final String organizationId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String description;
    private final String ects;
    private final String finalGradeMax;
    private final String workloadHours;
    private final String state;
    private final String coordinatorUserId;
    private final String initialCourseId;
    private final Set<String> initialCourseIds;
    private final String initialCurricularYear;
    private final String initialTerm;
    private final boolean initialMandatory;

    public SubjectFormData(
            Long id,
            String organizationId,
            String name,
            String acronym,
            String photo,
            String description,
            String ects,
            String finalGradeMax,
            String workloadHours,
            String state,
            String coordinatorUserId,
            String initialCourseId,
            Set<String> initialCourseIds,
            String initialCurricularYear,
            String initialTerm,
            boolean initialMandatory
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.acronym = acronym;
        this.photo = photo;
        this.description = description;
        this.ects = ects;
        this.finalGradeMax = finalGradeMax;
        this.workloadHours = workloadHours;
        this.state = state;
        this.coordinatorUserId = coordinatorUserId;
        this.initialCourseId = initialCourseId;
        this.initialCourseIds = initialCourseIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(initialCourseIds));
        this.initialCurricularYear = initialCurricularYear;
        this.initialTerm = initialTerm;
        this.initialMandatory = initialMandatory;
    }

    public static SubjectFormData blank(Long organizationId) {
        return blank(organizationId, null);
    }

    public static SubjectFormData blank(Long organizationId, Long initialCourseId) {
        String selectedInitialCourseId = initialCourseId == null ? "" : Long.toString(initialCourseId);
        return new SubjectFormData(
                null,
                organizationId == null ? "" : Long.toString(organizationId),
                "",
                "",
                "",
                "",
                "",
                "20",
                "",
                SubjectState.ACTIVE.name(),
                "",
                selectedInitialCourseId,
                selectedInitialCourseId.isBlank() ? Set.of() : Set.of(selectedInitialCourseId),
                "",
                "",
                true
        );
    }

    public static SubjectFormData from(Subject subject) {
        return new SubjectFormData(
                subject.id(),
                Long.toString(subject.organizationId()),
                subject.name(),
                subject.acronym() == null ? "" : subject.acronym(),
                subject.photo() == null ? "" : subject.photo(),
                subject.description() == null ? "" : subject.description(),
                subject.ects() == null ? "" : subject.ects().stripTrailingZeros().toPlainString(),
                subject.finalGradeMax() == null ? "" : subject.finalGradeMax().stripTrailingZeros().toPlainString(),
                subject.workloadHours() == null ? "" : Integer.toString(subject.workloadHours()),
                subject.state().name(),
                "",
                "",
                Set.of(),
                "",
                "",
                true
        );
    }

    public static SubjectFormData from(HttpServletRequest request, Long id) {
        return new SubjectFormData(
                id,
                value(request, "organizationId"),
                value(request, "name"),
                value(request, "acronym"),
                value(request, "photo"),
                value(request, "description"),
                value(request, "ects"),
                value(request, "finalGradeMax"),
                value(request, "workloadHours"),
                value(request, "state"),
                value(request, "coordinatorUserId"),
                primaryInitialCourseId(request),
                selectedInitialCourseIds(request),
                value(request, "initialCurricularYear"),
                value(request, "initialTerm"),
                request.getParameter("initialMandatory") != null
        );
    }

    public Long getId() {
        return id;
    }

    public String getOrganizationId() {
        return organizationId;
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

    public String getCoordinatorUserId() {
        return coordinatorUserId;
    }

    public String getInitialCourseId() {
        return initialCourseId;
    }

    public Set<String> getInitialCourseIds() {
        return initialCourseIds;
    }

    public boolean isInitialCourseSelected(long courseId) {
        return initialCourseIds.contains(Long.toString(courseId))
                || (initialCourseIds.isEmpty() && initialCourseId.equals(Long.toString(courseId)));
    }

    public String getInitialCurricularYear() {
        return initialCurricularYear;
    }

    public String getInitialTerm() {
        return initialTerm;
    }

    public boolean isInitialMandatory() {
        return initialMandatory;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static Set<String> selectedInitialCourseIds(HttpServletRequest request) {
        String[] values = request.getParameterValues("initialCourseIds");
        if (values == null || values.length == 0) {
            String value = value(request, "initialCourseId");
            return value.isBlank() ? Set.of() : Set.of(value);
        }
        Set<String> selected = new LinkedHashSet<>();
        Arrays.stream(values)
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .forEach(selected::add);
        return Set.copyOf(selected);
    }

    private static String primaryInitialCourseId(HttpServletRequest request) {
        return selectedInitialCourseIds(request).stream().findFirst().orElse("");
    }
}
