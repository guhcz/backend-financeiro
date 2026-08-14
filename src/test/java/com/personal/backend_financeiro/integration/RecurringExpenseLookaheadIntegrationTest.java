package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the actual user-facing feature: a fixed expense should already show up in future
 * months' Movimentações as soon as it's created, not only once its due date arrives — and
 * editing/ending the rule afterward must keep those already-generated future months in sync.
 * Pins lookahead-months to a small value so counts are easy to reason about.
 */
@TestPropertySource(properties = "app.recurring-expense.lookahead-months=2")
class RecurringExpenseLookaheadIntegrationTest extends AbstractApiIntegrationTest {

	private final java.util.Map<String, Long> pixMethodByToken = new java.util.HashMap<>();

	@Test
	void create_makesFixedExpenseVisibleInFutureMonths_immediately() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		int dueDay = Math.min(LocalDate.now().getDayOfMonth(), 28);

		createRecurringExpense(token, categoryId, "Internet", dueDay, LocalDate.now(), null);

		// lookahead-months=2: this month plus 2 ahead, so a date 2 months out must already exist,
		// without the daily job ever running -- this is the behavior the user asked for.
		LocalDate twoMonthsOut = LocalDate.now().plusMonths(2);
		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true")
						.param("startDate", twoMonthsOut.withDayOfMonth(1).toString())
						.param("endDate", twoMonthsOut.withDayOfMonth(twoMonthsOut.lengthOfMonth()).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Internet"));
	}

	@Test
	void update_appliesNewAmountToAlreadyGeneratedFutureMonths() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		int dueDay = Math.min(LocalDate.now().getDayOfMonth(), 28);
		long ruleId = createRecurringExpense(token, categoryId, "Internet", dueDay, LocalDate.now(), null);

		mockMvc.perform(put("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet","amount":199.90,
								"transactionMethodId":%d,"cardTransactionMode":null,"notes":null,"dueDay":%d,"endDate":null}"""
								.formatted(categoryId, pixMethod(token), dueDay)))
				.andExpect(status().isOk());

		LocalDate twoMonthsOut = LocalDate.now().plusMonths(2);
		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true")
						.param("startDate", twoMonthsOut.withDayOfMonth(1).toString())
						.param("endDate", twoMonthsOut.withDayOfMonth(twoMonthsOut.lengthOfMonth()).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].amount").value(199.90));
	}

	@Test
	void delete_removesAlreadyGeneratedFutureMonths_butKeepsPastOnes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		int dueDay = Math.min(LocalDate.now().getDayOfMonth(), 28);
		// Started a month ago, so creation generates last month (past), this month, +1 and +2 ahead.
		long ruleId = createRecurringExpense(token, categoryId, "Internet", dueDay, LocalDate.now().minusMonths(1), null);

		mockMvc.perform(delete("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		LocalDate lastMonth = LocalDate.now().minusMonths(1);
		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true")
						.param("startDate", lastMonth.withDayOfMonth(1).toString())
						.param("endDate", lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		LocalDate twoMonthsOut = LocalDate.now().plusMonths(2);
		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true")
						.param("startDate", twoMonthsOut.withDayOfMonth(1).toString())
						.param("endDate", twoMonthsOut.withDayOfMonth(twoMonthsOut.lengthOfMonth()).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
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

	private long createRecurringExpense(String token, long categoryId, String description, int dueDay,
			LocalDate startDate, LocalDate endDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":119.90,
								"transactionMethodId":%d,"cardTransactionMode":null,"notes":null,"frequency":"MONTHLY",
								"dueDay":%d,"startDate":"%s","endDate":%s}"""
								.formatted(categoryId, description, pixMethod(token), dueDay, startDate,
										endDate == null ? "null" : "\"" + endDate + "\"")))
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

}
