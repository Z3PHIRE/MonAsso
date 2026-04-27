package com.monasso.app.service;

import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.model.TaskItem;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    @Test
    void shouldCreateFilterAndUpdateTasks() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("task-service")) {
            Member assignee = fixture.memberService.addMember("Lina", "Martin", null, null, null, LocalDate.now(), true, null);
            Event linkedEvent = fixture.eventService.addEvent(
                    "Atelier jeunes",
                    LocalDate.now().plusDays(4),
                    LocalTime.of(14, 0),
                    LocalTime.of(16, 0),
                    "Salle A",
                    "Atelier",
                    20,
                    assignee.id(),
                    ScheduleStatus.CONFIRMED,
                    "Education"
            );

            TaskItem created = fixture.taskService.addTask(
                    "Preparer materiel",
                    TaskLinkType.EVENT,
                    linkedEvent.id(),
                    null,
                    assignee.id(),
                    LocalDate.now().plusDays(2),
                    TaskPriority.HIGH,
                    TaskStatus.TODO,
                    "Verifier les kits"
            );

            List<TaskItem> filtered = fixture.taskService.getTasks("materiel", assignee.id(), LocalDate.now().plusDays(3), TaskStatus.TODO);
            assertEquals(1, filtered.size());

            TaskItem updated = fixture.taskService.updateTask(
                    created.id(),
                    created.title(),
                    created.linkType(),
                    created.linkedEventId(),
                    created.linkedMeetingId(),
                    created.assigneeMemberId(),
                    created.dueDate(),
                    created.priority(),
                    TaskStatus.DONE,
                    created.notes()
            );

            assertEquals(TaskStatus.DONE, updated.status());

            List<TaskItem> openTasks = fixture.taskService.getTasks("", assignee.id(), null, TaskStatus.TODO);
            assertFalse(openTasks.stream().anyMatch(task -> task.id() == created.id()));
        }
    }

    @Test
    void shouldReturnOnlyOverdueOpenTasks() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("task-overdue")) {
            fixture.taskService.addTask(
                    "Relance partenaire",
                    TaskLinkType.NONE,
                    null,
                    null,
                    null,
                    LocalDate.now().minusDays(2),
                    TaskPriority.HIGH,
                    TaskStatus.TODO,
                    null
            );
            fixture.taskService.addTask(
                    "Ancienne tache terminee",
                    TaskLinkType.NONE,
                    null,
                    null,
                    null,
                    LocalDate.now().minusDays(1),
                    TaskPriority.MEDIUM,
                    TaskStatus.DONE,
                    null
            );
            fixture.taskService.addTask(
                    "Tache future",
                    TaskLinkType.NONE,
                    null,
                    null,
                    null,
                    LocalDate.now().plusDays(2),
                    TaskPriority.MEDIUM,
                    TaskStatus.TODO,
                    null
            );

            List<TaskItem> overdue = fixture.taskService.getOverdueTasks(10);
            assertEquals(1, overdue.size());
            assertEquals("Relance partenaire", overdue.getFirst().title());
            assertTrue(overdue.stream().allMatch(task -> task.status() != TaskStatus.DONE));
        }
    }
}
