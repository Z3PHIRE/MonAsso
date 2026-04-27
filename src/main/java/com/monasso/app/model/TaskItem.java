package com.monasso.app.model;

import java.time.LocalDate;

public record TaskItem(
        long id,
        String title,
        TaskLinkType linkType,
        Long linkedEventId,
        Long linkedMeetingId,
        String linkedLabel,
        Long assigneeMemberId,
        String assigneeName,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        String notes,
        String createdAt
) {
    public boolean isOpen() {
        return status != TaskStatus.DONE;
    }
}
