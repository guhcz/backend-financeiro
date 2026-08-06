package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns404_whenCategoryDoesNotBelongToUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Food");

		mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Hack","amount":1.00,
								"expenseDate":"2026-07-01","paymentMethod":"OTHER","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void filter_byDateRange_returnsOnlyMatchingExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		createExpense(token, categoryId, "Lunch", "2026-07-10");
		createExpense(token, categoryId, "Dinner", "2026-06-20");

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-07-01")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Lunch"));
	}

	@Test
	void filter_withoutParams_returnsAllOwnExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		createExpense(token, categoryId, "Lunch", "2026-07-10");
		createExpense(token, categoryId, "Dinner", "2026-06-20");

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void getOne_returns404_forOtherUsersExpense() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Food");
		long expenseId = createExpense(tokenAlice, categoryId, "Lunch", "2026-07-10");

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_manualExpense_hasRecurrenceFieldsFalseAndNull() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long expenseId = createExpense(token, categoryId, "Lunch", "2026-07-10");

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generatedAutomatically").value(false))
				.andExpect(jsonPath("$.recurring").value(false))
				.andExpect(jsonPath("$.recurringExpenseId").doesNotExist());
	}

	@Test
	void filter_byRecurring_separatesManualFromGeneratedExpenses() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		createExpense(token, categoryId, "Manual expense", "2026-07-10");
		createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now());

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Internet"));

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Manual expense"));
	}

	@Test
	void update_withScopeThisAndFuture_alsoUpdatesRecurringRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now());
		long expenseId = onlyGeneratedExpenseId(token);

		mockMvc.perform(put("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token)
						.param("scope", "THIS_AND_FUTURE")
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet Plus","amount":199.90,
								"expenseDate":"2026-07-10","paymentMethod":"PIX","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Internet Plus"))
				.andExpect(jsonPath("$.amount").value(199.90))
				.andExpect(jsonPath("$.paymentMethod").value("PIX"));
	}

	@Test
	void delete_withScopeThisAndFuture_preservesExpenseHistory_andEndsRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now());
		long expenseId = onlyGeneratedExpenseId(token);

		mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token)
						.param("scope", "THIS_AND_FUTURE"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/expenses/" + expenseId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private long createRecurringExpense(String token, long categoryId, String description, int dueDay, LocalDate startDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":119.90,
								"paymentMethod":"CREDIT_CARD","notes":null,"frequency":"MONTHLY",
								"dueDay":%d,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, description, dueDay, startDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long onlyGeneratedExpenseId(String token) throws Exception {
		var result = mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("content").get(0).get("id").asLong();
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

	private long createExpense(String token, long categoryId, String description, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":10.00,
								"expenseDate":"%s","paymentMethod":"PIX","notes":null}"""
								.formatted(categoryId, description, date)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
