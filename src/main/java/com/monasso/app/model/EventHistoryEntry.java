package com.monasso.app.model;

public record EventHistoryEntry(
        long id,
        long eventId,
        String actionType,
        String details,
        String createdAt
) {
}
