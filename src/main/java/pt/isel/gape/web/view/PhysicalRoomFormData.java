package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;

public final class PhysicalRoomFormData {

    private final String code;
    private final Long organizationId;
    private final Long organicUnitId;
    private final String name;
    private final String description;
    private final String capacity;
    private final String location;
    private final String state;

    private PhysicalRoomFormData(
            String code,
            Long organizationId,
            Long organicUnitId,
            String name,
            String description,
            String capacity,
            String location,
            String state
    ) {
        this.code = code;
        this.organizationId = organizationId;
        this.organicUnitId = organicUnitId;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.location = location;
        this.state = state;
    }

    public static PhysicalRoomFormData blank(Long organizationId) {
        return new PhysicalRoomFormData(
                "",
                organizationId,
                null,
                "",
                "",
                "",
                "",
                PhysicalRoomState.ACTIVE.name()
        );
    }

    public static PhysicalRoomFormData from(PhysicalRoom room) {
        return new PhysicalRoomFormData(
                room.code(),
                room.organizationId(),
                room.organicUnitId(),
                room.name(),
                room.description(),
                Integer.toString(room.capacity()),
                room.location(),
                room.state().name()
        );
    }

    public static PhysicalRoomFormData from(HttpServletRequest request, boolean includeCode, String existingCode) {
        return new PhysicalRoomFormData(
                includeCode ? valueOrEmpty(request.getParameter("code")) : existingCode,
                optionalLong(request.getParameter("organizationId")),
                optionalLong(request.getParameter("organicUnitId")),
                valueOrEmpty(request.getParameter("name")),
                valueOrEmpty(request.getParameter("description")),
                valueOrEmpty(request.getParameter("capacity")),
                valueOrEmpty(request.getParameter("location")),
                normalizeState(request.getParameter("state"), PhysicalRoomState.ACTIVE).name()
        );
    }

    public String getCode() {
        return code;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getOrganicUnitId() {
        return organicUnitId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCapacity() {
        return capacity;
    }

    public String getLocation() {
        return location;
    }

    public String getState() {
        return state;
    }

    public boolean isOrganizationSelected(long candidate) {
        return organizationId != null && organizationId == candidate;
    }

    public boolean isOrganicUnitSelected(long candidate) {
        return organicUnitId != null && organicUnitId == candidate;
    }

    public boolean isStateSelected(String candidate) {
        return state.equalsIgnoreCase(candidate);
    }

    public PhysicalRoomState roomState() {
        return PhysicalRoomState.valueOf(state);
    }

    public int capacityValue() {
        return Integer.parseInt(capacity);
    }

    private static PhysicalRoomState normalizeState(String value, PhysicalRoomState fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        for (PhysicalRoomState state : PhysicalRoomState.values()) {
            if (state.name().equalsIgnoreCase(normalized) || state.toDatabaseValue().equalsIgnoreCase(normalized)) {
                return state;
            }
        }
        return fallback;
    }

    private static Long optionalLong(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : Long.parseLong(normalized);
    }

    private static String valueOrEmpty(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "" : normalized;
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
