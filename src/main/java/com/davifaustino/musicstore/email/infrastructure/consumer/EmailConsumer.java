package com.davifaustino.musicstore.email.infrastructure.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.davifaustino.musicstore.email.application.service.EmailService;
import com.davifaustino.musicstore.email.application.service.EventService;
import com.davifaustino.musicstore.email.infrastructure.config.RabbitMQConfig;
import com.davifaustino.musicstore.email.model.event.Event;
import com.davifaustino.musicstore.email.model.event.UserCreatedEvent;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConsumer.class);

    private final EmailService emailService;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public EmailConsumer(EmailService emailService, EventService eventService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_CREATED_QUEUE_NAME)
    public void listenUserCreatedQueue(Event event) throws JacksonException {
        if (!eventService.saveIfNew(event)) {
            LOGGER.info("Event {} already processed, skipping", event.getId());
            return;
        }

        var userCreatedEvent = objectMapper.readValue(event.getPayload(), UserCreatedEvent.class);

        var email = emailService.sendUserCreatedEmail(userCreatedEvent);
        LOGGER.info("Email {} processed with status {}", email.getId(), email.getEmailStatus());
    }
}
