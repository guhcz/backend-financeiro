package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
