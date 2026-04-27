package com.monasso.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record DashboardScheduleItem(
        CalendarEntryType type,
        long sourceId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String statusLabel
) {
}
