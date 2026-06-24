package pt.isel.gape.learning.model;

public record PhysicalRoomUpdateCommand(
        long organizationId,
        Long organicUnitId,
        String name,
        String description,
        int capacity,
        String location,
        PhysicalRoomState state
) {
}
