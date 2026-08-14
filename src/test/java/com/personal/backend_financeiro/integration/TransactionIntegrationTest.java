package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIntegrationTest extends AbstractApiIntegrationTest {

	private final java.util.Map<String, Long> pixMethodByToken = new java.util.HashMap<>();

	@Test
	void filter_byTypeExpense_returnsOnlyExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Supermercado", "50.00", "2026-08-01");
		createIncome(token, categoryId, "Salário", "6200.00", "2026-08-05");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("type", "EXPENSE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
				.andExpect(jsonPath("$.content[0].description").value("Supermercado"));
	}

	@Test
	void filter_byTypeIncome_returnsOnlyIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Supermercado", "50.00", "2026-08-01");
		createIncome(token, categoryId, "Salário", "6200.00", "2026-08-05");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("type", "INCOME"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].type").value("INCOME"))
				.andExpect(jsonPath("$.content[0].description").value("Salário"));
	}

	@Test
	void filter_withoutType_returnsExpensesAndIncomesMerged() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Supermercado", "50.00", "2026-08-01");
		createIncome(token, categoryId, "Salário", "6200.00", "2026-08-05");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void filter_ordersByDateDesc_asDefault_acrossExpensesAndIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Oldest", "10.00", "2026-08-01");
		createIncome(token, categoryId, "Middle", "20.00", "2026-08-10");
		createExpense(token, categoryId, "Newest", "30.00", "2026-08-20");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].description").value("Newest"))
				.andExpect(jsonPath("$.content[1].description").value("Middle"))
				.andExpect(jsonPath("$.content[2].description").value("Oldest"));
	}

	@Test
	void filter_ordersByAmountAsc_whenSortRequested() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Big", "300.00", "2026-08-01");
		createIncome(token, categoryId, "Small", "10.00", "2026-08-01");
		createExpense(token, categoryId, "Medium", "150.00", "2026-08-01");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("sort", "amount,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].description").value("Small"))
				.andExpect(jsonPath("$.content[1].description").value("Medium"))
				.andExpect(jsonPath("$.content[2].description").value("Big"));
	}

	@Test
	void filter_returns400_whenSortPropertyIsNotAllowed() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("sort", "description,asc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void filter_paginatesAcrossExpensesAndIncomes_withCorrectTotalElements() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		for (int i = 0; i < 3; i++) {
			createExpense(token, categoryId, "Expense " + i, "10.00", "2026-08-0" + (i + 1));
		}
		for (int i = 0; i < 3; i++) {
			createIncome(token, categoryId, "Income " + i, "10.00", "2026-08-1" + (i + 1));
		}

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("page", "0")
						.param("size", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(6))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.content.length()").value(4));

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("page", "1")
						.param("size", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2));
	}

	@Test
	void filter_byCategory_returnsOnlyMatchingTransactions() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryWork = createCategory(token, "Trabalho");
		long categoryFood = createCategory(token, "Alimentação");
		createIncome(token, categoryWork, "Salário", "6200.00", "2026-08-05");
		createExpense(token, categoryFood, "Supermercado", "50.00", "2026-08-01");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("categoryId", String.valueOf(categoryWork)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Salário"));
	}

	@Test
	void filter_byDateRange_returnsOnlyMatchingTransactions() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "July expense", "10.00", "2026-07-15");
		createIncome(token, categoryId, "August income", "10.00", "2026-08-15");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-08-01")
						.param("endDate", "2026-08-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("August income"));
	}

	@Test
	void filter_byPartialDescription_matchesBothTypesCaseInsensitively() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Pagamento aluguel", "800.00", "2026-08-01");
		createIncome(token, categoryId, "Reembolso pagamento", "50.00", "2026-08-02");
		createIncome(token, categoryId, "Salário", "6200.00", "2026-08-05");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("description", "PAGAMENTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void filter_byRecurring_matchesBothTypes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createExpense(token, categoryId, "Manual expense", "10.00", "2026-08-01");
		createIncome(token, categoryId, "Manual income", "10.00", "2026-08-01");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void filter_expenseAndIncomeWithSameNumericId_haveDistinctTransactionKeys() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long expenseId = createExpense(token, categoryId, "First expense", "10.00", "2026-08-01");
		long incomeId = createIncome(token, categoryId, "First income", "10.00", "2026-08-01");

		// Both are the first row created for their respective tables, so it's plausible they
		// share the same numeric id — the response must still disambiguate them via type/transactionKey.
		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[?(@.type == 'EXPENSE')].id").value(org.hamcrest.Matchers.hasItem((int) expenseId)))
				.andExpect(jsonPath("$.content[?(@.type == 'INCOME')].id").value(org.hamcrest.Matchers.hasItem((int) incomeId)))
				.andExpect(jsonPath("$.content[?(@.type == 'EXPENSE')].transactionKey").value(org.hamcrest.Matchers.hasItem("EXPENSE-" + expenseId)))
				.andExpect(jsonPath("$.content[?(@.type == 'INCOME')].transactionKey").value(org.hamcrest.Matchers.hasItem("INCOME-" + incomeId)));
	}

	@Test
	void filter_isolatesTransactionsBetweenUsers() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryAlice = createCategory(tokenAlice, "Trabalho");
		long categoryBob = createCategory(tokenBob, "Trabalho");
		createExpense(tokenAlice, categoryAlice, "Alice expense", "10.00", "2026-08-01");
		createIncome(tokenAlice, categoryAlice, "Alice income", "10.00", "2026-08-01");
		createIncome(tokenBob, categoryBob, "Bob income", "10.00", "2026-08-01");

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + tokenAlice))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
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

	private long createExpense(String token, long categoryId, String description, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":%s,
								"expenseDate":"%s","transactionMethodId":%d,"cardTransactionMode":null,"notes":null}"""
								.formatted(categoryId, description, amount, date, pixMethod(token))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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

	private long createIncome(String token, long categoryId, String description, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":%s,
								"incomeDate":"%s","receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId, description, amount, date)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
