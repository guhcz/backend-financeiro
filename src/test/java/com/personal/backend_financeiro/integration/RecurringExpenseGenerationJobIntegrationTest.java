package com.personal.backend_financeiro.integration;

import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.scheduler.RecurringExpenseGenerationJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Overrides app.recurring-expense.scheduler-enabled=true just for this class so the
 * {@link RecurringExpenseGenerationJob} bean exists (it's conditional and disabled by
 * default in application-test.properties). The job's run() method is invoked directly
 * rather than waiting on the real cron trigger.
 */
@TestPropertySource(properties = "app.recurring-expense.scheduler-enabled=true")
class RecurringExpenseGenerationJobIntegrationTest extends AbstractApiIntegrationTest {

	@Autowired
	private RecurringExpenseGenerationJob job;

	@Autowired
	private RecurringExpenseRepository recurringExpenseRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Test
	void run_generatesOccurrence_andAdvancesNextGenerationDate_forEligibleRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now().plusMonths(6));

		makeRuleDueToday(ruleId);
		long expensesBefore = expenseRepository.count();

		job.run();

		assertThat(expenseRepository.count()).isEqualTo(expensesBefore + 1);
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElseThrow();
		assertThat(rule.getNextGenerationDate()).isAfter(LocalDate.now());
	}

	@Test
	void run_skipsPausedRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Gym", 10, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);

		mockMvc.perform(patch("/api/v1/recurring-expenses/" + ruleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		long expensesBefore = expenseRepository.count();
		job.run();

		assertThat(expenseRepository.count()).isEqualTo(expensesBefore);
	}

	@Test
	void run_doesNotDuplicate_whenExecutedTwiceForTheSameDueOccurrence() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);

		job.run();
		long expensesAfterFirstRun = expenseRepository.count();
		job.run();

		assertThat(expenseRepository.count()).isEqualTo(expensesAfterFirstRun);
	}

	private void makeRuleDueToday(long ruleId) {
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElseThrow();
		rule.setNextGenerationDate(LocalDate.now());
		recurringExpenseRepository.save(rule);
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

	private long createRecurringExpense(String token, long categoryId, String description, int dueDay, LocalDate startDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-expenses")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":119.90,
								"paymentMethod":"PIX","notes":null,"frequency":"MONTHLY",
								"dueDay":%d,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, description, dueDay, startDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
