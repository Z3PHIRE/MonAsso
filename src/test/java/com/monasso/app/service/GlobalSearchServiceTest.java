package com.monasso.app.service;

import com.monasso.app.model.GlobalSearchResult;
import com.monasso.app.model.GlobalSearchType;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSearchServiceTest {

    @Test
    void shouldSearchAcrossPersonsEventsMeetingsAndTasks() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("global-search-service")) {
            var member = fixture.memberService.addMember("Alice", "Atelier", null, null, null, LocalDate.now(), true, null);

            fixture.eventService.addEvent(
                    "Atelier numerique",
                    LocalDate.now().plusDays(5),
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 0),
                    "Salle 1",
                    "Session",
                    null,
                    member.id(),
                    ScheduleStatus.CONFIRMED,
                    "Formation"
            );

            fixture.meetingService.addMeeting(
                    "Atelier pilotage",
                    LocalDate.now().plusDays(2),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0),
                    "Salle 2",
                    "Bureau",
                    member.id(),
                    "Ordre du jour",
                    "Notes",
                    ScheduleStatus.PLANNED,
                    "Administratif",
                    ""
            );

            fixture.taskService.addTask(
                    "Atelier checklist",
                    TaskLinkType.NONE,
                    null,
                    null,
                    member.id(),
                    LocalDate.now().plusDays(1),
                    TaskPriority.MEDIUM,
                    TaskStatus.TODO,
                    ""
            );

            List<GlobalSearchResult> results = fixture.globalSearchService.search("atelier");
            Set<GlobalSearchType> types = results.stream().map(GlobalSearchResult::type).collect(Collectors.toSet());

            assertTrue(types.contains(GlobalSearchType.PERSON));
            assertTrue(types.contains(GlobalSearchType.EVENT));
            assertTrue(types.contains(GlobalSearchType.MEETING));
            assertTrue(types.contains(GlobalSearchType.TASK));
        }
    }
}
