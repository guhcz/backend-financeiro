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
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":9,"year":2026,"amount":6000.00}"""
								.formatted(cardId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transactionMethod.id").value(cardId))
				.andExpect(jsonPath("$.amount").value(6000.00));
	}

	@Test
	void create_returns409_onDuplicateCardPeriod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		createCardPlanning(token, cardId, 9, 2026, "6000.00");

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":9,"year":2026,"amount":1000.00}"""
								.formatted(cardId)))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns404_whenCardBelongsToAnotherUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long cardId = createTransactionMethod(tokenAlice, "Nubank", "CARD", 20, 10);

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":9,"year":2026,"amount":6000.00}"""
								.formatted(cardId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_returns400_whenTransactionMethodIsNotACard() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long pixId = createTransactionMethod(token, "Pix", "PIX");

		mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":9,"year":2026,"amount":6000.00}"""
								.formatted(pixId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void list_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long cardId = createTransactionMethod(tokenAlice, "Nubank", "CARD", 20, 10);
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
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long categoryId = createCategory(token, "Food");
		createCardPlanning(token, cardId, 9, 2026, "6000.00");
		// Purchased 15/08 on credit -> counts towards the September competence (month after).
		createCardExpense(token, categoryId, cardId, "CREDIT", "4250.00", "2026-08-15");

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
	void list_ignoresDebitExpenses_onTheSameCard() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long categoryId = createCategory(token, "Food");
		createCardPlanning(token, cardId, 9, 2026, "6000.00");
		createCardExpense(token, categoryId, cardId, "CREDIT", "1000.00", "2026-08-15");
		createCardExpense(token, categoryId, cardId, "DEBIT", "2000.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].spentAmount").value(1000.00));
	}

	@Test
	void list_allowsRemainingToGoNegative_whenSpentExceedsPlanned() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long categoryId = createCategory(token, "Food");
		createCardPlanning(token, cardId, 9, 2026, "1000.00");
		createCardExpense(token, categoryId, cardId, "CREDIT", "1500.00", "2026-08-15");

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
		long cardId = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long planningId = createCardPlanning(token, cardId, 9, 2026, "6000.00");

		mockMvc.perform(delete("/api/v1/monthly-card-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/monthly-card-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	private long createCardPlanning(String token, long cardId, int month, int year, String amount) throws Exception {
		var result = mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":%d,"year":%d,"amount":%s}"""
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

	private long createCardExpense(String token, long categoryId, long cardId, String cardTransactionMode, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Compra","amount":%s,
								"expenseDate":"%s","transactionMethodId":%d,"cardTransactionMode":"%s","notes":null}"""
								.formatted(categoryId, amount, date, cardId, cardTransactionMode)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
