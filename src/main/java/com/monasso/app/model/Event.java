package com.monasso.app.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Event(
        long id,
        String title,
        LocalDate eventDate,
        LocalTime eventTime,
        String location,
        String description,
        Integer capacity,
        int participantCount
) {
}
