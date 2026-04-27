package com.monasso.app.service;

import com.monasso.app.model.CalendarEntry;
import com.monasso.app.model.CalendarEntryType;
import com.monasso.app.model.Contribution;
import com.monasso.app.model.ContributionReminder;
import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.model.DashboardScheduleItem;
import com.monasso.app.model.DashboardTaskItem;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.TaskItem;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.TaskRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardService {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;
    private final ContributionRepository contributionRepository;
    private final TaskRepository taskRepository;
    private final CalendarService calendarService;

    public DashboardService(
            MemberRepository memberRepository,
            EventRepository eventRepository,
            MeetingRepository meetingRepository,
            ContributionRepository contributionRepository,
            TaskRepository taskRepository,
            CalendarService calendarService
    ) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
        this.contributionRepository = contributionRepository;
        this.taskRepository = taskRepository;
        this.calendarService = calendarService;
    }

    public DashboardMetrics getMetrics() {
        LocalDate today = LocalDate.now();

        List<DashboardScheduleItem> todayItems = calendarService.getTodayEntries()
                .stream()
                .map(this::toDashboardItem)
                .toList();

        List<DashboardScheduleItem> upcomingItems = calendarService
                .getEntries(today.plusDays(1), today.plusDays(14), null, null, null, null)
                .stream()
                .map(this::toDashboardItem)
                .toList();

        List<DashboardTaskItem> urgentTaskItems = taskRepository
                .findUrgent(today, today.plusDays(7), 10)
                .stream()
                .map(this::toDashboardTaskItem)
                .toList();

        String currentPeriod = String.valueOf(today.getYear());
        List<ContributionReminder> reminders = buildContributionReminders(currentPeriod, 10);

        List<DashboardScheduleItem> eventsToMonitor = eventRepository.findUpcoming(today, 8)
                .stream()
                .map(this::toDashboardEventItem)
                .toList();

        List<DashboardScheduleItem> nearbyMeetings = meetingRepository.findUpcoming(today, 8)
                .stream()
                .map(this::toDashboardMeetingItem)
                .toList();

        return new DashboardMetrics(
                todayItems,
                upcomingItems,
                urgentTaskItems,
                reminders,
                eventsToMonitor,
                nearbyMeetings
        );
    }

    private List<ContributionReminder> buildContributionReminders(String periodLabel, int limit) {
        Set<Long> paidMemberIds = new HashSet<>();
        for (Contribution contribution : contributionRepository.findByCriteria("", periodLabel, ContributionStatus.PAID)) {
            paidMemberIds.add(contribution.memberId());
        }

        return memberRepository.findByCriteria("", true)
                .stream()
                .filter(member -> !paidMemberIds.contains(member.id()))
                .limit(limit)
                .map(this::toReminder)
                .toList();
    }

    private ContributionReminder toReminder(Member member) {
        return new ContributionReminder(member.id(), member.fullName(), member.email());
    }

    private DashboardScheduleItem toDashboardItem(CalendarEntry entry) {
        return new DashboardScheduleItem(
                entry.entryType(),
                entry.sourceId(),
                entry.title(),
                entry.date(),
                entry.startTime(),
                entry.endTime(),
                entry.location(),
                entry.status() == null ? "" : entry.status().label()
        );
    }

    private DashboardScheduleItem toDashboardEventItem(Event event) {
        return new DashboardScheduleItem(
                CalendarEntryType.EVENT,
                event.id(),
                event.title(),
                event.eventDate(),
                event.eventTime(),
                event.endTime(),
                event.location(),
                event.statusLabel()
        );
    }

    private DashboardScheduleItem toDashboardMeetingItem(Meeting meeting) {
        return new DashboardScheduleItem(
                CalendarEntryType.MEETING,
                meeting.id(),
                meeting.title(),
                meeting.meetingDate(),
                meeting.startTime(),
                meeting.endTime(),
                meeting.location(),
                meeting.statusLabel()
        );
    }

    private DashboardTaskItem toDashboardTaskItem(TaskItem task) {
        return new DashboardTaskItem(
                task.id(),
                task.title(),
                task.assigneeName(),
                task.dueDate(),
                task.priority() == null ? TaskPriority.MEDIUM : task.priority(),
                task.status() == null ? TaskStatus.TODO : task.status(),
                task.linkedLabel()
        );
    }
}
