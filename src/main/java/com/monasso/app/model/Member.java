package com.monasso.app.model;

import java.time.LocalDate;

public record Member(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate joinDate
) {
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
