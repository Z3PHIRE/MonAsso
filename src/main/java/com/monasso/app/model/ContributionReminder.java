package com.monasso.app.model;

public record ContributionReminder(
        long memberId,
        String memberName,
        String email
) {
}
