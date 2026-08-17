package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.util.RecurrenceDateCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseGenerationServiceTest {

	private static final TransactionMethod PIX = TransactionMethod.builder().id(2L).name("Pix").type(TransactionMethodType.PIX).build();
	private static final TransactionMethod NUBANK = TransactionMethod.builder().id(5L).name("Nubank").type(TransactionMethodType.CARD).build();

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private RecurringExpenseRepository recurringExpenseRepository;

	@InjectMocks
	private RecurringExpenseGenerationService generationService;

	/**
	 * 0 makes generateUpTo's horizon collapse to exactly "today," reproducing the pre-lookahead,
	 * single-occurrence behavior most of these tests were written against. Tests that specifically
	 * exercise the lookahead window override this per-test.
	 */
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(generationService, "lookAheadMonths", 0);
	}

	private static RecurringExpense sampleRule(LocalDate startDate, LocalDate endDate, Integer dueDay) {
		RecurringExpense rule = RecurringExpense.builder()
				.id(1L)
				.user(new User())
				.category(new Category())
				.description("Internet")
				.amount(new BigDecimal("119.90"))
				.transactionMethod(PIX)
				.notes("Plano residencial")
				.frequency(RecurrenceFrequency.MONTHLY)
				.dueDay(dueDay)
				.startDate(startDate)
				.endDate(endDate)
				.status(RecurrenceStatus.ACTIVE)
				.build();
		// RecurringExpenseService.create() always sets this right before calling
		// generateInitialOccurrenceIfDue, so the generation service itself never reads startDate.
		rule.setNextGenerationDate(RecurrenceDateCalculator.resolveOccurrenceDate(startDate, dueDay));
		return rule;
	}

	@Test
	void generateInitialOccurrenceIfDue_generatesAndAdvances_whenStartDateIsTodayOrEarlier() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 10);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 10));

		verify(expenseRepository).save(any(Expense.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void generateInitialOccurrenceIfDue_doesNotGenerate_whenFirstOccurrenceIsBeyondLookaheadHorizon() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 12, 1), null, 10);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(expenseRepository, never()).save(any(Expense.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 12, 10));
	}

	@Test
	void generateInitialOccurrenceIfDue_generatesEveryOccurrence_upToLookaheadHorizon() {
		ReflectionTestUtils.setField(generationService, "lookAheadMonths", 3);
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 10);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		// horizon = last day of (August + 3 months) = 2026-11-30, so Aug/Sep/Oct/Nov 10th all
		// qualify (this month plus 3 ahead) but Dec 10th doesn't yet.
		verify(expenseRepository, times(4)).save(any(Expense.class));
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
		assertThat(saved.getTransactionMethod()).isEqualTo(PIX);
		assertThat(saved.getCardTransactionMode()).isNull();
		assertThat(saved.getNotes()).isEqualTo("Plano residencial");
		assertThat(saved.getRecurringExpense()).isSameAs(rule);
		assertThat(saved.isGeneratedAutomatically()).isTrue();
		assertThat(saved.getRecurrenceReferenceYear()).isEqualTo(2027);
		assertThat(saved.getRecurrenceReferenceMonth()).isEqualTo(2);
		assertThat(saved.getBillingMonth()).isEqualTo(2);
		assertThat(saved.getBillingYear()).isEqualTo(2027);
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

	@Test
	void generateForRule_cardCreditRule_carriesModeAndComputesBillingMonthAsMonthAfter() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 15);
		rule.setTransactionMethod(NUBANK);
		rule.setCardTransactionMode(CardTransactionMode.CREDIT);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 15));
		when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(false);

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 15));

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		Expense saved = captor.getValue();
		assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.of(2026, 8, 15));
		assertThat(saved.getTransactionMethod()).isSameAs(NUBANK);
		assertThat(saved.getCardTransactionMode()).isEqualTo(CardTransactionMode.CREDIT);
		assertThat(saved.getBillingMonth()).isEqualTo(9);
		assertThat(saved.getBillingYear()).isEqualTo(2026);
	}

	@Test
	void generateForRule_cardDebitRule_computesBillingMonthAsOccurrenceDatesOwnMonth() {
		RecurringExpense rule = sampleRule(LocalDate.of(2026, 8, 1), null, 15);
		rule.setTransactionMethod(NUBANK);
		rule.setCardTransactionMode(CardTransactionMode.DEBIT);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 15));
		when(recurringExpenseRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(false);

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 15));

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		Expense saved = captor.getValue();
		assertThat(saved.getCardTransactionMode()).isEqualTo(CardTransactionMode.DEBIT);
		assertThat(saved.getBillingMonth()).isEqualTo(8);
		assertThat(saved.getBillingYear()).isEqualTo(2026);
	}

}
