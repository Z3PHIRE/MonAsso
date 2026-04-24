package com.monasso.app.service;

import com.monasso.app.model.Member;
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
