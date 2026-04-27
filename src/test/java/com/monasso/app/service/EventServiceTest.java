package com.monasso.app.service;

import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventServiceTest {

    @Test
    void shouldCreateEventAndManageParticipantsWithCapacityCheck() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("event-service")) {
            Member member1 = fixture.memberService.addMember("Paul", "Martin", null, null, null, LocalDate.now(), true, null);
            Member member2 = fixture.memberService.addMember("Claire", "Bernard", null, null, null, LocalDate.now(), true, null);

            Event event = fixture.eventService.addEvent(
                    "Assemblee generale",
                    LocalDate.now().plusDays(15),
                    LocalTime.of(19, 0),
                    "Maison des associations",
                    "AG annuelle",
                    1
            );

            fixture.eventService.addParticipant(event.id(), member1.id());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> fixture.eventService.addParticipant(event.id(), member2.id()));
            assertTrue(exception.getMessage().toLowerCase().contains("capacite"));

            Event refreshed = fixture.eventService.getEvent(event.id());
            assertEquals(1, refreshed.participantCount());
        }
    }

    @Test
    void shouldRejectInactiveMemberForParticipant() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("event-inactive-member")) {
            Member inactiveMember = fixture.memberService.addMember("Nina", "Petit", null, null, null, LocalDate.now(), false, null);
            Event event = fixture.eventService.addEvent(
                    "Atelier",
                    LocalDate.now().plusDays(7),
                    LocalTime.of(10, 30),
                    "Salle 3",
                    "",
                    null
            );

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> fixture.eventService.addParticipant(event.id(), inactiveMember.id()));
            assertTrue(exception.getMessage().toLowerCase().contains("inactif"));
        }
    }

    @Test
    void shouldReturnEventsToPrepareFromNextTwoWeeks() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("event-to-prepare")) {
            fixture.eventService.addEvent(
                    "Preparation forum",
                    LocalDate.now().plusDays(3),
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 0),
                    "Salle A",
                    "",
                    null,
                    null,
                    ScheduleStatus.DRAFT,
                    "Communication"
            );
            fixture.eventService.addEvent(
                    "Planning benevoles",
                    LocalDate.now().plusDays(7),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0),
                    "Salle B",
                    "",
                    null,
                    null,
                    ScheduleStatus.PLANNED,
                    "Organisation"
            );
            fixture.eventService.addEvent(
                    "Evenement confirme",
                    LocalDate.now().plusDays(5),
                    LocalTime.of(14, 0),
                    LocalTime.of(16, 0),
                    "Salle C",
                    "",
                    null,
                    null,
                    ScheduleStatus.CONFIRMED,
                    "Autre"
            );
            fixture.eventService.addEvent(
                    "Trop loin",
                    LocalDate.now().plusDays(25),
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0),
                    "Salle D",
                    "",
                    null,
                    null,
                    ScheduleStatus.PLANNED,
                    "Autre"
            );

            var toPrepare = fixture.eventService.getEventsToPrepare(10);
            assertEquals(2, toPrepare.size());
            assertTrue(toPrepare.stream().allMatch(event ->
                    event.status() == ScheduleStatus.DRAFT || event.status() == ScheduleStatus.PLANNED
            ));
            assertTrue(toPrepare.stream().allMatch(event ->
                    !event.eventDate().isAfter(LocalDate.now().plusDays(14))
            ));
        }
    }
}
