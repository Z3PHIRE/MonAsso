package com.monasso.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Meeting(
        long id,
        String title,
        LocalDate meetingDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String organizer,
        Long responsibleMemberId,
        String responsibleName,
        String agenda,
        String notes,
        ScheduleStatus status,
        String category,
        String linkedDocuments,
        boolean archived,
        int participantCount
) {
    public String statusLabel() {
        return (status == null ? ScheduleStatus.PLANNED : status).label();
    }
}
