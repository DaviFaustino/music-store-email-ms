package com.davifaustino.musicstore.email.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.davifaustino.musicstore.email.model.EmailModel;

public interface EmailRepository extends JpaRepository<EmailModel, UUID> {

}
