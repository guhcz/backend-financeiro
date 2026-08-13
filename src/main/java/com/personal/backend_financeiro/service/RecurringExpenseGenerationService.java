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

	@Transactional
	public void generateInitialOccurrenceIfDue(RecurringExpense rule, LocalDate today) {
		LocalDate candidate = RecurrenceDateCalculator.resolveOccurrenceDate(rule.getStartDate(), rule.getDueDay());
		boolean startedByToday = !rule.getStartDate().isAfter(today);
		boolean withinEndDate = rule.getEndDate() == null || !rule.getEndDate().isBefore(candidate);

		if (!startedByToday || !withinEndDate) {
			return;
		}

		generateOccurrence(rule, rule.getStartDate().getYear(), rule.getStartDate().getMonthValue(), candidate);
		rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(candidate, rule.getDueDay()));
	}

	@Transactional
	public void generateForRule(Long ruleId, LocalDate today) {
		RecurringExpense rule = recurringExpenseRepository.findById(ruleId).orElse(null);
		if (rule == null || rule.getStatus() != RecurrenceStatus.ACTIVE) {
			return;
		}

		LocalDate occurrenceDate = rule.getNextGenerationDate();
		if (occurrenceDate.isAfter(today)) {
			return;
		}
		if (rule.getEndDate() != null && rule.getEndDate().isBefore(occurrenceDate)) {
			return;
		}

		generateOccurrence(rule, occurrenceDate.getYear(), occurrenceDate.getMonthValue(), occurrenceDate);
		rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(occurrenceDate, rule.getDueDay()));
	}

	private void generateOccurrence(RecurringExpense rule, int year, int month, LocalDate expenseDate) {
		boolean alreadyGenerated = expenseRepository
				.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(rule.getId(), year, month);
		if (alreadyGenerated) {
			log.info("Occurrence already generated for recurring expense {} on {}/{}, skipping", rule.getId(), month, year);
			return;
		}

		YearMonth competence = CompetenceResolver.resolve(expenseDate);

		Expense expense = Expense.builder()
				.user(rule.getUser())
				.category(rule.getCategory())
				.description(rule.getDescription())
				.amount(rule.getAmount())
				.expenseDate(expenseDate)
				.paymentMethod(rule.getPaymentMethod())
				.notes(rule.getNotes())
				.creditCard(rule.getCreditCard())
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
