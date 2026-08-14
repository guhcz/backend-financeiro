package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanningIntegrationTest extends AbstractApiIntegrationTest {

	private final java.util.Map<String, Long> pixMethodByToken = new java.util.HashMap<>();

	@Test
	void summary_withLimit_returnsAllComputedFields() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		long transport = createCategory(token, "Transport");
		createMonthlyLimit(token, 7, 2026, "15000.00");
		createPlanning(token, food, 7, 2026, "1800.00");
		createPlanning(token, transport, 7, 2026, "500.00");
		// Pix competence is the expense date's own month.
		createExpense(token, food, "5250.00", "2026-07-05");
		createExpense(token, transport, "3000.00", "2026-07-10");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyLimit").value(15000.00))
				.andExpect(jsonPath("$.totalSpent").value(8250.00))
				.andExpect(jsonPath("$.availableAmount").value(6750.00))
				.andExpect(jsonPath("$.percentageUsed").value(55.00))
				.andExpect(jsonPath("$.totalPlanned").value(2300.00))
				.andExpect(jsonPath("$.unplannedAmount").value(12700.00));
	}

	@Test
	void summary_withoutLimit_returnsNullLimitFields_butKeepsSpentAndPlanned() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		createPlanning(token, food, 7, 2026, "800.00");
		createExpense(token, food, "620.00", "2026-07-05");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyLimit").doesNotExist())
				.andExpect(jsonPath("$.availableAmount").doesNotExist())
				.andExpect(jsonPath("$.percentageUsed").doesNotExist())
				.andExpect(jsonPath("$.unplannedAmount").doesNotExist())
				.andExpect(jsonPath("$.totalSpent").value(620.00))
				.andExpect(jsonPath("$.totalPlanned").value(800.00));
	}

	@Test
	void summary_userWithoutExpensesOrPlanning_returnsZeros() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalSpent").value(0))
				.andExpect(jsonPath("$.totalPlanned").value(0));
	}

	@Test
	void summary_returns400_whenMonthOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/planning/summary")
						.header("Authorization", "Bearer " + token)
						.param("month", "13")
						.param("year", "2026"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void expensesByCategory_returnsEmptyArray_whenNoExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/planning/expenses-by-category")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void expensesByCategory_returnsAmountsAndPercentages_orderedByAmountDesc() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		long transport = createCategory(token, "Transport");
		createExpense(token, food, "600.00", "2026-07-05");
		createExpense(token, transport, "400.00", "2026-07-10");

		mockMvc.perform(get("/api/v1/planning/expenses-by-category")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].category.name").value("Food"))
				.andExpect(jsonPath("$[0].amount").value(600.00))
				.andExpect(jsonPath("$[0].percentage").value(60.00))
				.andExpect(jsonPath("$[1].category.name").value("Transport"))
				.andExpect(jsonPath("$[1].amount").value(400.00))
				.andExpect(jsonPath("$[1].percentage").value(40.00));
	}

	@Test
	void expenseEvolution_accumulatesMultipleExpensesOnSameDay() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		// Pix competence is the expense date's own month (July here).
		createExpense(token, food, "300.00", "2026-07-01");
		createExpense(token, food, "550.00", "2026-07-01");
		createExpense(token, food, "1130.00", "2026-07-05");

		mockMvc.perform(get("/api/v1/planning/expense-evolution")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].date").value("2026-07-01"))
				.andExpect(jsonPath("$[0].dailyAmount").value(850.00))
				.andExpect(jsonPath("$[0].accumulatedAmount").value(850.00))
				.andExpect(jsonPath("$[1].date").value("2026-07-05"))
				.andExpect(jsonPath("$[1].dailyAmount").value(1130.00))
				.andExpect(jsonPath("$[1].accumulatedAmount").value(1980.00));
	}

	@Test
	void expenseEvolution_returnsEmptyArray_whenUserHasNoExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/planning/expense-evolution")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void dashboard_combinesSummaryExpensesByCategoryAndEvolution() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		createMonthlyLimit(token, 7, 2026, "5000.00");
		createExpense(token, food, "500.00", "2026-07-05");

		mockMvc.perform(get("/api/v1/planning/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "7")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.monthlyLimit").value(5000.00))
				.andExpect(jsonPath("$.summary.totalSpent").value(500.00))
				.andExpect(jsonPath("$.expensesByCategory.length()").value(1))
				.andExpect(jsonPath("$.expenseEvolution.length()").value(1));
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

	private void createMonthlyLimit(String token, int month, int year, String amount) throws Exception {
		mockMvc.perform(post("/api/v1/monthly-limits")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"month":%d,"year":%d,"amount":%s}"""
								.formatted(month, year, amount)))
				.andExpect(status().isCreated());
	}

	private void createPlanning(String token, long categoryId, int month, int year, String amount) throws Exception {
		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":%d,"year":%d,"amount":%s}"""
								.formatted(categoryId, month, year, amount)))
				.andExpect(status().isCreated());
	}

	private void createExpense(String token, long categoryId, String amount, String date) throws Exception {
		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Expense","amount":%s,
								"expenseDate":"%s","transactionMethodId":%d,"cardTransactionMode":null,"notes":null}"""
								.formatted(categoryId, amount, date, pixMethod(token))))
				.andExpect(status().isCreated());
	}

	private long pixMethod(String token) throws Exception {
		Long existing = pixMethodByToken.get(token);
		if (existing != null) {
			return existing;
		}
		long id = createTransactionMethod(token, "Pix", "PIX");
		pixMethodByToken.put(token, id);
		return id;
	}

}
