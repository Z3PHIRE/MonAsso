package com.monasso.app.service;

import com.monasso.app.model.CalendarEntry;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarServiceTest {

    @Test
    void shouldMergeEventsAndMeetingsAndDetectSimpleConflicts() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("calendar-service")) {
            LocalDate targetDate = LocalDate.now().plusDays(2);
            fixture.eventService.addEvent(
                    "Collecte locale",
                    targetDate,
                    LocalTime.of(18, 0),
                    LocalTime.of(20, 0),
                    "Gymnase",
                    "Collecte",
                    null,
                    null,
                    ScheduleStatus.CONFIRMED,
                    "Logistique"
            );
            fixture.meetingService.addMeeting(
                    "Reunion planning",
                    targetDate,
                    LocalTime.of(19, 0),
                    LocalTime.of(20, 30),
                    "Salle 1",
                    "Bureau",
                    null,
                    "Planning semaine",
                    "",
                    ScheduleStatus.PLANNED,
                    "Administratif",
                    ""
            );

            List<CalendarEntry> entries = fixture.calendarService.getEntries(targetDate, targetDate, null, null, null, null);
            assertEquals(2, entries.size());
            assertTrue(entries.stream().allMatch(CalendarEntry::conflict));
        }
    }
}
