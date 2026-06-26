package com.davifaustino.musicstore.email;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTests {

	@LocalServerPort
	protected int port;

	protected HttpClient httpClient;
    
	protected HttpHeaders headers;

    @BeforeEach
    void setup() {
		httpClient = HttpClient.newHttpClient();
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
    }

	protected HttpResponse<String> postJson(String path, String body) throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	protected void testLog(String message) {
		System.out.println("\u001b[33m" + message + "\u001b[0m");
	}
}
