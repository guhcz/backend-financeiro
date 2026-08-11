package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.util.RecurrenceDateCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringIncomeGenerationService {

	private final IncomeRepository incomeRepository;
	private final RecurringIncomeRepository recurringIncomeRepository;

	@Transactional
	public void generateInitialOccurrenceIfDue(RecurringIncome rule, LocalDate today) {
		LocalDate candidate = RecurrenceDateCalculator.resolveOccurrenceDate(rule.getStartDate(), rule.getReceiptDay());
		boolean startedByToday = !rule.getStartDate().isAfter(today);
		boolean withinEndDate = rule.getEndDate() == null || !rule.getEndDate().isBefore(candidate);

		if (!startedByToday || !withinEndDate) {
			return;
		}

		generateOccurrence(rule, rule.getStartDate().getYear(), rule.getStartDate().getMonthValue(), candidate);
		rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(candidate, rule.getReceiptDay()));
	}

	@Transactional
	public void generateForRule(Long ruleId, LocalDate today) {
		RecurringIncome rule = recurringIncomeRepository.findById(ruleId).orElse(null);
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
		rule.setNextGenerationDate(RecurrenceDateCalculator.nextMonthOccurrence(occurrenceDate, rule.getReceiptDay()));
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
