package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class AuthIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void register_returns201() throws Exception {
		mockMvc.perform(post("/api/v1/users/register")
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Alice","email":"alice@example.com","password":"password123"}"""))
				.andExpect(status().isCreated());
	}

	@Test
	void register_returns409_whenEmailAlreadyRegistered() throws Exception {
		registerUser("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/users/register")
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Alice2","email":"alice@example.com","password":"password123"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void register_returns400_whenPayloadInvalid() throws Exception {
		mockMvc.perform(post("/api/v1/users/register")
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"","email":"not-an-email","password":"123"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_returnsToken_withCorrectCredentials() throws Exception {
		registerUser("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(APPLICATION_JSON)
						.content("""
								{"email":"alice@example.com","password":"password123"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").value(3600000))
				.andExpect(jsonPath("$.refreshExpiresIn").value(2592000000L));
	}

	@Test
	void refresh_returnsNewTokenPair() throws Exception {
		registerUser("Alice", "alice@example.com", "password123");

		var loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(APPLICATION_JSON)
						.content("""
								{"email":"alice@example.com","password":"password123"}"""))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.get("refreshToken").asText();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty());
	}

	@Test
	void refresh_returns401_whenAccessTokenIsSubmitted() throws Exception {
		String accessToken = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", accessToken))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_rejectsRefreshToken() throws Exception {
		registerUser("Alice", "alice@example.com", "password123");
		var loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(APPLICATION_JSON)
						.content("""
								{"email":"alice@example.com","password":"password123"}"""))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.get("refreshToken").asText();

		mockMvc.perform(get("/api/v1/categories")
						.header("Authorization", "Bearer " + refreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void login_returns401_withWrongPassword() throws Exception {
		registerUser("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(APPLICATION_JSON)
						.content("""
								{"email":"alice@example.com","password":"wrongpassword"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void login_returns401_withUnknownEmail() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(APPLICATION_JSON)
						.content("""
								{"email":"nobody@example.com","password":"whatever123"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_returns401_withoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void protectedEndpoint_returns401_withMalformedToken() throws Exception {
		mockMvc.perform(get("/api/v1/categories")
						.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

}
