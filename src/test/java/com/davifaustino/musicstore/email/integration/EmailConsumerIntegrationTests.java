package com.davifaustino.musicstore.email.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.davifaustino.musicstore.email.IntegrationTests;
import com.davifaustino.musicstore.email.infrastructure.config.RabbitMQConfig;
import com.davifaustino.musicstore.email.infrastructure.repository.EmailRepository;
import com.davifaustino.musicstore.email.infrastructure.repository.EventRepository;
import com.davifaustino.musicstore.email.model.enums.EmailStatus;
import com.davifaustino.musicstore.email.model.event.Event;

class EmailConsumerIntegrationTests extends IntegrationTests {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EmailRepository emailRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void cleanState() {
        reset(javaMailSender);
        emailRepository.deleteAll();
        eventRepository.deleteAll();
        rabbitAdmin.purgeQueue(RabbitMQConfig.USER_CREATED_QUEUE_NAME, true);
    }

    @Test
    void shouldConsumeUserCreatedEventAndSaveSentEmail() {
        var eventId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        sendUserCreatedEvent(eventId, correlationId, userId, "Davi", "davifaustino.dev@gmail.com");

        awaitUntilAsserted(() -> {
            assertThat(eventRepository.findAll()).singleElement().satisfies(event -> {
                assertThat(event.getId()).isEqualTo(eventId);
                assertThat(event.getCorrelationId()).isEqualTo(correlationId);
                assertThat(event.getEventType()).isEqualTo("USER_CREATED");
                assertThat(event.getPayload()).contains(userId.toString(), "\"name\":\"Davi\"");
            });

            assertThat(emailRepository.findAll()).singleElement().satisfies(email -> {
                assertThat(email.getEmailTo()).isEqualTo("davifaustino.dev@gmail.com");
                assertThat(email.getSubject()).isEqualTo("New account at Music Store");
                assertThat(email.getText()).isEqualTo("Congrats, Davi! You've created a new account at Music Store.");
                assertThat(email.getEmailStatus()).isEqualTo(EmailStatus.SENT);
                assertThat(email.getSendDateEmail()).isNotNull();
                assertThat(email.getErrorMessage()).isNull();
                assertThat(email.getRetryCount()).isZero();
            });
        });

        var messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        var message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("davifaustino.dev@gmail.com");
        assertThat(message.getSubject()).isEqualTo("New account at Music Store");
        assertThat(message.getText()).isEqualTo("Congrats, Davi! You've created a new account at Music Store.");
    }

    @Test
    void shouldPersistFailedEmailWhenMailSenderThrowsException() {
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        var eventId = UUID.randomUUID();

        sendUserCreatedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Davi",
                "davifaustino.dev@gmail.com");

        awaitUntilAsserted(() -> {
            assertThat(eventRepository.findAll()).singleElement()
                    .satisfies(event -> assertThat(event.getId()).isEqualTo(eventId));

            assertThat(emailRepository.findAll()).singleElement().satisfies(email -> {
                assertThat(email.getEmailTo()).isEqualTo("davifaustino.dev@gmail.com");
                assertThat(email.getEmailStatus()).isEqualTo(EmailStatus.FAILED);
                assertThat(email.getSendDateEmail()).isNull();
                assertThat(email.getErrorMessage()).isEqualTo("SMTP unavailable");
            });
        });

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSkipDuplicatedUserCreatedEvent() {
        var eventId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        sendUserCreatedEvent(eventId, correlationId, userId, "Davi", "davifaustino.dev@gmail.com");

        awaitUntilAsserted(() -> {
            assertThat(eventRepository.findAll()).hasSize(1);
            assertThat(emailRepository.findAll()).hasSize(1);
        });

        sendUserCreatedEvent(eventId, correlationId, userId, "Davi", "davifaustino.dev@gmail.com");

        verify(javaMailSender, after(1_000).times(1)).send(any(SimpleMailMessage.class));
        assertThat(eventRepository.findAll()).hasSize(1);
        assertThat(emailRepository.findAll()).hasSize(1);
    }

    private void sendUserCreatedEvent(UUID eventId, UUID correlationId, UUID userId, String name, String email) {
        var payload = String.format(
                "{\"userId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"}",
                userId,
                name,
                email);
        var event = new Event(eventId, correlationId, "USER_CREATED", payload);

        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_CREATED_QUEUE_NAME, event);
    }

    private static void awaitUntilAsserted(Runnable assertion) {
        var deadline = System.nanoTime() + DEFAULT_TIMEOUT.toNanos();
        AssertionError lastError = null;

        while (System.nanoTime() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError error) {
                lastError = error;
                sleep();
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
