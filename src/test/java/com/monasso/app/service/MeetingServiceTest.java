package com.monasso.app.service;

import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingServiceTest {

    @Test
    void shouldCreateMeetingAndManageParticipants() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("meeting-service")) {
            Member member1 = fixture.memberService.addMember("Paul", "Martin", null, null, null, LocalDate.now(), true, null);
            Member member2 = fixture.memberService.addMember("Claire", "Bernard", null, null, null, LocalDate.now(), true, null);

            Meeting meeting = fixture.meetingService.addMeeting(
                    "Reunion de suivi",
                    LocalDate.now().plusDays(3),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 30),
                    "Salle 2",
                    "Bureau",
                    member1.id(),
                    "Ordre du jour",
                    "Notes",
                    ScheduleStatus.CONFIRMED,
                    "Administratif",
                    "compte-rendu.pdf"
            );

            fixture.meetingService.addParticipant(meeting.id(), member1.id());
            fixture.meetingService.addParticipant(meeting.id(), member2.id());

            Meeting refreshed = fixture.meetingService.getMeeting(meeting.id());
            assertEquals(2, refreshed.participantCount());

            List<Meeting> meetings = fixture.meetingService.getMeetings("suivi", true);
            assertEquals(1, meetings.size());
        }
    }

    @Test
    void shouldRejectMeetingParticipantWhenMemberInactive() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("meeting-inactive-member")) {
            Member inactiveMember = fixture.memberService.addMember("Nina", "Petit", null, null, null, LocalDate.now(), false, null);
            Meeting meeting = fixture.meetingService.addMeeting(
                    "Reunion projet",
                    LocalDate.now().plusDays(5),
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    "Salle A",
                    "Equipe",
                    null,
                    "",
                    "",
                    ScheduleStatus.PLANNED,
                    "",
                    ""
            );

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fixture.meetingService.addParticipant(meeting.id(), inactiveMember.id())
            );
            assertTrue(exception.getMessage().toLowerCase().contains("inactif"));
        }
    }
}
