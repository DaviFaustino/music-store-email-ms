package com.davifaustino.musicstore.email.application.dto;

public record EmailDto(
    String recipient,
    String subject,
    String text
) {
}
