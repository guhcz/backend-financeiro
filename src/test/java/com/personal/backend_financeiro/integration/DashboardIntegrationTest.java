package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void dashboard_returns400_whenMonthOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "13")
						.param("year", "2026"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void dashboard_returns400_whenYearBelowMinimum() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "1999"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void dashboard_forUserWithoutData_returnsZerosNullsAndEmptyLists() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.totalIncome").value(0))
				.andExpect(jsonPath("$.summary.balance").value(0))
				.andExpect(jsonPath("$.summary.totalExpenses").value(0))
				.andExpect(jsonPath("$.summary.monthlyLimit").doesNotExist())
				.andExpect(jsonPath("$.recentExpenses.length()").value(0))
				.andExpect(jsonPath("$.expensesByCategory.length()").value(0))
				.andExpect(jsonPath("$.monthlyExpenseHistory.length()").value(6))
				.andExpect(jsonPath("$.monthlyExpenseHistory[5].month").value(8))
				.andExpect(jsonPath("$.monthlyExpenseHistory[5].year").value(2026))
				.andExpect(jsonPath("$.monthlyExpenseHistory[5].amount").value(0))
				.andExpect(jsonPath("$.recurringExpensesSummary.activeCount").value(0))
				.andExpect(jsonPath("$.recurringExpensesSummary.dueInNext7DaysCount").value(0))
				.andExpect(jsonPath("$.recurringExpensesSummary.dueTodayAmount").value(0))
				.andExpect(jsonPath("$.financialStatus.type").value("NO_LIMIT"));
	}

	@Test
	void dashboard_withLimitAndExpenses_computesSummaryAndFinancialStatus() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		createMonthlyLimit(token, 8, 2026, "1000.00");
		// Competence is always the month after the expense date, so an expense counted in
		// August's dashboard must be dated in July.
		createExpense(token, categoryId, "850.00", "2026-07-05");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.totalExpenses").value(850.00))
				.andExpect(jsonPath("$.summary.monthlyLimit").value(1000.00))
				.andExpect(jsonPath("$.summary.availableAmount").value(150.00))
				.andExpect(jsonPath("$.summary.limitPercentageUsed").value(85.00))
				.andExpect(jsonPath("$.financialStatus.type").value("ATTENTION"))
				.andExpect(jsonPath("$.expensesByCategory[0].category.name").value("Food"))
				.andExpect(jsonPath("$.expensesByCategory[0].amount").value(850.00));
	}

	@Test
	void dashboard_recentExpenses_returnsAtMost5OrderedByDateDesc() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		for (int day = 1; day <= 6; day++) {
			createExpense(token, categoryId, "10.00", "2026-07-0" + day);
		}

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recentExpenses.length()").value(5))
				.andExpect(jsonPath("$.recentExpenses[0].expenseDate").value("2026-07-06"))
				.andExpect(jsonPath("$.recentExpenses[4].expenseDate").value("2026-07-02"));
	}

	@Test
	void dashboard_recurringExpensesSummary_countsActiveRuleDueToday() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet","amount":119.90,
								"paymentMethod":"PIX","notes":null,"frequency":"MONTHLY",
								"dueDay":%d,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, today.getDayOfMonth(), tomorrow)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", String.valueOf(today.getMonthValue()))
						.param("year", String.valueOf(today.getYear())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recurringExpensesSummary.activeCount").value(1))
				.andExpect(jsonPath("$.recurringExpensesSummary.dueInNext7DaysCount").value(1))
				.andExpect(jsonPath("$.recurringExpensesSummary.dueTodayAmount").value(119.90));
	}

	@Test
	void dashboard_withIncomes_computesTotalIncomeAndBalance() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Salary");
		createExpense(token, categoryId, "400.00", "2026-07-05");
		createIncome(token, categoryId, "1000.00", "2026-07-01");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.totalIncome").value(1000.00))
				.andExpect(jsonPath("$.summary.totalExpenses").value(400.00))
				.andExpect(jsonPath("$.summary.balance").value(600.00));
	}

	@Test
	void dashboard_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryAlice = createCategory(tokenAlice, "Food");
		createExpense(tokenAlice, categoryAlice, "500.00", "2026-08-05");

		mockMvc.perform(get("/api/v1/dashboard")
						.header("Authorization", "Bearer " + tokenBob)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.totalExpenses").value(0))
				.andExpect(jsonPath("$.recentExpenses.length()").value(0));
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

	private void createExpense(String token, long categoryId, String amount, String date) throws Exception {
		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Expense","amount":%s,
								"expenseDate":"%s","paymentMethod":"PIX","notes":null}"""
								.formatted(categoryId, amount, date)))
				.andExpect(status().isCreated());
	}

	private void createIncome(String token, long categoryId, String amount, String date) throws Exception {
		mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Income","amount":%s,
								"incomeDate":"%s","receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId, amount, date)))
				.andExpect(status().isCreated());
	}

}
