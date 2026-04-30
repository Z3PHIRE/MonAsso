package com.monasso.app.service;

import com.monasso.app.model.CalendarEntryType;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationFlowSmokeTest {

    @Test
    void shouldCoverMainBusinessFlowsWithoutError() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("smoke-flow")) {
            assertTrue(Files.exists(fixture.databasePath));

            Member member = fixture.memberService.addMember(
                    "Emma",
                    "Legrand",
                    "emma@example.org",
                    "0677001122",
                    "10 rue Victor Hugo",
                    LocalDate.now(),
                    true,
                    ""
            );
            assertTrue(member.id() > 0);

            var event = fixture.eventService.addEvent(
                    "Forum des associations",
                    LocalDate.now().plusDays(12),
                    LocalTime.of(11, 0),
                    "Gymnase central",
                    "Stand MonAsso",
                    30
            );
            fixture.eventService.addParticipant(event.id(), member.id());
            assertEquals(1, fixture.eventService.getEvent(event.id()).participantCount());

            var task = fixture.taskService.addTask(
                    "Installer le stand",
                    TaskLinkType.EVENT,
                    event.id(),
                    null,
                    member.id(),
                    LocalDate.now().plusDays(10),
                    TaskPriority.HIGH,
                    TaskStatus.TODO,
                    "Verifier materiel et signaletique"
            );
            assertTrue(task.id() > 0);

            var meeting = fixture.meetingService.addMeeting(
                    "Preparation benevoles",
                    LocalDate.now().plusDays(5),
                    LocalTime.of(18, 30),
                    LocalTime.of(20, 0),
                    "Salle reunion",
                    "Coordination logistique",
                    member.id(),
                    "Ordre du jour",
                    "",
                    ScheduleStatus.PLANNED,
                    "Coordination",
                    ""
            );
            assertTrue(meeting.id() > 0);

            var calendarEntries = fixture.calendarService.getEntries(
                    LocalDate.now(),
                    LocalDate.now().plusDays(30),
                    null,
                    null,
                    null,
                    null
            );
            assertTrue(calendarEntries.stream().anyMatch(entry ->
                    entry.entryType() == CalendarEntryType.EVENT && entry.sourceId() == event.id()
            ));
            assertTrue(calendarEntries.stream().anyMatch(entry ->
                    entry.entryType() == CalendarEntryType.MEETING && entry.sourceId() == meeting.id()
            ));

            var contribution = fixture.contributionService.addContribution(
                    member.id(),
                    40.0,
                    LocalDate.now(),
                    String.valueOf(LocalDate.now().getYear()),
                    null,
                    "Cheque",
                    "Test flux principal"
            );
            assertTrue(contribution.id() > 0);

            Path exported = fixture.exportService.exportMembersCsv(fixture.settingsService.getExportDirectory());
            assertTrue(Files.exists(exported));

            Path backup = fixture.dataSafetyService.createBackup();
            assertTrue(Files.exists(backup));

            fixture.brandingService.updateBranding("MonAsso QA", "#2B4F81", "#EFF4FB", "#F28B30", null);
            assertEquals("MonAsso QA", fixture.brandingService.getCurrentBranding().appName());
        }
    }
}
