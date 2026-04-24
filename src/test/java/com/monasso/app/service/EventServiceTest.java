package com.monasso.app.service;

import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
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
}
