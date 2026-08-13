package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyCardPlanningIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns201() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"creditCardId":%d,"month":9,"year":2026,"amount":6000.00}"""
								.formatted(cardId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.creditCard.id").value(cardId))
				.andExpect(jsonPath("$.amount").value(6000.00));
	}

	@Test
	void create_returns409_onDuplicateCardPeriod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);
		createCardPlanning(token, cardId, 9, 2026, "6000.00");

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"creditCardId":%d,"month":9,"year":2026,"amount":1000.00}"""
								.formatted(cardId)))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns404_whenCardBelongsToAnotherUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long cardId = createCard(tokenAlice, "Nubank", 20, 10);

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"creditCardId":%d,"month":9,"year":2026,"amount":6000.00}"""
								.formatted(cardId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void list_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long cardId = createCard(tokenAlice, "Nubank", 20, 10);
		createCardPlanning(tokenAlice, cardId, 9, 2026, "6000.00");

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + tokenBob)
						.param("month", "9")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void list_computesUsedRemainingAndPercentage_fromCreditCardExpensesInBillingPeriod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);
		long categoryId = createCategory(token, "Food");
		createCardPlanning(token, cardId, 9, 2026, "6000.00");
		// Purchased 15/08, closing day 20 and due day 10 -> falls into the September invoice.
		createCardExpense(token, categoryId, cardId, "4250.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].plannedAmount").value(6000.00))
				.andExpect(jsonPath("$.content[0].spentAmount").value(4250.00))
				.andExpect(jsonPath("$.content[0].remainingAmount").value(1750.00))
				.andExpect(jsonPath("$.content[0].percentageUsed").value(70.83));
	}

	@Test
	void list_allowsRemainingToGoNegative_whenSpentExceedsPlanned() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);
		long categoryId = createCategory(token, "Food");
		createCardPlanning(token, cardId, 9, 2026, "1000.00");
		createCardExpense(token, categoryId, cardId, "1500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].remainingAmount").value(-500.00))
				.andExpect(jsonPath("$.content[0].percentageUsed").value(150.00));
	}

	@Test
	void delete_thenGet_returns404() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createCard(token, "Nubank", 20, 10);
		long planningId = createCardPlanning(token, cardId, 9, 2026, "6000.00");

		mockMvc.perform(delete("/api/v1/monthly-card-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/monthly-card-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
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

	private long createCardPlanning(String token, long cardId, int month, int year, String amount) throws Exception {
		var result = mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"creditCardId":%d,"month":%d,"year":%d,"amount":%s}"""
								.formatted(cardId, month, year, amount)))
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

	private long createCardExpense(String token, long categoryId, long cardId, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Compra","amount":%s,
								"expenseDate":"%s","paymentMethod":"CREDIT_CARD","notes":null,"creditCardId":%d}"""
								.formatted(categoryId, amount, date, cardId)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
