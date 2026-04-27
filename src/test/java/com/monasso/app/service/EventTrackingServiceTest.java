package com.monasso.app.service;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.EventAttendanceStatus;
import com.monasso.app.model.EventBudgetLineType;
import com.monasso.app.model.EventBudgetPhase;
import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTrackingServiceTest {

    @Test
    void shouldManageEventTrackingDataEndToEnd() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("event-tracking-service")) {
            Member member = fixture.memberService.addMember(
                    "Alice",
                    "Durand",
                    null,
                    null,
                    null,
                    LocalDate.now(),
                    true,
                    null
            );

            var event = fixture.eventService.addEvent(
                    "Forum associations",
                    LocalDate.now().plusDays(12),
                    LocalTime.of(10, 0),
                    LocalTime.of(18, 0),
                    "Gymnase",
                    "Edition annuelle",
                    40,
                    member.id(),
                    null,
                    "Administratif",
                    "Stand, kakemono",
                    "Montage a 8h",
                    "Ville",
                    "Priorite securite"
            );

            fixture.eventService.addParticipant(event.id(), member.id());
            fixture.eventTrackingService.updateParticipantAttendance(event.id(), member.id(), EventAttendanceStatus.PRESENT);

            fixture.eventTrackingService.addBudgetLine(
                    event.id(),
                    EventBudgetLineType.REVENUE,
                    EventBudgetPhase.PLANNED,
                    "Budget",
                    "Subvention",
                    1000.0,
                    ""
            );
            fixture.eventTrackingService.addBudgetLine(
                    event.id(),
                    EventBudgetLineType.EXPENSE,
                    EventBudgetPhase.ACTUAL,
                    "Logistique",
                    "Location barnum",
                    300.0,
                    ""
            );
            var summary = fixture.eventTrackingService.getBudgetSummary(event.id());
            assertEquals(1000.0, summary.plannedRevenue(), 0.01);
            assertEquals(300.0, summary.actualExpense(), 0.01);

            var task = fixture.eventTrackingService.addTask(
                    event.id(),
                    "Valider plan de salle",
                    "",
                    LocalDate.now().plusDays(7),
                    member.id()
            );
            fixture.eventTrackingService.setTaskCompleted(event.id(), task.id(), true);

            fixture.eventTrackingService.addDocument(
                    event.id(),
                    "Plan implantation",
                    "C:\\docs\\plan.pdf",
                    "Version 1"
            );
            fixture.checklistService.addItem(CategoryScope.EVENT, event.id(), "Securite", "Verifier extincteurs");
            fixture.checklistService.setChecked(
                    fixture.checklistService.getItems(CategoryScope.EVENT, event.id()).getFirst().id(),
                    true
            );

            var progress = fixture.eventTrackingService.getProgress(event.id());
            assertEquals(1, progress.participantsPresent());
            assertEquals(1, progress.tasksCompleted());
            assertEquals(1, progress.checklistCompleted());
            assertTrue(progress.completionRatio() > 0.5);

            assertFalse(fixture.eventTrackingService.getHistory(event.id(), 20).isEmpty());
            assertFalse(fixture.eventTrackingService.getDocuments(event.id()).isEmpty());
            assertFalse(fixture.eventTrackingService.getTasks(event.id(), false).isEmpty());
            assertTrue(fixture.eventTrackingService.getBudgetLines(event.id()).size() >= 2);
        }
    }
}
