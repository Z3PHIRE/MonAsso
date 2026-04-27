package com.monasso.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Event(
        long id,
        String title,
        LocalDate eventDate,
        LocalTime eventTime,
        LocalTime endTime,
        String location,
        String description,
        Integer capacity,
        Long responsibleMemberId,
        String responsibleName,
        ScheduleStatus status,
        String category,
        String materials,
        String logisticsNeeds,
        String partners,
        String internalNotes,
        boolean archived,
        int participantCount
) {
    public String statusLabel() {
        return (status == null ? ScheduleStatus.CONFIRMED : status).label();
    }
}
