package com.monasso.app.model;

public record DashboardMetrics(
        long totalMembers,
        long totalEvents,
        long totalContributions,
        double totalContributionAmount
) {
}
