package com.monasso.app.model;

import java.time.LocalDate;

public record DashboardTaskItem(
        long taskId,
        String title,
        String assigneeName,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        String linkedLabel
) {
}
