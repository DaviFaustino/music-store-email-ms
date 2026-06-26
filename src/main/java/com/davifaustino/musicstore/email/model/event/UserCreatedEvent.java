package com.davifaustino.musicstore.email.model.event;

import java.util.UUID;

public record UserCreatedEvent(
    UUID userId,
    String name,
    String email) {
}
