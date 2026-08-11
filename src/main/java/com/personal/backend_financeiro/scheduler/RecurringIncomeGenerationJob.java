package com.personal.backend_financeiro.scheduler;

import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.service.RecurringIncomeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.recurring-income", name = "scheduler-enabled", matchIfMissing = true)
public class RecurringIncomeGenerationJob {

	private final RecurringIncomeRepository recurringIncomeRepository;
	private final RecurringIncomeGenerationService generationService;

	@Scheduled(cron = "${app.recurring-income.generation-cron:0 0 1 * * *}")
	public void run() {
		LocalDate today = LocalDate.now();
		for (Long ruleId : recurringIncomeRepository.findEligibleRuleIds(today)) {
			try {
				generationService.generateForRule(ruleId, today);
			} catch (Exception e) {
				log.error("Failed to generate occurrence for recurring income rule {}", ruleId, e);
			}
		}
	}

}
