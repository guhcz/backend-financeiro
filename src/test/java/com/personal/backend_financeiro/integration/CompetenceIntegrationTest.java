package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the financial competence rule (CompetenceResolver): CARD + CREDIT counts towards the
 * month after its own date (invoice behavior); every other case -- CARD + DEBIT included -- and
 * incomes count towards their own date's month. Every consumer (transactions list, category
 * planning, card planning, monthly limit, dashboard) agrees on the same rule.
 */
class CompetenceIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void pixExpense_countsTowardsItsOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		createExpense(token, categoryId, pix, null, "200.00", "2026-08-15");

		assertTransactionCount(token, 8, 2026, 1);
		assertTransactionCount(token, 9, 2026, 0);
	}

	@Test
	void cardDebitExpense_countsTowardsItsOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		createExpense(token, categoryId, nubank, "DEBIT", "200.00", "2026-08-15");

		assertTransactionCount(token, 8, 2026, 1);
		assertTransactionCount(token, 9, 2026, 0);
	}

	@Test
	void cardCreditExpense_countsTowardsTheMonthAfterItsDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		createExpense(token, categoryId, nubank, "CREDIT", "500.00", "2026-08-15");

		assertTransactionCount(token, 8, 2026, 0);
		assertTransactionCount(token, 9, 2026, 1);
	}

	@Test
	void income_countsTowardsItsOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Freela");
		createIncome(token, categoryId, "1000.00", "2026-08-15");

		assertTransactionCount(token, 8, 2026, 1);
		assertTransactionCount(token, 9, 2026, 0);
	}

	@Test
	void categoryPlanning_forCardCredit_consumesTheMonthAfterExpenseDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		createPlanning(token, categoryId, 8, 2026, "1500.00");
		createPlanning(token, categoryId, 9, 2026, "1500.00");
		createExpense(token, categoryId, nubank, "CREDIT", "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "8").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(0));

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(500.00));
	}

	@Test
	void categoryPlanning_forNonCard_consumesExpenseDatesOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		createPlanning(token, categoryId, 8, 2026, "1500.00");
		createPlanning(token, categoryId, 9, 2026, "1500.00");
		createExpense(token, categoryId, pix, null, "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "8").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(500.00));

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(0));
	}

	@Test
	void monthlyLimit_forCardCredit_consumesTheMonthAfterExpenseDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":9,"year":2026,"amount":12000.00}"""))
				.andExpect(status().isCreated());
		createExpense(token, categoryId, nubank, "CREDIT", "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "8").param("year", "2026"))
				.andExpect(jsonPath("$.totalSpent").value(0));

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.totalSpent").value(500.00))
				.andExpect(jsonPath("$.monthlyLimit").value(12000.00));
	}

	@Test
	void monthlyLimit_forNonCard_consumesExpenseDatesOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":8,"year":2026,"amount":12000.00}"""))
				.andExpect(status().isCreated());
		createExpense(token, categoryId, pix, null, "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "8").param("year", "2026"))
				.andExpect(jsonPath("$.totalSpent").value(500.00))
				.andExpect(jsonPath("$.monthlyLimit").value(12000.00));
	}

	@Test
	void expenseDate_isNeverOverwritten_byCompetenceCalculation() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long expenseId = createExpense(token, categoryId, nubank, "CREDIT", "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expenseDate").value("2026-08-15"))
				.andExpect(jsonPath("$.billingMonth").value(9))
				.andExpect(jsonPath("$.billingYear").value(2026));
	}

	@Test
	void billingPeriod_rollsOverToNextYear_whenCardCreditExpenseDateIsDecember() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long expenseId = createExpense(token, categoryId, nubank, "CREDIT", "200.00", "2026-12-20");

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.billingMonth").value(1))
				.andExpect(jsonPath("$.billingYear").value(2027));

		assertTransactionCount(token, 1, 2027, 1);
	}

	@Test
	void billingPeriod_doesNotRollOver_whenNonCardExpenseDateIsDecember() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		long expenseId = createExpense(token, categoryId, pix, null, "200.00", "2026-12-20");

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.billingMonth").value(12))
				.andExpect(jsonPath("$.billingYear").value(2026));

		assertTransactionCount(token, 12, 2026, 1);
	}

	@Test
	void futureExpense_isCreatedSuccessfully_andAppearsWhenQueryingItsOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");

		createExpense(token, categoryId, pix, null, "300.00", "2026-10-05");

		assertTransactionCount(token, 10, 2026, 1);
	}

	@Test
	void futureIncome_isCreatedSuccessfully_andAppearsWhenQueryingItsOwnMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		createIncome(token, categoryId, "1000.00", "2026-10-05");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("month", "10").param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].type").value("INCOME"));
	}

	@Test
	void fullScenario_creditCardCategoryAndLimitPlanning_agreeOnSeptemberCompetence() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long foodCategory = createCategory(token, "Alimentação");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long itau = createTransactionMethod(token, "Itau", "CARD", 25, 5);

		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":9,"year":2026,"amount":12000.00}"""))
				.andExpect(status().isCreated());
		createCardPlanning(token, nubank, 9, 2026, "6000.00");
		createCardPlanning(token, itau, 9, 2026, "4000.00");
		createPlanning(token, foodCategory, 9, 2026, "1500.00");

		// Purchased 15/08 on credit -> counts towards September (the month after).
		createExpense(token, foodCategory, nubank, "CREDIT", "500.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("month", "8").param("year", "2026"))
				.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].amount").value(500.00));

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.content[?(@.transactionMethod.name == 'Nubank')].spentAmount").value(org.hamcrest.Matchers.hasItem(500.00)))
				.andExpect(jsonPath("$.content[?(@.transactionMethod.name == 'Itau')].spentAmount").value(org.hamcrest.Matchers.hasItem(0)));

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(500.00));

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.totalSpent").value(500.00));
	}

	@Test
	void cardPlanning_forSameCard_onlyCreditConsumesIt_debitDoesNot() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long foodCategory = createCategory(token, "Alimentação");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		createCardPlanning(token, nubank, 9, 2026, "6000.00");

		// Both purchased 15/08: credit counts towards September's card planning, debit counts
		// towards August only (its own month) and never towards card planning at all.
		createExpense(token, foodCategory, nubank, "CREDIT", "500.00", "2026-08-15");
		createExpense(token, foodCategory, nubank, "DEBIT", "200.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "9").param("year", "2026"))
				.andExpect(jsonPath("$.content[0].spentAmount").value(500.00));

		assertTransactionCount(token, 8, 2026, 1);
		assertTransactionCount(token, 9, 2026, 1);
	}

	private void assertTransactionCount(String token, int month, int year, int expectedCount) throws Exception {
		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("month", String.valueOf(month))
						.param("year", String.valueOf(year)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(expectedCount));
	}

	private long createCardPlanning(String token, long transactionMethodId, int month, int year, String amount) throws Exception {
		var result = mockMvc.perform(post("/api/v1/monthly-card-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"transactionMethodId":%d,"month":%d,"year":%d,"amount":%s}"""
								.formatted(transactionMethodId, month, year, amount)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long createPlanning(String token, long categoryId, int month, int year, String amount) throws Exception {
		var result = mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":%d,"year":%d,"amount":%s}"""
								.formatted(categoryId, month, year, amount)))
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

	private long createExpense(String token, long categoryId, long transactionMethodId, String cardTransactionMode, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Compra","amount":%s,
								"expenseDate":"%s","transactionMethodId":%d,"cardTransactionMode":%s,"notes":null}"""
								.formatted(categoryId, amount, date, transactionMethodId,
										cardTransactionMode == null ? "null" : "\"" + cardTransactionMode + "\"")))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long createIncome(String token, long categoryId, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Receita","amount":%s,
								"incomeDate":"%s","receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId, amount, date)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
