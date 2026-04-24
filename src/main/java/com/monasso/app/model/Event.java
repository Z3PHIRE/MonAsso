package com.monasso.app.model;

import java.time.LocalDate;

public record Event(
        long id,
        String name,
        LocalDate eventDate,
        String location,
        String description
) {
}
