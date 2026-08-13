package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyPlanningIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns201() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":2026,"amount":1000.00}"""
								.formatted(categoryId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.id").value(categoryId))
				.andExpect(jsonPath("$.amount").value(1000.00));
	}

	@Test
	void create_returns409_onDuplicateCategoryPeriod() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		createPlanning(token, categoryId, 8, 2026, "1000.00");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":2026,"amount":500.00}"""
								.formatted(categoryId)))
				.andExpect(status().isConflict());
	}

	@Test
	void create_returns400_whenMonthBelowRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":0,"year":2026,"amount":500.00}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenMonthAboveRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":13,"year":2026,"amount":500.00}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenYearBelowMinimum() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":1999,"amount":500.00}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenAmountIsZero() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":2026,"amount":0}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns404_whenCategoryDoesNotExist() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":9999,"month":8,"year":2026,"amount":500.00}"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_returns404_whenCategoryBelongsToAnotherUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Food");

		mockMvc.perform(post("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":2026,"amount":500.00}"""
								.formatted(categoryId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void list_isIsolatedPerUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryAlice = createCategory(tokenAlice, "Food");
		createPlanning(tokenAlice, categoryAlice, 8, 2026, "1000.00");

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + tokenBob)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void list_returnsPagedResults() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		long transport = createCategory(token, "Transport");
		createPlanning(token, food, 8, 2026, "1000.00");
		createPlanning(token, transport, 8, 2026, "500.00");

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026")
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void list_computesSpentRemainingAndPercentage() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		createPlanning(token, categoryId, 8, 2026, "1000.00");
		// Competence is always the month after the expense date.
		createExpense(token, categoryId, "620.00", "2026-07-05");

		mockMvc.perform(get("/api/v1/monthly-plannings")
						.header("Authorization", "Bearer " + token)
						.param("month", "8")
						.param("year", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].plannedAmount").value(1000.00))
				.andExpect(jsonPath("$.content[0].spentAmount").value(620.00))
				.andExpect(jsonPath("$.content[0].remainingAmount").value(380.00))
				.andExpect(jsonPath("$.content[0].percentageUsed").value(62.00));
	}

	@Test
	void update_returns409_whenMovingToPeriodAlreadyOccupied() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long food = createCategory(token, "Food");
		long transport = createCategory(token, "Transport");
		createPlanning(token, food, 8, 2026, "1000.00");
		long transportPlanningId = createPlanning(token, transport, 9, 2026, "500.00");

		mockMvc.perform(put("/api/v1/monthly-plannings/" + transportPlanningId)
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"month":8,"year":2026,"amount":500.00}"""
								.formatted(food)))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_thenGet_returns404() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Food");
		long planningId = createPlanning(token, categoryId, 8, 2026, "1000.00");

		mockMvc.perform(delete("/api/v1/monthly-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/monthly-plannings/" + planningId)
						.header("Authorization", "Bearer " + token))
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

	private long createExpense(String token, long categoryId, String amount, String date) throws Exception {
		var result = mockMvc.perform(post("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Expense","amount":%s,
								"expenseDate":"%s","paymentMethod":"PIX","notes":null}"""
								.formatted(categoryId, amount, date)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
