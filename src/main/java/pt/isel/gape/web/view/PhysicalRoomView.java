package pt.isel.gape.web.view;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;

public final class PhysicalRoomView {

    private final PhysicalRoom room;
    private final String organizationName;
    private final String organizationAcronym;
    private final String organicUnitName;
    private final String organicUnitAcronym;

    private PhysicalRoomView(
            PhysicalRoom room,
            String organizationName,
            String organizationAcronym,
            String organicUnitName,
            String organicUnitAcronym
    ) {
        this.room = room;
        this.organizationName = organizationName;
        this.organizationAcronym = organizationAcronym;
        this.organicUnitName = organicUnitName;
        this.organicUnitAcronym = organicUnitAcronym;
    }

    public static PhysicalRoomView from(PhysicalRoom room, Organization organization, OrganicUnit organicUnit) {
        return new PhysicalRoomView(
                room,
                organization == null ? "Unknown organization" : organization.name(),
                organization == null ? "" : organization.acronym(),
                organicUnit == null ? null : organicUnit.name(),
                organicUnit == null ? null : organicUnit.acronym()
        );
    }

    public String getCode() {
        return room.code();
    }

    public String getEncodedCode() {
        return URLEncoder.encode(room.code(), StandardCharsets.UTF_8).replace("+", "%20");
    }

    public long getOrganizationId() {
        return room.organizationId();
    }

    public Long getOrganicUnitId() {
        return room.organicUnitId();
    }

    public String getName() {
        return room.name();
    }

    public String getDescription() {
        return room.description() == null || room.description().isBlank()
                ? "No description."
                : room.description();
    }

    public String getRawDescription() {
        return room.description();
    }

    public int getCapacity() {
        return room.capacity();
    }

    public String getCapacityLabel() {
        return room.capacity() == 1 ? "1 seat" : room.capacity() + " seats";
    }

    public String getLocation() {
        return room.location() == null || room.location().isBlank() ? "-" : room.location();
    }

    public String getRawLocation() {
        return room.location();
    }

    public String getState() {
        return room.state().name();
    }

    public String getStateLabel() {
        return switch (room.state()) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case UNAVAILABLE -> "Unavailable";
        };
    }

    public String getStateBadgeClass() {
        return switch (room.state()) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
            case UNAVAILABLE -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return room.state() == PhysicalRoomState.ACTIVE;
    }

    public boolean isInactive() {
        return room.state() == PhysicalRoomState.INACTIVE;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationAcronym() {
        return organizationAcronym == null || organizationAcronym.isBlank() ? organizationName : organizationAcronym;
    }

    public String getOrganicUnitName() {
        return organicUnitName == null || organicUnitName.isBlank() ? "-" : organicUnitName;
    }

    public String getOrganicUnitAcronym() {
        if (organicUnitAcronym == null || organicUnitAcronym.isBlank()) {
            return getOrganicUnitName();
        }
        return organicUnitAcronym;
    }

    public String getContextLabel() {
        if (room.organicUnitId() != null) {
            return getOrganicUnitAcronym() + " | " + getOrganizationAcronym();
        }
        return getOrganizationAcronym();
    }

    public String getContextHtml() {
        if (room.organicUnitId() != null) {
            return contextPartHtml(getOrganicUnitAcronym(), getOrganicUnitName())
                    + " | "
                    + contextPartHtml(getOrganizationAcronym(), getOrganizationName());
        }
        return contextPartHtml(getOrganizationAcronym(), getOrganizationName());
    }

    public String getContextTitle() {
        if (room.organicUnitId() != null) {
            return getOrganicUnitName() + " | " + getOrganizationName();
        }
        return getOrganizationName();
    }

    private static String contextPartHtml(String acronym, String name) {
        String compact = acronym == null || acronym.isBlank() ? name : acronym;
        compact = compact == null || compact.isBlank() ? "-" : compact;
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
