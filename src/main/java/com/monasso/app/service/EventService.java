package com.monasso.app.service;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.repository.EventParticipantRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MemberRepository memberRepository;

    public EventService(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            MemberRepository memberRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.memberRepository = memberRepository;
    }

    public List<Event> getEvents(String searchQuery, boolean upcomingOnly) {
        return getEvents(searchQuery, upcomingOnly, ArchiveFilter.ACTIVE);
    }

    public List<Event> getEvents(String searchQuery, boolean upcomingOnly, ArchiveFilter archiveFilter) {
        return eventRepository.findByCriteria(searchQuery, upcomingOnly, archiveFilter);
    }

    public List<Event> getUpcomingEvents(int limit) {
        return eventRepository.findUpcoming(LocalDate.now(), limit);
    }

    public List<Event> getEventsToPrepare(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        LocalDate today = LocalDate.now();
        LocalDate untilDate = today.plusDays(14);
        Set<ScheduleStatus> statusesToPrepare = EnumSet.of(ScheduleStatus.DRAFT, ScheduleStatus.PLANNED);

        return eventRepository.findByDateRange(today, untilDate, ArchiveFilter.ACTIVE)
                .stream()
                .filter(event -> statusesToPrepare.contains(event.status()))
                .limit(safeLimit)
                .toList();
    }

    public Event addEvent(
            String title,
            LocalDate date,
            LocalTime time,
            String location,
            String description,
            Integer capacity
    ) {
        LocalTime safeStart = time == null ? null : time;
        LocalTime defaultEnd = safeStart == null ? null : safeStart.plusHours(2);
        return addEvent(
                title,
                date,
                safeStart,
                defaultEnd,
                location,
                description,
                capacity,
                null,
                ScheduleStatus.CONFIRMED,
                null
        );
    }

    public Event addEvent(
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String description,
            Integer capacity,
            Long responsibleMemberId,
            ScheduleStatus status,
            String category
    ) {
        return addEvent(
                title,
                date,
                startTime,
                endTime,
                location,
                description,
                capacity,
                responsibleMemberId,
                status,
                category,
                null,
                null,
                null,
                null
        );
    }

    public Event addEvent(
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String description,
            Integer capacity,
            Long responsibleMemberId,
            ScheduleStatus status,
            String category,
            String materials,
            String logisticsNeeds,
            String partners,
            String internalNotes
    ) {
        Event event = buildEvent(
                0L,
                title,
                date,
                startTime,
                endTime,
                location,
                description,
                capacity,
                responsibleMemberId,
                null,
                status,
                category,
                materials,
                logisticsNeeds,
                partners,
                internalNotes,
                false,
                0
        );
        return eventRepository.create(event);
    }

    public Event updateEvent(
            long eventId,
            String title,
            LocalDate date,
            LocalTime time,
            String location,
            String description,
            Integer capacity
    ) {
        LocalTime safeStart = time == null ? null : time;
        LocalTime defaultEnd = safeStart == null ? null : safeStart.plusHours(2);
        return updateEvent(
                eventId,
                title,
                date,
                safeStart,
                defaultEnd,
                location,
                description,
                capacity,
                null,
                ScheduleStatus.CONFIRMED,
                null
        );
    }

    public Event updateEvent(
            long eventId,
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String description,
            Integer capacity,
            Long responsibleMemberId,
            ScheduleStatus status,
            String category
    ) {
        return updateEvent(
                eventId,
                title,
                date,
                startTime,
                endTime,
                location,
                description,
                capacity,
                responsibleMemberId,
                status,
                category,
                null,
                null,
                null,
                null
        );
    }

    public Event updateEvent(
            long eventId,
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String description,
            Integer capacity,
            Long responsibleMemberId,
            ScheduleStatus status,
            String category,
            String materials,
            String logisticsNeeds,
            String partners,
            String internalNotes
    ) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Identifiant evenement invalide.");
        }
        Event existing = getEvent(eventId);
        long participantsCountValue = eventParticipantRepository.countParticipants(eventId);
        int currentParticipants = participantsCountValue > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) participantsCountValue;
        Event event = buildEvent(
                eventId,
                title,
                date,
                startTime,
                endTime,
                location,
                description,
                capacity,
                responsibleMemberId,
                null,
                status,
                category,
                materials,
                logisticsNeeds,
                partners,
                internalNotes,
                existing.archived(),
                currentParticipants
        );
        if (event.capacity() != null && event.capacity() < event.participantCount()) {
            throw new IllegalArgumentException("La capacite ne peut pas etre inferieure au nombre de participants deja inscrits.");
        }
        return eventRepository.update(event);
    }

    public Event getEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Evenement introuvable."));
    }

    public void deleteEvent(long eventId) {
        if (!eventRepository.deleteById(eventId)) {
            throw new IllegalStateException("L'evenement n'existe plus.");
        }
    }

    public void setArchived(long eventId, boolean archived) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Identifiant evenement invalide.");
        }
        if (!eventRepository.setArchived(eventId, archived)) {
            throw new IllegalStateException("L'evenement n'existe plus.");
        }
    }

    public List<Member> getParticipants(long eventId, String searchQuery) {
        return eventParticipantRepository.findParticipants(eventId, searchQuery);
    }

    public List<Member> getAvailableMembersForEvent(long eventId, String searchQuery) {
        Set<Long> participantIds = eventParticipantRepository.findParticipantIds(eventId);
        return memberRepository.findByCriteria(searchQuery, true)
                .stream()
                .filter(member -> !participantIds.contains(member.id()))
                .collect(Collectors.toList());
    }

    public void addParticipant(long eventId, long memberId) {
        Event event = getEvent(eventId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Membre introuvable."));

        if (!member.active()) {
            throw new IllegalArgumentException("Le membre selectionne est inactif.");
        }

        long participantCount = eventParticipantRepository.countParticipants(eventId);
        if (event.capacity() != null && participantCount >= event.capacity()) {
            throw new IllegalArgumentException("Capacite maximale atteinte pour cet evenement.");
        }
        eventParticipantRepository.addParticipant(eventId, memberId);
    }

    public void removeParticipant(long eventId, long memberId) {
        if (!eventParticipantRepository.removeParticipant(eventId, memberId)) {
            throw new IllegalStateException("Le participant n'etait pas inscrit.");
        }
    }

    private Event buildEvent(
            long id,
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String description,
            Integer capacity,
            Long responsibleMemberId,
            String responsibleName,
            ScheduleStatus status,
            String category,
            String materials,
            String logisticsNeeds,
            String partners,
            String internalNotes,
            boolean archived,
            int participantCount
    ) {
        String safeTitle = ValidationUtils.requireText(title, "Le titre");
        if (date == null) {
            throw new IllegalArgumentException("La date de l'evenement est obligatoire.");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("L'heure de debut de l'evenement est obligatoire.");
        }
        LocalTime safeEndTime = endTime == null ? startTime.plusHours(2) : endTime;
        if (!safeEndTime.isAfter(startTime)) {
            throw new IllegalArgumentException("L'heure de fin doit etre superieure a l'heure de debut.");
        }
        Integer safeCapacity = capacity;
        if (safeCapacity != null && safeCapacity <= 0) {
            throw new IllegalArgumentException("La capacite doit etre superieure a 0.");
        }
        if (responsibleMemberId != null) {
            memberRepository.findById(responsibleMemberId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsable introuvable."));
        }

        return new Event(
                id,
                safeTitle,
                date,
                startTime,
                safeEndTime,
                ValidationUtils.normalizeOptional(location),
                ValidationUtils.normalizeOptional(description),
                safeCapacity,
                responsibleMemberId,
                responsibleName,
                status == null ? ScheduleStatus.CONFIRMED : status,
                ValidationUtils.normalizeOptional(category),
                ValidationUtils.normalizeOptional(materials),
                ValidationUtils.normalizeOptional(logisticsNeeds),
                ValidationUtils.normalizeOptional(partners),
                ValidationUtils.normalizeOptional(internalNotes),
                archived,
                participantCount
        );
    }
}
