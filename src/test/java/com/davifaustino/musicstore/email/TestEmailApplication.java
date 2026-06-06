package com.davifaustino.musicstore.email;

import org.springframework.boot.SpringApplication;

public class TestEmailApplication {

	public static void main(String[] args) {
		SpringApplication.from(EmailApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
