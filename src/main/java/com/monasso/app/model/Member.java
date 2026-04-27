package com.monasso.app.model;

import java.time.LocalDate;

public record Member(
        long id,
        String firstName,
        String lastName,
        PersonType personType,
        String email,
        String phone,
        boolean active,
        String address,
        LocalDate joinDate,
        String associationRole,
        String skills,
        String availability,
        String notes,
        String emergencyContact,
        String clothingSize,
        String certifications,
        String constraintsInfo,
        String linkedDocuments
) {
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }

    public String statusLabel() {
        return active ? "Actif" : "Inactif";
    }

    public String personTypeLabel() {
        return (personType == null ? PersonType.MEMBER : personType).label();
    }
}
