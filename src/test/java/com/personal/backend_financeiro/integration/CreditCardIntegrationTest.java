package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditCardIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns201() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/credit-cards")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","closingDay":20,"dueDay":10}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Nubank"))
				.andExpect(jsonPath("$.closingDay").value(20))
				.andExpect(jsonPath("$.dueDay").value(10))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void create_returns409_onDuplicateName() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		createCard(token, "Nubank", 20, 10);

		mockMvc.perform(post("/api/v1/credit-cards")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","closingDay":5,"dueDay":15}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns400_whenClosingDayOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/credit-cards")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Nubank","closingDay":32,"dueDay":10}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listAll_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		createCard(tokenAlice, "Nubank", 20, 10);

		mockMvc.perform(get("/api/v1/credit-cards/all")
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void update_returns404_whenCardBelongsToAnotherUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long cardId = createCard(tokenAlice, "Nubank", 20, 10);

		mockMvc.perform(put("/api/v1/credit-cards/" + cardId)
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"Roubado","closingDay":1,"dueDay":10}"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void delete_returns409_whenCardHasExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);
		long categoryId = createCategory(token, "Food");
		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Compra","amount":50.00,
								"expenseDate":"2026-08-15","paymentMethod":"CREDIT_CARD","notes":null,"creditCardId":%d}"""
								.formatted(categoryId, cardId)))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/credit-cards/" + cardId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_thenListAll_omitsCard() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);

		mockMvc.perform(delete("/api/v1/credit-cards/" + cardId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/credit-cards/all")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	private long createCard(String token, String name, int closingDay, int dueDay) throws Exception {
		var result = mockMvc.perform(post("/api/v1/credit-cards")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"name":"%s","closingDay":%d,"dueDay":%d}"""
								.formatted(name, closingDay, dueDay)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long createCategory(String token, String name) throws Exception {
		var result = mockMvc.perform(post("/api/v1/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"name\":\"%s\",\"color\":\"#FF0000\",\"icon\":null}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
