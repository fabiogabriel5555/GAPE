package pt.isel.gape.web.view;

import java.math.BigDecimal;

import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;

public final class SubjectView {

    private final long id;
    private final long organizationId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String description;
    private final BigDecimal ects;
    private final Integer workloadHours;
    private final SubjectState state;
    private final String organizationName;
    private final String organizationAcronym;

    private SubjectView(Subject subject, String organizationName, String organizationAcronym) {
        this.id = subject.id();
        this.organizationId = subject.organizationId();
        this.name = subject.name();
        this.acronym = subject.acronym();
        this.photo = MediaPathValidator.safeRelativePath(subject.photo()).orElse(null);
        this.description = subject.description();
        this.ects = subject.ects();
        this.workloadHours = subject.workloadHours();
        this.state = subject.state();
        this.organizationName = organizationName;
        this.organizationAcronym = organizationAcronym;
    }

    public static SubjectView from(Subject subject, String organizationName, String organizationAcronym) {
        return new SubjectView(subject, organizationName, organizationAcronym);
    }

    public long getId() {
        return id;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? "-" : acronym;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean isHasPhoto() {
        return photo != null && !photo.isBlank();
    }

    public String getDescription() {
        return description == null || description.isBlank() ? "No description registered." : description;
    }

    public String getEctsLabel() {
        return ects == null ? "-" : ects.stripTrailingZeros().toPlainString() + " ECTS";
    }

    public String getWorkloadHoursLabel() {
        return workloadHours == null ? "-" : workloadHours + " h";
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return state == SubjectState.ACTIVE;
    }

    public boolean isArchived() {
        return state == SubjectState.INACTIVE;
    }

    public String getOrganizationName() {
        return organizationName == null || organizationName.isBlank() ? "Unknown organization" : organizationName;
    }

    public String getOrganizationAcronym() {
        return organizationAcronym == null || organizationAcronym.isBlank() ? "" : organizationAcronym;
    }

    public String getOrganizationContextLabel() {
        return compactPart(getOrganizationAcronym(), getOrganizationName());
    }

    public String getOrganizationContextHtml() {
        return contextPartHtml(getOrganizationAcronym(), getOrganizationName());
    }

    public String getThumbnail() {
        if (isHasPhoto()) {
            return "media/" + photo;
        }
        return null;
    }

    private static String compactPart(String acronym, String name) {
        if (acronym != null && !acronym.isBlank()) {
            return acronym;
        }
        return name == null || name.isBlank() ? "-" : name;
    }

    private static String contextPartHtml(String acronym, String name) {
        String compact = compactPart(acronym, name);
        if (acronym == null || acronym.isBlank()) {
            return escapeHtml(compact);
        }
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(name == null || name.isBlank() ? compact : name)
                + "\">"
                + escapeHtml(compact)
                + "</span>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
