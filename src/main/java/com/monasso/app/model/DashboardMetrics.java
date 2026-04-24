package com.monasso.app.model;

import java.util.List;

public record DashboardMetrics(
        long totalMembers,
        long totalEvents,
        long paidContributions,
        long pendingContributions,
        long upcomingEventsCount,
        double totalContributionAmount,
        List<Event> upcomingEvents
) {
}
