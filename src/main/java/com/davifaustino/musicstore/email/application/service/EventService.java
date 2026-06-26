package com.davifaustino.musicstore.email.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.davifaustino.musicstore.email.infrastructure.repository.EventRepository;
import com.davifaustino.musicstore.email.model.event.Event;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public boolean saveIfNew(Event event) {
        if (eventRepository.existsById(event.getId())) {
            return false;
        }

        try {
            eventRepository.saveAndFlush(event);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}
