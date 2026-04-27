package com.monasso.app.model;

public record EventParticipantAttendance(
        long memberId,
        String fullName,
        String email,
        boolean active,
        EventAttendanceStatus attendanceStatus
) {
}
