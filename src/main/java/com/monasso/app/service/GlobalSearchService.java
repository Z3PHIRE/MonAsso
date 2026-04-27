package com.monasso.app.service;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Event;
import com.monasso.app.model.GlobalSearchResult;
import com.monasso.app.model.GlobalSearchType;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.TaskItem;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.TaskRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GlobalSearchService {

    private static final int DEFAULT_LIMIT_PER_TYPE = 20;

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;
    private final TaskRepository taskRepository;

    public GlobalSearchService(
            MemberRepository memberRepository,
            EventRepository eventRepository,
            MeetingRepository meetingRepository,
            TaskRepository taskRepository
    ) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
        this.taskRepository = taskRepository;
    }

    public List<GlobalSearchResult> search(String query) {
        return search(query, DEFAULT_LIMIT_PER_TYPE);
    }

    public List<GlobalSearchResult> search(String query, int limitPerType) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        int safeLimit = limitPerType <= 0 ? DEFAULT_LIMIT_PER_TYPE : limitPerType;

        List<GlobalSearchResult> results = new ArrayList<>();

        for (Member member : memberRepository.findByCriteria(trimmed, null).stream().limit(safeLimit).toList()) {
            results.add(new GlobalSearchResult(
                    GlobalSearchType.PERSON,
                    member.id(),
                    member.fullName(),
                    member.personTypeLabel() + " | " + member.statusLabel(),
                    buildPersonDetails(member)
            ));
        }

        for (Event event : eventRepository.findByCriteria(trimmed, false, ArchiveFilter.ACTIVE).stream().limit(safeLimit).toList()) {
            results.add(new GlobalSearchResult(
                    GlobalSearchType.EVENT,
                    event.id(),
                    event.title(),
                    event.eventDate() + " " + event.eventTime() + " | " + event.statusLabel(),
                    safeText(event.location())
            ));
        }

        for (Meeting meeting : meetingRepository.findByCriteria(trimmed, false, ArchiveFilter.ACTIVE).stream().limit(safeLimit).toList()) {
            results.add(new GlobalSearchResult(
                    GlobalSearchType.MEETING,
                    meeting.id(),
                    meeting.title(),
                    meeting.meetingDate() + " " + meeting.startTime() + " | " + meeting.statusLabel(),
                    safeText(meeting.location())
            ));
        }

        for (TaskItem task : taskRepository.findByCriteria(trimmed, null, null, null).stream().limit(safeLimit).toList()) {
            String due = task.dueDate() == null ? "Sans echeance" : "Echeance " + task.dueDate();
            results.add(new GlobalSearchResult(
                    GlobalSearchType.TASK,
                    task.id(),
                    task.title(),
                    task.status().label() + " | " + task.priority().label(),
                    due + " | " + safeText(task.assigneeName())
            ));
        }

        String lowered = trimmed.toLowerCase(Locale.ROOT);
        return results.stream()
                .sorted(Comparator
                        .comparingInt((GlobalSearchResult result) -> score(result, lowered)).reversed()
                        .thenComparing(result -> result.type().ordinal())
                        .thenComparing(result -> result.title().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private int score(GlobalSearchResult result, String query) {
        String title = result.title() == null ? "" : result.title().toLowerCase(Locale.ROOT);
        String subtitle = result.subtitle() == null ? "" : result.subtitle().toLowerCase(Locale.ROOT);
        String details = result.details() == null ? "" : result.details().toLowerCase(Locale.ROOT);

        if (title.startsWith(query)) {
            return 3;
        }
        if (title.contains(query)) {
            return 2;
        }
        if (subtitle.contains(query) || details.contains(query)) {
            return 1;
        }
        return 0;
    }

    private String buildPersonDetails(Member member) {
        String email = safeText(member.email());
        String phone = safeText(member.phone());
        if (!email.isBlank() && !phone.isBlank()) {
            return email + " | " + phone;
        }
        return email.isBlank() ? phone : email;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
