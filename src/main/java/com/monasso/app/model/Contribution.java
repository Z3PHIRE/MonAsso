package com.monasso.app.model;

import java.time.LocalDate;

public record Contribution(
        long id,
        long memberId,
        String memberName,
        double amount,
        LocalDate contributionDate,
        String paymentMethod,
        String notes
) {
}
