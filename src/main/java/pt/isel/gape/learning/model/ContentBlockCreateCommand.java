package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record ContentBlockCreateCommand(
        long classGroupId,
        String code,
        String name,
        String description,
        int orderNo,
        ContentBlockAccessMode accessMode,
        ContentBlockState state,
        LocalDateTime availableFrom,
        LocalDateTime availableUntil
) {
}
