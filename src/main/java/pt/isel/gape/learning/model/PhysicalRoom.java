package pt.isel.gape.learning.model;

public record PhysicalRoom(
        String code,
        long organizationId,
        Long organicUnitId,
        String name,
        String description,
        int capacity,
        String location,
        PhysicalRoomState state
) {
}
