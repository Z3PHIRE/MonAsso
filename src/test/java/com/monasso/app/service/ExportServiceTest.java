package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportServiceTest {

    @Test
    void shouldGenerateCsvXlsxAndPdfFiles() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("export-service")) {
            Member member = fixture.memberService.addMember("Jean", "Valette", "jean@example.org", null, null, LocalDate.now(), true, null);
            fixture.eventService.addEvent(
                    "Bourse aux livres",
                    LocalDate.now().plusDays(5),
                    LocalTime.of(9, 30),
                    "Salle communale",
                    "Collecte et distribution",
                    25
            );
            fixture.contributionService.addContribution(
                    member.id(),
                    35.0,
                    LocalDate.now(),
                    String.valueOf(LocalDate.now().getYear()),
                    null,
                    "Carte",
                    null
            );

            Path targetDir = fixture.appHome.resolve("exports");

            Path membersCsv = fixture.exportService.exportMembersCsv(targetDir);
            Path globalXlsx = fixture.exportService.exportGlobalXlsx(targetDir);
            Path contributionsPdf = fixture.exportService.exportContributionsPdf(targetDir);

            assertTrue(Files.exists(membersCsv));
            assertTrue(Files.size(membersCsv) > 0);
            assertTrue(Files.exists(globalXlsx));
            assertTrue(Files.size(globalXlsx) > 0);
            assertTrue(Files.exists(contributionsPdf));
            assertTrue(Files.size(contributionsPdf) > 0);
        }
    }
}
