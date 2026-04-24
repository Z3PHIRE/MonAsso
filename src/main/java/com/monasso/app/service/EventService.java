package com.monasso.app.service;

import com.monasso.app.model.Event;
import com.monasso.app.repository.EventRepository;

import java.time.LocalDate;
import java.util.List;

public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event addEvent(String name, LocalDate date, String location, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'evenement est obligatoire.");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date de l'evenement est obligatoire.");
        }
        Event event = new Event(
                0L,
                name.trim(),
                date,
                cleanOptional(location),
                cleanOptional(description)
        );
        return eventRepository.create(event);
    }

    public List<Event> getUpcomingEvents(int limit) {
        return eventRepository.findUpcoming(LocalDate.now(), limit);
    }

    public void deleteEvent(long eventId) {
        if (!eventRepository.deleteById(eventId)) {
            throw new IllegalStateException("L'evenement n'existe plus.");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
