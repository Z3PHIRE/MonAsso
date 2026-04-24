package com.monasso.app.model;

import java.time.LocalDate;

public record Member(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        LocalDate joinDate,
        boolean active,
        String notes
) {
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }

    public String statusLabel() {
        return active ? "Actif" : "Inactif";
    }
}
