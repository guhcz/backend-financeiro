package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyLimitIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns201() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":7,"year":2026,"amount":1000.00}"""))
				.andExpect(status().isCreated());
	}

	@Test
	void create_returns409_onDuplicatePeriod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":7,"year":2026,"amount":1000.00}"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":7,"year":2026,"amount":500.00}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns400_whenMonthOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":13,"year":2026,"amount":500.00}"""))
				.andExpect(status().isBadRequest());
	}

}
