package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.util.CompetenceResolver;
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
public class RecurringExpenseGenerationService {

	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseRepository recurringExpenseRepository;

	@Value("${app.recurring-expense.lookahead-months:12}")
	private int lookAheadMonths;

	@Transactional
	public void generateInitialOccurrenceIfDue(RecurringExpense rule, LocalDate today) {
		generateUpTo(rule, horizonEnd(today));
	}

	@Transactional
	public void generateForRule(Long ruleId, LocalDate today) {
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElse(null);
		if (rule == null || rule.getStatus() != RecurrenceStatus.ACTIVE) {
			return;
		}
		generateUpTo(rule, horizonEnd(today));
	}

	/**
	 * The last day of the (today + lookAheadMonths)th month, not a fixed day count — so the
	 * current month's occurrence is always covered by lookahead-months=0 regardless of whether
	 * today happens to fall before or after the rule's due day within the month, and each
	 * additional lookahead month always adds one full calendar month.
	 */
	public LocalDate horizonEnd(LocalDate today) {
		return YearMonth.from(today).plusMonths(lookAheadMonths).atEndOfMonth();
	}

	/**
	 * Generates every not-yet-created occurrence from the rule's current nextGenerationDate up
	 * through horizonEnd, so fixed expenses are visible in Movimentações months ahead of their
	 * actual due date instead of only appearing once the day arrives. Advances nextGenerationDate
	 * one month at a time as it goes, so the next call (whether right after creation or from the
	 * next day's job run) picks up exactly where this one left off.
	 */
	private void generateUpTo(RecurringExpense rule, LocalDate horizonEnd) {
		while (true) {
			LocalDate occurrenceDate = rule.getNextGenerationDate();
			if (occurrenceDate.isAfter(horizonEnd)) {
				break;
			}
			if (rule.getEndDate() != null && rule.getEndDate().isBefore(occurrenceDate)) {
				break;
			}
			generateOccurrence(rule, occurrenceDate.getYear(), occurrenceDate.getMonthValue(), occurrenceDate);
			rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(occurrenceDate, rule.getDueDay()));
		}
	}

	private void generateOccurrence(RecurringExpense rule, int year, int month, LocalDate expenseDate) {
		boolean alreadyGenerated = expenseRepository
				.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(rule.getId(), year, month);
		if (alreadyGenerated) {
			log.info("Occurrence already generated for recurring expense {} on {}/{}, skipping", rule.getId(), month, year);
			return;
		}

		YearMonth competence = CompetenceResolver.resolve(expenseDate, rule.getTransactionMethod().getType(), rule.getCardTransactionMode());

		Expense expense = Expense.builder()
				.user(rule.getUser())
				.category(rule.getCategory())
				.description(rule.getDescription())
				.amount(rule.getAmount())
				.expenseDate(expenseDate)
				.transactionMethod(rule.getTransactionMethod())
				.cardTransactionMode(rule.getCardTransactionMode())
				.notes(rule.getNotes())
				.billingMonth(competence.getMonthValue())
				.billingYear(competence.getYear())
				.recurringExpense(rule)
				.generatedAutomatically(true)
				.recurrenceReferenceYear(year)
				.recurrenceReferenceMonth(month)
				.build();

		try {
			expenseRepository.save(expense);
		} catch (DataIntegrityViolationException e) {
			log.warn("Concurrent generation detected for recurring expense {} on {}/{}, skipping", rule.getId(), month, year, e);
		}
	}

}
