package com.personal.backend_financeiro.scheduler;

import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.service.RecurringExpenseGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.recurring-expense", name = "scheduler-enabled", matchIfMissing = true)
public class RecurringExpenseGenerationJob {

	private final RecurringExpenseRepository recurringExpenseRepository;
	private final RecurringExpenseGenerationService generationService;

	@Scheduled(cron = "${app.recurring-expense.generation-cron:0 0 1 * * *}")
	public void run() {
		LocalDate today = LocalDate.now();
		LocalDate horizonEnd = generationService.horizonEnd(today);
		for (Long ruleId : recurringExpenseRepository.findEligibleRuleIds(horizonEnd)) {
			try {
				generationService.generateForRule(ruleId, today);
			} catch (Exception e) {
				log.error("Failed to generate occurrence for recurring expense rule {}", ruleId, e);
			}
		}
	}

}
