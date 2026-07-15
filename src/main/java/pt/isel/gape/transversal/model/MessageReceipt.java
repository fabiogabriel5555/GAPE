package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

public record MessageReceipt(
        long userId,
        long messageId,
        LocalDateTime deliveredAt,
        LocalDateTime readAt,
        MessageReceiptState state
) {
}
