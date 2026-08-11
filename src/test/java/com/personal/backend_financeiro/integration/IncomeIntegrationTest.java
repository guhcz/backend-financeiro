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

class IncomeIntegrationTest extends AbstractApiIntegrationTest {

	@Test
	void create_returns404_whenCategoryDoesNotBelongToUser() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Trabalho");

		mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + tokenBob)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Hack","amount":1.00,
								"incomeDate":"2026-07-01","receiptMethod":"OTHER","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_returns400_whenAmountIsNotPositive() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");

		mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Salário","amount":0,
								"incomeDate":"2026-07-01","receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400_whenIncomeDateIsMissing() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");

		mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Salário","amount":100.00,
								"receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void filter_byDateRange_returnsOnlyMatchingIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createIncome(token, categoryId, "Salário", "2026-07-10");
		createIncome(token, categoryId, "Freelance", "2026-06-20");

		mockMvc.perform(get("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.param("startDate", "2026-07-01")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Salário"));
	}

	@Test
	void filter_withoutParams_returnsAllOwnIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createIncome(token, categoryId, "Salário", "2026-07-10");
		createIncome(token, categoryId, "Freelance", "2026-06-20");

		mockMvc.perform(get("/api/v1/incomes")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void filter_byReceiptMethod_returnsOnlyMatchingIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createIncome(token, categoryId, "Salário", "2026-07-10");
		createIncomeWithMethod(token, categoryId, "Venda", "2026-07-12", "CASH");

		mockMvc.perform(get("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.param("receiptMethod", "CASH"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Venda"));
	}

	@Test
	void getOne_returns404_forOtherUsersIncome() throws Exception {
		String tokenAlice = registerAndLogin("Alice", "alice@example.com", "password123");
		String tokenBob = registerAndLogin("Bob", "bob@example.com", "password123");
		long categoryId = createCategory(tokenAlice, "Trabalho");
		long incomeId = createIncome(tokenAlice, categoryId, "Salário", "2026-07-10");

		mockMvc.perform(get("/api/v1/incomes/" + incomeId)
						.header("Authorization", "Bearer " + tokenBob))
				.andExpect(status().isNotFound());
	}

	@Test
	void create_manualIncome_hasRecurrenceFieldsFalseAndNull() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long incomeId = createIncome(token, categoryId, "Salário", "2026-07-10");

		mockMvc.perform(get("/api/v1/incomes/" + incomeId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generatedAutomatically").value(false))
				.andExpect(jsonPath("$.recurring").value(false))
				.andExpect(jsonPath("$.recurringIncomeId").doesNotExist());
	}

	@Test
	void filter_byRecurring_separatesManualFromGeneratedIncomes() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		createIncome(token, categoryId, "Manual income", "2026-07-10");
		createRecurringIncome(token, categoryId, "Salário", 5, LocalDate.now());

		mockMvc.perform(get("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Salário"));

		mockMvc.perform(get("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.param("recurring", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].description").value("Manual income"));
	}

	@Test
	void update_withScopeThisAndFuture_alsoUpdatesRecurringRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long ruleId = createRecurringIncome(token, categoryId, "Salário", 5, LocalDate.now());
		long incomeId = onlyGeneratedIncomeId(token);

		mockMvc.perform(put("/api/v1/incomes/" + incomeId)
						.header("Authorization", "Bearer " + token)
						.param("scope", "THIS_AND_FUTURE")
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"Salário Plus","amount":6800.00,
								"incomeDate":"2026-07-05","receiptMethod":"PIX","notes":null}"""
								.formatted(categoryId)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/recurring-incomes/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Salário Plus"))
				.andExpect(jsonPath("$.amount").value(6800.00))
				.andExpect(jsonPath("$.receiptMethod").value("PIX"));
	}

	@Test
	void delete_withScopeThisAndFuture_preservesIncomeHistory_andEndsRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long ruleId = createRecurringIncome(token, categoryId, "Salário", 5, LocalDate.now());
		long incomeId = onlyGeneratedIncomeId(token);

		mockMvc.perform(delete("/api/v1/incomes/" + incomeId)
						.header("Authorization", "Bearer " + token)
						.param("scope", "THIS_AND_FUTURE"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/incomes/" + incomeId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/recurring-incomes/" + ruleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private long createRecurringIncome(String token, long categoryId, String description, int receiptDay, LocalDate startDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":6200.00,
								"receiptMethod":"BANK_TRANSFER","notes":null,"frequency":"MONTHLY",
								"receiptDay":%d,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, description, receiptDay, startDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long onlyGeneratedIncomeId(String token) throws Exception {
		var result = mockMvc.perform(get("/api/v1/incomes")
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

	private long createIncome(String token, long categoryId, String description, String date) throws Exception {
		return createIncomeWithMethod(token, categoryId, description, date, "PIX");
	}

	private long createIncomeWithMethod(String token, long categoryId, String description, String date, String receiptMethod) throws Exception {
		var result = mockMvc.perform(post("/api/v1/incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":10.00,
								"incomeDate":"%s","receiptMethod":"%s","notes":null}"""
								.formatted(categoryId, description, date, receiptMethod)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
