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
 * rather than waiting on the real cron trigger. Also pins lookahead-months to a small,
 * easy-to-count value instead of the production default (12).
 */
@TestPropertySource(properties = {
		"app.recurring-expense.scheduler-enabled=true",
		"app.recurring-expense.lookahead-months=2"
})
class RecurringExpenseGenerationJobIntegrationTest extends AbstractApiIntegrationTest {

	@Autowired
	private RecurringExpenseGenerationJob job;

	@Autowired
	private RecurringExpenseRepository recurringExpenseRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Test
	void create_generatesOccurrencesUpToLookaheadHorizon_immediately() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		long expensesBefore = expenseRepository.count();
		int dueDay = Math.min(LocalDate.now().getDayOfMonth(), 28);

		long ruleId = createRecurringExpense(token, categoryId, "Internet", dueDay, LocalDate.now());

		// lookahead-months=2 means "this occurrence + 2 more months ahead" = 3, generated
		// synchronously at creation time, with no need for the daily job to ever run.
		assertThat(expenseRepository.count()).isEqualTo(expensesBefore + 3);
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElseThrow();
		assertThat(rule.getNextGenerationDate()).isAfter(LocalDate.now().plusMonths(2));
	}

	@Test
	void run_catchesUpEveryOccurrence_upToLookaheadHorizon_inOneRun() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Home");
		// startDate beyond the 2-month horizon so creation itself generates nothing yet.
		long ruleId = createRecurringExpense(token, categoryId, "Internet", 10, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);
		long expensesBefore = expenseRepository.count();

		job.run();

		// Catches up today's occurrence plus every month within the horizon in this single run.
		assertThat(expenseRepository.count()).isEqualTo(expensesBefore + 3);
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElseThrow();
		assertThat(rule.getNextGenerationDate()).isAfter(LocalDate.now().plusMonths(2));
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
	void run_doesNotGenerateFurther_onceCaughtUpToHorizon() throws Exception {
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
								"transactionMethodId":%d,"cardTransactionMode":null,"notes":null,"frequency":"MONTHLY",
								"dueDay":%d,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, description, createTransactionMethod(token, "Pix", "PIX"), dueDay, startDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
