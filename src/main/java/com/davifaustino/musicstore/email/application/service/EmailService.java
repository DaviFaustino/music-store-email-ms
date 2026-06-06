package com.davifaustino.musicstore.email.application.service;

import java.time.LocalDateTime;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.davifaustino.musicstore.email.application.dto.EmailDto;
import com.davifaustino.musicstore.email.infrastructure.repository.EmailRepository;
import com.davifaustino.musicstore.email.model.EmailModel;
import com.davifaustino.musicstore.email.model.enums.EmailStatus;

@Service
public class EmailService {

    private static final String EMAIL_FROM = "noreply@demomailtrap.co";

    private final EmailRepository emailRepository;
    private final JavaMailSender emailSender;

    public EmailService(EmailRepository emailRepository, JavaMailSender emailSender) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
    }

    public EmailModel sendEmail(EmailDto emailDto) {
        var email = createPendingEmail(emailDto);
        email = emailRepository.save(email);

        try {
            send(emailDto);
            email.setEmailStatus(EmailStatus.SENT);
            email.setSendDateEmail(LocalDateTime.now());
            email.setUpdatedAt(LocalDateTime.now());
            return emailRepository.save(email);
        } catch (Exception exception) {
            email.setEmailStatus(EmailStatus.FAILED);
            email.setErrorMessage(exception.getMessage());
            email.setUpdatedAt(LocalDateTime.now());
            return emailRepository.save(email);
        }
    }

    private EmailModel createPendingEmail(EmailDto emailDto) {
        var now = LocalDateTime.now();

        var email = new EmailModel();
        email.setEmailFrom(EMAIL_FROM);
        email.setEmailTo(emailDto.recipient());
        email.setSubject(emailDto.subject());
        email.setText(emailDto.text());
        email.setEmailStatus(EmailStatus.PENDING);
        email.setRetryCount(0);
        email.setCreatedAt(now);
        email.setUpdatedAt(now);

        return email;
    }

    private void send(EmailDto emailDto) {
        var message = new SimpleMailMessage();
        message.setFrom(EMAIL_FROM);
        message.setTo(emailDto.recipient());
        message.setSubject(emailDto.subject());
        message.setText(emailDto.text());

        emailSender.send(message);
    }
}
