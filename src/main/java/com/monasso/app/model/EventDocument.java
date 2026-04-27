package com.monasso.app.model;

public record EventDocument(
        long id,
        long eventId,
        String documentName,
        String documentRef,
        String notes
) {
}
