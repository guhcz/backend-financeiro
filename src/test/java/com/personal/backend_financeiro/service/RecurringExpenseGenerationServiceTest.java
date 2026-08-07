package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.enums.PaymentMethod;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseGenerationServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private RecurringExpenseRepository recurringExpenseRepository;

	@InjectMocks
	private RecurringExpenseGenerationService generationService;

	private static RecurringExpense sampleRule(LocalDate startDate, LocalDate endDate, Integer dueDay) {
		return RecurringExpense.builder()
				.id(1L)
				.user(new User())
				.category(new Category())
				.description("Internet")
				.amount(new BigDecimal("119.90"))
				.paymentMethod(PaymentMethod.CREDIT_CARD)
				.notes("Plano residencial")
				.frequency(RecurrenceFrequency.MONTHLY)
				.dueDay(dueDay)
				.startDate(startDate)
				.endDate(endDate)
				.status(RecurrenceStatus.ACTIVE)
				.build();
	}

	@Test
	void generateInitialOccurrenceIfDue_generatesAndAdvances_whenStartDateIsTodayOrEarlier() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 10);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(expenseRepository).save(any(Expense.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void generateInitialOccurrenceIfDue_doesNotGenerate_whenStartDateIsInTheFuture() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 12, 1), null, 10);
		rule.setNextGenerationDate(LocalDate.of(2026, 12, 10));

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(expenseRepository, never()).save(any(Expense.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 12, 10));
	}

	@Test
	void generateInitialOccurrenceIfDue_doesNotGenerate_whenEndDateBeforeCandidateOccurrence() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), 10);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(expenseRepository, never()).save(any(Expense.class));
	}

	@Test
	void generateForRule_advancesNextGenerationDate_whenOccurrenceAlreadyGenerated() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 10);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 10));
		when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(true);

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 10));

		verify(expenseRepository, never()).save(any(Expense.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void generateForRule_swallowsDataIntegrityViolation_andStillAdvancesNextGenerationDate() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 10);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 10));
		when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(false);
		when(expenseRepository.save(any(Expense.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 10));

		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void generateForRule_generatesExpense_withCorrectMetadataAndClampedDueDay() {
		RecurringExpense rule = sampleRule(LocalDate.of(2027, 1, 1), null, 31);
		rule.setNextGenerationDate(LocalDate.of(2027, 2, 28));
		when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2027, 2))
				.thenReturn(false);

		generationService.generateForRule(1L, LocalDate.of(2027, 2, 28));

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		Expense saved = captor.getValue();
		assertThat(saved.getUser()).isSameAs(rule.getUser());
		assertThat(saved.getCategory()).isSameAs(rule.getCategory());
		assertThat(saved.getDescription()).isEqualTo("Internet");
		assertThat(saved.getAmount()).isEqualByComparingTo("119.90");
		assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.of(2027, 2, 28));
		assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(saved.getNotes()).isEqualTo("Plano residencial");
		assertThat(saved.getRecurringExpense()).isSameAs(rule);
		assertThat(saved.isGeneratedAutomatically()).isTrue();
		assertThat(saved.getRecurrenceReferenceYear()).isEqualTo(2027);
		assertThat(saved.getRecurrenceReferenceMonth()).isEqualTo(2);
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2027, 3, 31));
	}

	@Test
	void generateInitialOccurrenceIfDue_usesFirstDayOfMonth_whenDueDayIsNull() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, null);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		assertThat(captor.getValue().getExpenseDate()).isEqualTo(LocalDate.of(2026, 8, 1));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

}
