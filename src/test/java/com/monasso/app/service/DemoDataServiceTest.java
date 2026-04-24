package com.monasso.app.service;

import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataServiceTest {

    @Test
    void shouldLoadDemoDataOnlyOnEmptyDatabase() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("demo-data")) {
            assertTrue(fixture.demoDataService.canLoadDemoData());

            DemoDataService.DemoDataResult result = fixture.demoDataService.loadDemoData();
            assertEquals(4, result.members());
            assertEquals(3, result.events());
            assertEquals(3, result.contributions());

            assertThrows(IllegalStateException.class, fixture.demoDataService::loadDemoData);
        }
    }

    @Test
    void shouldRejectDemoLoadWhenDataAlreadyExists() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("demo-data-existing")) {
            fixture.memberService.addMember("Marc", "Tessier", null, null, null, LocalDate.now(), true, null);
            IllegalStateException exception = assertThrows(IllegalStateException.class, fixture.demoDataService::loadDemoData);
            assertTrue(exception.getMessage().toLowerCase().contains("base vide"));
        }
    }
}
