package com.monasso.app.service;

import com.monasso.app.model.CalendarEntry;
import com.monasso.app.model.CalendarEntryType;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MeetingRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarService {

    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;

    public CalendarService(EventRepository eventRepository, MeetingRepository meetingRepository) {
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
    }

    public List<CalendarEntry> getEntries(
            LocalDate fromDate,
            LocalDate toDate,
            CalendarEntryType typeFilter,
            ScheduleStatus statusFilter,
            Long responsibleMemberId,
            String categoryFilter
    ) {
        LocalDate safeFrom = fromDate == null ? LocalDate.now() : fromDate;
        LocalDate safeTo = toDate == null ? safeFrom : toDate;
        if (safeTo.isBefore(safeFrom)) {
            safeTo = safeFrom;
        }

        List<CalendarEntry> entries = new ArrayList<>();
        if (typeFilter == null || typeFilter == CalendarEntryType.EVENT) {
            for (Event event : eventRepository.findByDateRange(safeFrom, safeTo)) {
                entries.add(new CalendarEntry(
                        CalendarEntryType.EVENT,
                        event.id(),
                        event.title(),
                        event.eventDate(),
                        event.eventTime(),
                        event.endTime(),
                        event.location(),
                        event.responsibleMemberId(),
                        event.responsibleName(),
                        event.status(),
                        event.category(),
                        false
                ));
            }
        }
        if (typeFilter == null || typeFilter == CalendarEntryType.MEETING) {
            for (Meeting meeting : meetingRepository.findByDateRange(safeFrom, safeTo)) {
                entries.add(new CalendarEntry(
                        CalendarEntryType.MEETING,
                        meeting.id(),
                        meeting.title(),
                        meeting.meetingDate(),
                        meeting.startTime(),
                        meeting.endTime(),
                        meeting.location(),
                        meeting.responsibleMemberId(),
                        meeting.responsibleName(),
                        meeting.status(),
                        meeting.category(),
                        false
                ));
            }
        }

        List<CalendarEntry> filtered = entries.stream()
                .filter(entry -> statusFilter == null || statusFilter == entry.status())
                .filter(entry -> responsibleMemberId == null || (entry.responsibleMemberId() != null && entry.responsibleMemberId().equals(responsibleMemberId)))
                .filter(entry -> matchCategory(entry, categoryFilter))
                .sorted(Comparator
                        .comparing(CalendarEntry::date)
                        .thenComparing(CalendarEntry::startTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CalendarEntry::sourceId))
                .toList();

        return withConflictFlags(filtered);
    }

    public List<CalendarEntry> getTodayEntries() {
        LocalDate today = LocalDate.now();
        return getEntries(today, today, null, null, null, null);
    }

    public List<CalendarEntry> getWeekEntries(LocalDate referenceDate) {
        LocalDate safeReference = referenceDate == null ? LocalDate.now() : referenceDate;
        LocalDate weekStart = safeReference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = safeReference.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return getEntries(weekStart, weekEnd, null, null, null, null);
    }

    public List<CalendarEntry> getListEntries(LocalDate referenceDate, int daysForward) {
        LocalDate safeReference = referenceDate == null ? LocalDate.now() : referenceDate;
        int horizon = daysForward <= 0 ? 45 : daysForward;
        return getEntries(safeReference, safeReference.plusDays(horizon), null, null, null, null);
    }

    private boolean matchCategory(CalendarEntry entry, String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isBlank()) {
            return true;
        }
        String safeCategory = entry.category() == null ? "" : entry.category();
        return safeCategory.toLowerCase(Locale.ROOT)
                .contains(categoryFilter.trim().toLowerCase(Locale.ROOT));
    }

    private List<CalendarEntry> withConflictFlags(List<CalendarEntry> entries) {
        if (entries.isEmpty()) {
            return entries;
        }

        boolean[] conflicted = new boolean[entries.size()];
        Map<LocalDate, List<Integer>> indexesByDate = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            indexesByDate.computeIfAbsent(entries.get(i).date(), ignored -> new ArrayList<>()).add(i);
        }

        for (List<Integer> dayIndexes : indexesByDate.values()) {
            for (int i = 0; i < dayIndexes.size(); i++) {
                int leftIndex = dayIndexes.get(i);
                CalendarEntry left = entries.get(leftIndex);
                LocalTime leftStart = effectiveStart(left);
                LocalTime leftEnd = effectiveEnd(left);
                for (int j = i + 1; j < dayIndexes.size(); j++) {
                    int rightIndex = dayIndexes.get(j);
                    CalendarEntry right = entries.get(rightIndex);
                    LocalTime rightStart = effectiveStart(right);
                    LocalTime rightEnd = effectiveEnd(right);
                    if (isOverlap(leftStart, leftEnd, rightStart, rightEnd)) {
                        conflicted[leftIndex] = true;
                        conflicted[rightIndex] = true;
                    }
                }
            }
        }

        List<CalendarEntry> flagged = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            CalendarEntry entry = entries.get(i);
            flagged.add(new CalendarEntry(
                    entry.entryType(),
                    entry.sourceId(),
                    entry.title(),
                    entry.date(),
                    entry.startTime(),
                    entry.endTime(),
                    entry.location(),
                    entry.responsibleMemberId(),
                    entry.responsibleName(),
                    entry.status(),
                    entry.category(),
                    conflicted[i]
            ));
        }
        return flagged;
    }

    private LocalTime effectiveStart(CalendarEntry entry) {
        return entry.startTime() == null ? LocalTime.MIN : entry.startTime();
    }

    private LocalTime effectiveEnd(CalendarEntry entry) {
        if (entry.endTime() != null) {
            return entry.endTime();
        }
        LocalTime start = effectiveStart(entry);
        return start.plusHours(2);
    }

    private boolean isOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
