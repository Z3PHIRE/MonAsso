package com.monasso.app.service;

import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSafetyServiceTest {

    @Test
    void shouldCreateAndRestoreBackup() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("data-safety")) {
            fixture.memberService.addMember("Anna", "Briand", null, null, null, LocalDate.now(), true, null);
            assertEquals(1, fixture.memberService.countAllMembers());

            var backup = fixture.dataSafetyService.createBackup();
            assertTrue(Files.exists(backup));

            fixture.memberService.addMember("Liam", "Perrin", null, null, null, LocalDate.now(), true, null);
            assertEquals(2, fixture.memberService.countAllMembers());

            fixture.dataSafetyService.restoreBackup(backup);
            assertEquals(1, fixture.memberService.countAllMembers());

            List<java.nio.file.Path> backups = fixture.dataSafetyService.listBackups();
            assertTrue(backups.stream().anyMatch(path -> path.getFileName().toString().endsWith(".db")));
        }
    }

    @Test
    void shouldRejectMissingBackupFile() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("data-safety-errors")) {
            var missing = fixture.appHome.resolve("backups").resolve("absente.db");
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fixture.dataSafetyService.restoreBackup(missing)
            );
            assertTrue(exception.getMessage().toLowerCase().contains("introuvable"));
        }
    }
}
