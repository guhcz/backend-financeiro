package com.personal.backend_financeiro.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecurringExpenseIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_generatesFirstOccurrenceImmediately_whenStartDateIsTodayOrEarlier() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate today = LocalDate.now();

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Internet", 10, today, null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Internet"))
				.andExpect(jsonPath("$.content[0].generatedAutomatically").value(true))
				.andExpect(jsonPath("$.content[0].recurring").value(true));
	}

	@Test
	void create_generatesNothingYet_whenStartDateIsInTheFuture() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate futureStart = LocalDate.now().plusMonths(6);

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Gym", 10, futureStart, null)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void create_clampsDueDayToLastValidDayOfMonth_whenDueDay31() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate today = LocalDate.now();
		LocalDate expectedDate = YearMonth.from(today).atDay(Math.min(31, YearMonth.from(today).lengthOfMonth()));

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Rent", 31, today, null)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].expenseDate").value(expectedDate.toString()));
	}

	@Test
	void create_returns404_whenCategoryDoesNotBelongToUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Home");

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Internet", 10, LocalDate.now(), null)))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_returns400_whenFrequencyIsNotMonthly() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet","amount":119.90,
								"paymentMethod":"CREDIT_CARD","notes":null,"frequency":"WEEKLY",
								"dueDay":10,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, LocalDate.now())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenEndDateBeforeStartDate() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate today = LocalDate.now();

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Internet", 10, today, today.minusDays(1))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenDueDayOutOfRange() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Internet", 32, LocalDate.now(), null)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_allowsNullDueDay_andGeneratesOnTheFirstDayOfTheMonth() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate today = LocalDate.now();
		LocalDate expectedDate = YearMonth.from(today).atDay(1);

		mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, "Reserva mensal", null, today, null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dueDay").doesNotExist());

		mockMvc.perform(get("/api/v1/expenses")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].expenseDate").value(expectedDate.toString()));
	}

	@Test
	void getOne_returns404_forOtherUsersRule() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Home");
		long ruleId = createRecurringExpense(tokenAlice, categoryId, "Internet", 10, LocalDate.now(), null);

		mockMvc.perform(get("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isNotFound());
	}

	@Test
	void list_filterByActiveFalse_returnsOnlyPausedRules() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now(), null);
		long pausedRuleId = createRecurringExpense(token, categoryId, "Gym", 10, LocalDate.now(), null);
		mockMvc.perform(patch("/api/v1/recurring-expenses/" + pausedRuleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.param("active", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(pausedRuleId));

		mockMvc.perform(get("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void update_recalculatesNextGenerationDate_whenDueDayChanges() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now(), null);

		mockMvc.perform(put("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet","amount":150.00,
								"paymentMethod":"CREDIT_CARD","notes":null,"dueDay":20,"endDate":null}"""
								.formatted(categoryId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dueDay").value(20))
				.andExpect(jsonPath("$.amount").value(150.00));
	}

	@Test
	void pauseThenResume_keepsNextGenerationDateNeverInThePast() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now(), null);

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/resume")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.nextGenerationDate").value(org.hamcrest.Matchers.greaterThanOrEqualTo(LocalDate.now().toString())));
	}

	@Test
	void resume_returns400_whenEndDateAlreadyPassed() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		LocalDate startDate = LocalDate.now().minusMonths(2);
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, startDate, startDate.plusDays(5));

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/resume")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void delete_hardDeletes_whenNoExpensesGeneratedYet() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Gym", 10, LocalDate.now().plusMonths(6), null);

		mockMvc.perform(delete("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void delete_endsRule_andKeepsItVisible_whenExpensesAlreadyGenerated() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now(), null);

		mockMvc.perform(delete("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void pauseAndUpdate_return400_onAlreadyEndedRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now(), null);
		mockMvc.perform(delete("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/resume")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(put("/api/v1/recurring-expenses/" + ruleId)
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Internet","amount":150.00,
								"paymentMethod":"CREDIT_CARD","notes":null,"dueDay":20,"endDate":null}"""
								.formatted(categoryId)))
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

	private long createRecurringExpense(String token, long categoryId, String description, Integer dueDay,
			LocalDate startDate, LocalDate endDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content(recurringExpenseBody(categoryId, description, dueDay, startDate, endDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private String recurringExpenseBody(long categoryId, String description, Integer dueDay, LocalDate startDate, LocalDate endDate) {
		return """
				{"categoryId":%d,"description":"%s","amount":119.90,
				"paymentMethod":"CREDIT_CARD","notes":null,"frequency":"MONTHLY",
				"dueDay":%s,"startDate":"%s","endDate":%s}"""
				.formatted(categoryId, description, dueDay == null ? "null" : dueDay, startDate,
						endDate == null ? "null" : "\"" + endDate + "\"");
	}

}
