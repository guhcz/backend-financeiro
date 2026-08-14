package com.personal.backend_financeiro.integration;

import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.scheduler.RecurringIncomeGenerationJob;
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
 * Overrides app.recurring-income.scheduler-enabled=true just for this class so the
 * {@link RecurringIncomeGenerationJob} bean exists (it's conditional and disabled by default in
 * application-test.properties). The job's run() method is invoked directly rather than waiting
 * on the real cron trigger. Also pins lookahead-months to a small, easy-to-count value instead
 * of the production default (12).
 */
@TestPropertySource(properties = {
		"app.recurring-income.scheduler-enabled=true",
		"app.recurring-income.lookahead-months=2"
})
class RecurringIncomeGenerationJobIntegrationTest extends AbstractApiIntegrationTest {

	@Autowired
	private RecurringIncomeGenerationJob job;

	@Autowired
	private RecurringIncomeRepository recurringIncomeRepository;

	@Autowired
	private IncomeRepository incomeRepository;

	@Test
	void create_generatesOccurrencesUpToLookaheadHorizon_immediately() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long incomesBefore = incomeRepository.count();
		int receiptDay = Math.min(LocalDate.now().getDayOfMonth(), 28);

		long ruleId = createRecurringIncome(token, categoryId, "Salário", receiptDay, LocalDate.now());

		// lookahead-months=2 means "this occurrence + 2 more months ahead" = 3, generated
		// synchronously at creation time, with no need for the daily job to ever run.
		assertThat(incomeRepository.count()).isEqualTo(incomesBefore + 3);
		RecurringIncome rule = recurringIncomeRepository.findById(ruleId).orElseThrow();
		assertThat(rule.getNextGenerationDate()).isAfter(LocalDate.now().plusMonths(2));
	}

	@Test
	void run_catchesUpEveryOccurrence_upToLookaheadHorizon_inOneRun() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		// startDate beyond the 2-month horizon so creation itself generates nothing yet.
		long ruleId = createRecurringIncome(token, categoryId, "Salário", 5, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);
		long incomesBefore = incomeRepository.count();

		job.run();

		// Catches up today's occurrence plus every month within the horizon in this single run.
		assertThat(incomeRepository.count()).isEqualTo(incomesBefore + 3);
		RecurringIncome rule = recurringIncomeRepository.findById(ruleId).orElseThrow();
		assertThat(rule.getNextGenerationDate()).isAfter(LocalDate.now().plusMonths(2));
	}

	@Test
	void run_skipsPausedRule() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long ruleId = createRecurringIncome(token, categoryId, "Renda extra", 5, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);

		mockMvc.perform(patch("/api/v1/recurring-incomes/" + ruleId + "/pause")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		long incomesBefore = incomeRepository.count();
		job.run();

		assertThat(incomeRepository.count()).isEqualTo(incomesBefore);
	}

	@Test
	void run_doesNotGenerateFurther_onceCaughtUpToHorizon() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long ruleId = createRecurringIncome(token, categoryId, "Salário", 5, LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);

		job.run();
		long incomesAfterFirstRun = incomeRepository.count();
		job.run();

		assertThat(incomeRepository.count()).isEqualTo(incomesAfterFirstRun);
	}

	@Test
	void run_generatesOccurrences_whenReceiptDayIsNull() throws Exception {
		String token = registerAndLogin("Alice", "alice@example.com", "password123");
		long categoryId = createCategory(token, "Trabalho");
		long ruleId = createRecurringIncomeWithNullReceiptDay(token, categoryId, "Renda variável", LocalDate.now().plusMonths(6));
		makeRuleDueToday(ruleId);

		long incomesBefore = incomeRepository.count();
		job.run();

		assertThat(incomeRepository.count()).isEqualTo(incomesBefore + 3);
	}

	private void makeRuleDueToday(long ruleId) {
		RecurringIncome rule = recurringIncomeRepository.findById(ruleId).orElseThrow();
		rule.setNextGenerationDate(LocalDate.now());
		recurringIncomeRepository.save(rule);
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

	private long createRecurringIncomeWithNullReceiptDay(String token, long categoryId, String description, LocalDate startDate) throws Exception {
		var result = mockMvc.perform(post("/api/v1/recurring-incomes")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"description":"%s","amount":800.00,
								"receiptMethod":"BANK_TRANSFER","notes":null,"frequency":"MONTHLY",
								"receiptDay":null,"startDate":"%s","endDate":null}"""
								.formatted(categoryId, description, startDate)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

}
