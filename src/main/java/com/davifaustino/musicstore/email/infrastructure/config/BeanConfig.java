package com.davifaustino.musicstore.email.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class BeanConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
