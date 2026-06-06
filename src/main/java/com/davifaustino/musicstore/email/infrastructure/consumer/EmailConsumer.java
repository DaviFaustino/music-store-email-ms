package com.davifaustino.musicstore.email.infrastructure.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.davifaustino.musicstore.email.application.dto.EmailDto;
import com.davifaustino.musicstore.email.application.service.EmailService;
import com.davifaustino.musicstore.email.infrastructure.config.RabbitMQConfig;

@Component
public class EmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConsumer.class);

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void listenEmailQueue(EmailDto emailDto) {
        var email = emailService.sendEmail(emailDto);
        LOGGER.info("Email {} processed with status {}", email.getId(), email.getEmailStatus());
    }
}
