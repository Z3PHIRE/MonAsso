package com.monasso.app.service;

import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.repository.EventParticipantRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.time.LocalTime;
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
        return eventRepository.findByCriteria(searchQuery, upcomingOnly);
    }

    public List<Event> getUpcomingEvents(int limit) {
        return eventRepository.findUpcoming(LocalDate.now(), limit);
    }

    public Event addEvent(
            String title,
            LocalDate date,
            LocalTime time,
            String location,
            String description,
            Integer capacity
    ) {
        Event event = buildEvent(
                0L,
                title,
                date,
                time,
                location,
                description,
                capacity,
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
        if (eventId <= 0) {
            throw new IllegalArgumentException("Identifiant evenement invalide.");
        }
        long participantsCountValue = eventParticipantRepository.countParticipants(eventId);
        int currentParticipants = participantsCountValue > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) participantsCountValue;
        Event event = buildEvent(
                eventId,
                title,
                date,
                time,
                location,
                description,
                capacity,
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
            LocalTime time,
            String location,
            String description,
            Integer capacity,
            int participantCount
    ) {
        String safeTitle = ValidationUtils.requireText(title, "Le titre");
        if (date == null) {
            throw new IllegalArgumentException("La date de l'evenement est obligatoire.");
        }
        if (time == null) {
            throw new IllegalArgumentException("L'heure de l'evenement est obligatoire.");
        }
        Integer safeCapacity = capacity;
        if (safeCapacity != null && safeCapacity <= 0) {
            throw new IllegalArgumentException("La capacite doit etre superieure a 0.");
        }

        return new Event(
                id,
                safeTitle,
                date,
                time,
                ValidationUtils.normalizeOptional(location),
                ValidationUtils.normalizeOptional(description),
                safeCapacity,
                participantCount
        );
    }
}
