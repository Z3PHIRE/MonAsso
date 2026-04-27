package com.monasso.app.model;

import java.util.List;

public record DashboardMetrics(
        List<DashboardScheduleItem> todayItems,
        List<DashboardScheduleItem> upcomingItems,
        List<DashboardTaskItem> urgentTaskItems,
        List<ContributionReminder> contributionReminders,
        List<DashboardScheduleItem> eventsToMonitor,
        List<DashboardScheduleItem> nearbyMeetingItems
) {
}
