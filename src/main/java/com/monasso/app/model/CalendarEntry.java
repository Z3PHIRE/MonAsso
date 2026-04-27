package com.monasso.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEntry(
        CalendarEntryType entryType,
        long sourceId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Long responsibleMemberId,
        String responsibleName,
        ScheduleStatus status,
        String category,
        boolean conflict
) {
    public String typeLabel() {
        return entryType == null ? "" : entryType.label();
    }
}
