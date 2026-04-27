package com.monasso.app.model;

import java.time.LocalDate;

public record EventTask(
        long id,
        long eventId,
        String title,
        String description,
        LocalDate dueDate,
        Long responsibleMemberId,
        String responsibleName,
        boolean completed
) {
    public String statusLabel() {
        return completed ? "Terminee" : "Ouverte";
    }
}
