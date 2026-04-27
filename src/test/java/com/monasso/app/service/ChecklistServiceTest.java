package com.monasso.app.service;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.ChecklistItem;
import com.monasso.app.model.Event;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistServiceTest {

    @Test
    void shouldPersistDynamicChecklistForEvent() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("checklist")) {
            Event event = fixture.eventService.addEvent(
                    "Collecte",
                    LocalDate.now().plusDays(7),
                    LocalTime.of(18, 0),
                    "Salle des fetes",
                    "Preparation logistique",
                    null
            );

            ChecklistItem item = fixture.checklistService.addItem(
                    CategoryScope.EVENT,
                    event.id(),
                    "Materiel",
                    "Verifier la sono"
            );
            fixture.checklistService.setChecked(item.id(), true);

            List<ChecklistItem> items = fixture.checklistService.getItems(CategoryScope.EVENT, event.id());
            assertEquals(1, items.size());
            assertEquals("Verifier la sono", items.get(0).label());
            assertTrue(items.get(0).checked());
        }
    }
}
