package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.util.RecurrenceDateCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringIncomeGenerationService {

	private final IncomeRepository incomeRepository;
	private final RecurringIncomeRepository recurringIncomeRepository;

	@Value("${app.recurring-income.lookahead-months:12}")
	private int lookAheadMonths;

	@Transactional
	public void generateInitialOccurrenceIfDue(RecurringIncome rule, LocalDate today) {
		generateUpTo(rule, horizonEnd(today));
	}

	@Transactional
	public void generateForRule(Long ruleId, LocalDate today) {
		RecurringIncome rule = recurringIncomeRepository.findById(ruleId).orElse(null);
		if (rule == null || rule.getStatus() != RecurrenceStatus.ACTIVE) {
			return;
		}
		generateUpTo(rule, horizonEnd(today));
	}

	/**
	 * The last day of the (today + lookAheadMonths)th month, not a fixed day count — so the
	 * current month's occurrence is always covered by lookahead-months=0 regardless of whether
	 * today happens to fall before or after the rule's receipt day within the month, and each
	 * additional lookahead month always adds one full calendar month.
	 */
	public LocalDate horizonEnd(LocalDate today) {
		return YearMonth.from(today).plusMonths(lookAheadMonths).atEndOfMonth();
	}

	/**
	 * Generates every not-yet-created occurrence from the rule's current nextGenerationDate up
	 * through horizonEnd, so fixed incomes are visible in Movimentações months ahead of their
	 * actual receipt date instead of only appearing once the day arrives. Advances
	 * nextGenerationDate one month at a time as it goes, so the next call (whether right after
	 * creation or from the next day's job run) picks up exactly where this one left off.
	 */
	private void generateUpTo(RecurringIncome rule, LocalDate horizonEnd) {
		while (true) {
			LocalDate occurrenceDate = rule.getNextGenerationDate();
			if (occurrenceDate.isAfter(horizonEnd)) {
				break;
			}
			if (rule.getEndDate() != null && rule.getEndDate().isBefore(occurrenceDate)) {
				break;
			}
			generateOccurrence(rule, occurrenceDate.getYear(), occurrenceDate.getMonthValue(), occurrenceDate);
			rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(occurrenceDate, rule.getReceiptDay()));
		}
	}

	private void generateOccurrence(RecurringIncome rule, int year, int month, LocalDate incomeDate) {
		boolean alreadyGenerated = incomeRepository
				.existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(rule.getId(), year, month);
		if (alreadyGenerated) {
			log.info("Occurrence already generated for recurring income {} on {}/{}, skipping", rule.getId(), month, year);
			return;
		}

		Income income = Income.builder()
				.user(rule.getUser())
				.category(rule.getCategory())
				.description(rule.getDescription())
				.amount(rule.getAmount())
				.incomeDate(incomeDate)
				.receiptMethod(rule.getReceiptMethod())
				.notes(rule.getNotes())
				.recurringIncome(rule)
				.generatedAutomatically(true)
				.recurrenceReferenceYear(year)
				.recurrenceReferenceMonth(month)
				.build();

		try {
			incomeRepository.save(income);
		} catch (DataIntegrityViolationException e) {
			log.warn("Concurrent generation detected for recurring income {} on {}/{}, skipping", rule.getId(), month, year, e);
		}
	}

}
