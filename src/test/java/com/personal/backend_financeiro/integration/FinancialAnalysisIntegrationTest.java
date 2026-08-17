package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the "Análise financeira" screen endpoints (GET /api/v1/financial-analysis and
 * GET /api/v1/financial-analysis/payment-methods): monthly income-vs-expenses/balance buckets,
 * financial competence per payment method, category/payment-method aggregation and percentages,
 * largest expenses, and the paginated payment-methods modal.
 */
class FinancialAnalysisIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void incomeVsExpenses_and_balance_zeroFillEmptyMonths_andCoverAllBalanceSigns() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");

		// June: expense only -> negative balance.
		createExpense(token, categoryId, pix, null, "500.00", "2026-06-10");
		// July: income only -> positive balance.
		createIncome(token, categoryId, "1000.00", "2026-07-10");
		// August: no movements -> zero-filled month.
		// September: income equals expense -> zero balance.
		createIncome(token, categoryId, "800.00", "2026-09-05");
		createExpense(token, categoryId, pix, null, "800.00", "2026-09-15");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-09-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incomeVsExpenses.length()").value(4))
				.andExpect(jsonPath("$.incomeVsExpenses[0].month").value(6))
				.andExpect(jsonPath("$.incomeVsExpenses[0].incomeAmount").value(0))
				.andExpect(jsonPath("$.incomeVsExpenses[0].expenseAmount").value(500.00))
				.andExpect(jsonPath("$.incomeVsExpenses[1].month").value(7))
				.andExpect(jsonPath("$.incomeVsExpenses[1].incomeAmount").value(1000.00))
				.andExpect(jsonPath("$.incomeVsExpenses[1].expenseAmount").value(0))
				.andExpect(jsonPath("$.incomeVsExpenses[2].month").value(8))
				.andExpect(jsonPath("$.incomeVsExpenses[2].incomeAmount").value(0))
				.andExpect(jsonPath("$.incomeVsExpenses[2].expenseAmount").value(0))
				.andExpect(jsonPath("$.incomeVsExpenses[3].month").value(9))
				.andExpect(jsonPath("$.incomeVsExpenses[3].incomeAmount").value(800.00))
				.andExpect(jsonPath("$.incomeVsExpenses[3].expenseAmount").value(800.00))
				.andExpect(jsonPath("$.monthlyBalanceEvolution[0].balance").value(-500.00))
				.andExpect(jsonPath("$.monthlyBalanceEvolution[1].balance").value(1000.00))
				.andExpect(jsonPath("$.monthlyBalanceEvolution[2].balance").value(0))
				.andExpect(jsonPath("$.monthlyBalanceEvolution[3].balance").value(0));
	}

	@Test
	void competence_pixAndCardDebitUseExpenseDate_cardCreditUsesMonthAfter_incomeUsesIncomeDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		createExpense(token, categoryId, pix, null, "100.00", "2026-08-15");
		createExpense(token, categoryId, nubank, "DEBIT", "200.00", "2026-08-15");
		createExpense(token, categoryId, nubank, "CREDIT", "300.00", "2026-08-15");
		createIncome(token, categoryId, "1000.00", "2026-08-20");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-08-01")
						.param("endDate", "2026-09-30"))
				.andExpect(status().isOk())
				// August: Pix + debit count here (300), credit does not; income counts here too.
				.andExpect(jsonPath("$.incomeVsExpenses[0].month").value(8))
				.andExpect(jsonPath("$.incomeVsExpenses[0].expenseAmount").value(300.00))
				.andExpect(jsonPath("$.incomeVsExpenses[0].incomeAmount").value(1000.00))
				// September: only the credit-card purchase (invoice month).
				.andExpect(jsonPath("$.incomeVsExpenses[1].month").value(9))
				.andExpect(jsonPath("$.incomeVsExpenses[1].expenseAmount").value(300.00))
				.andExpect(jsonPath("$.incomeVsExpenses[1].incomeAmount").value(0));
	}

	@Test
	void expensesByCategory_returnsEmpty_whenNoExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-06-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expensesByCategory.length()").value(0))
				.andExpect(jsonPath("$.expensesByPaymentMethod.length()").value(0))
				.andExpect(jsonPath("$.largestExpenses.length()").value(0));
	}

	@Test
	void expensesByCategory_groupsAndComputesPercentages_orderedByAmountDesc() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		long transport = createCategory(token, "Transport");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		createExpense(token, food, pix, null, "600.00", "2026-06-05");
		createExpense(token, transport, pix, null, "400.00", "2026-06-10");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-06-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expensesByCategory[0].category.name").value("Food"))
				.andExpect(jsonPath("$.expensesByCategory[0].amount").value(600.00))
				.andExpect(jsonPath("$.expensesByCategory[0].percentage").value(60.00))
				.andExpect(jsonPath("$.expensesByCategory[1].category.name").value("Transport"))
				.andExpect(jsonPath("$.expensesByCategory[1].percentage").value(40.00));
	}

	@Test
	void expensesByPaymentMethod_splitsSameCardByCardMode_andComputesTransactionCountAndPercentage() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);

		createExpense(token, categoryId, pix, null, "400.00", "2026-06-05");
		createExpense(token, categoryId, nubank, "CREDIT", "300.00", "2026-06-05");
		createExpense(token, categoryId, nubank, "CREDIT", "300.00", "2026-06-06");
		createExpense(token, categoryId, nubank, "DEBIT", "200.00", "2026-06-05");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expensesByPaymentMethod.length()").value(3))
				.andExpect(jsonPath("$.expensesByPaymentMethod[?(@.name == 'Nubank' && @.cardMode == 'CREDIT')].amount")
						.value(hasItem(600.00)))
				.andExpect(jsonPath("$.expensesByPaymentMethod[?(@.name == 'Nubank' && @.cardMode == 'CREDIT')].transactionCount")
						.value(hasItem(2)))
				.andExpect(jsonPath("$.expensesByPaymentMethod[?(@.name == 'Nubank' && @.cardMode == 'DEBIT')].amount")
						.value(hasItem(200.00)))
				.andExpect(jsonPath("$.expensesByPaymentMethod[?(@.name == 'Pix')].amount").value(hasItem(400.00)))
				.andExpect(jsonPath("$.expensesByPaymentMethod[?(@.name == 'Pix')].percentage").value(hasItem(33.33)));
	}

	@Test
	void largestExpenses_ordersByAmountDesc_limitsToFive_andIsolatesBetweenUsers() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long pix = createTransactionMethod(token, "Pix", "PIX");
		for (int i = 1; i <= 6; i++) {
			createExpense(token, categoryId, pix, null, (i * 100) + ".00", "2026-06-0" + i);
		}

		String otherToken = registerAndLogin("Bob", "bob@example.com", "password123");
		long otherCategory = createCategory(otherToken, "Other");
		long otherPix = createTransactionMethod(otherToken, "Pix", "PIX");
		createExpense(otherToken, otherCategory, otherPix, null, "999999.00", "2026-06-01");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-06-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.largestExpenses.length()").value(5))
				.andExpect(jsonPath("$.largestExpenses[0].amount").value(600.00))
				.andExpect(jsonPath("$.largestExpenses[4].amount").value(200.00));
	}

	@Test
	void paymentMethodsModal_paginatesFiltersAndPercentageAgainstFullPeriodTotal() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long nubank = createTransactionMethod(token, "Nubank", "CARD", 20, 10);
		long pix = createTransactionMethod(token, "Pix", "PIX");
		createExpense(token, categoryId, nubank, "CREDIT", "9860.00", "2026-05-05");
		createExpense(token, categoryId, pix, null, "4180.00", "2026-06-05");

		mockMvc.perform(get("/api/v1/financial-analysis/payment-methods")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-06-30")
						.param("search", "nubank")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Nubank"))
				// Percentage is against the whole period's total expenses (9860 + 4180), not just
				// the filtered/paged result (Nubank alone would be 100%).
				.andExpect(jsonPath("$.content[0].percentage").value(70.23))
				.andExpect(jsonPath("$.totalAmount").value(9860.00))
				.andExpect(jsonPath("$.totalTransactionCount").value(1));
	}

	@Test
	void paymentMethodsModal_invalidSort_returns400() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/financial-analysis/payment-methods")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-06-01")
						.param("endDate", "2026-06-30")
						.param("sort", "unknown,desc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getAnalysis_returns400_whenStartDateAfterEndDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/financial-analysis")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-08-01")
						.param("endDate", "2026-06-01"))
				.andExpect(status().isBadRequest());
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
