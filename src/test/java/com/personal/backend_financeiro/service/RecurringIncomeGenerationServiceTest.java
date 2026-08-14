package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.enums.ReceiptMethod;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
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
class RecurringIncomeGenerationServiceTest {

	@Mock
	private IncomeRepository incomeRepository;
	@Mock
	private RecurringIncomeRepository recurringIncomeRepository;

	@InjectMocks
	private RecurringIncomeGenerationService generationService;

	/**
	 * 0 makes generateUpTo's horizon collapse to exactly "today," reproducing the pre-lookahead,
	 * single-occurrence behavior most of these tests were written against. Tests that specifically
	 * exercise the lookahead window override this per-test.
	 */
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(generationService, "lookAheadMonths", 0);
	}

	private static RecurringIncome sampleRule(LocalDate startDate, LocalDate endDate, Integer receiptDay) {
		RecurringIncome rule = RecurringIncome.builder()
				.id(1L)
				.user(new User())
				.category(new Category())
				.description("Salário")
				.amount(new BigDecimal("6200.00"))
				.receiptMethod(ReceiptMethod.BANK_TRANSFER)
				.notes("Salário mensal")
				.frequency(RecurrenceFrequency.MONTHLY)
				.receiptDay(receiptDay)
				.startDate(startDate)
				.endDate(endDate)
				.status(RecurrenceStatus.ACTIVE)
				.build();
		// RecurringIncomeService.create() always sets this right before calling
		// generateInitialOccurrenceIfDue, so the generation service itself never reads startDate.
		rule.setNextGenerationDate(RecurrenceDateCalculator.resolveOccurrenceDate(startDate, receiptDay));
		return rule;
	}

	@Test
	void generateInitialOccurrenceIfDue_generatesAndAdvances_whenStartDateIsTodayOrEarlier() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), null, 5);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(incomeRepository).save(any(Income.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 5));
	}

	@Test
	void generateInitialOccurrenceIfDue_doesNotGenerate_whenFirstOccurrenceIsBeyondLookaheadHorizon() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 12, 1), null, 5);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(incomeRepository, never()).save(any(Income.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 12, 5));
	}

	@Test
	void generateInitialOccurrenceIfDue_generatesEveryOccurrence_upToLookaheadHorizon() {
		ReflectionTestUtils.setField(generationService, "lookAheadMonths", 3);
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), null, 5);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 1));

		// horizon = last day of (August + 3 months) = 2026-11-30, so Aug/Sep/Oct/Nov 5th all
		// qualify (this month plus 3 ahead) but Dec 5th doesn't yet.
		verify(incomeRepository, times(4)).save(any(Income.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 12, 5));
	}

	@Test
	void generateInitialOccurrenceIfDue_doesNotGenerate_whenEndDateBeforeCandidateOccurrence() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), 5);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		verify(incomeRepository, never()).save(any(Income.class));
	}

	@Test
	void generateForRule_advancesNextGenerationDate_whenOccurrenceAlreadyGenerated() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), null, 5);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 5));
		when(recurringIncomeRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(true);

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 5));

		verify(incomeRepository, never()).save(any(Income.class));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 5));
	}

	@Test
	void generateForRule_swallowsDataIntegrityViolation_andStillAdvancesNextGenerationDate() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), null, 5);
		rule.setNextGenerationDate(LocalDate.of(2026, 8, 5));
		when(recurringIncomeRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2026, 8))
				.thenReturn(false);
		when(incomeRepository.save(any(Income.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		generationService.generateForRule(1L, LocalDate.of(2026, 8, 5));

		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 5));
	}

	@Test
	void generateForRule_generatesIncome_withCorrectMetadataAndClampedReceiptDay() {
		RecurringIncome rule = sampleRule(LocalDate.of(2027, 1, 1), null, 31);
		rule.setNextGenerationDate(LocalDate.of(2027, 2, 28));
		when(recurringIncomeRepository.findById(1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(1L, 2027, 2))
				.thenReturn(false);

		generationService.generateForRule(1L, LocalDate.of(2027, 2, 28));

		ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
		verify(incomeRepository).save(captor.capture());
		Income saved = captor.getValue();
		assertThat(saved.getUser()).isSameAs(rule.getUser());
		assertThat(saved.getCategory()).isSameAs(rule.getCategory());
		assertThat(saved.getDescription()).isEqualTo("Salário");
		assertThat(saved.getAmount()).isEqualByComparingTo("6200.00");
		assertThat(saved.getIncomeDate()).isEqualTo(LocalDate.of(2027, 2, 28));
		assertThat(saved.getReceiptMethod()).isEqualTo(ReceiptMethod.BANK_TRANSFER);
		assertThat(saved.getNotes()).isEqualTo("Salário mensal");
		assertThat(saved.getRecurringIncome()).isSameAs(rule);
		assertThat(saved.isGeneratedAutomatically()).isTrue();
		assertThat(saved.getRecurrenceReferenceYear()).isEqualTo(2027);
		assertThat(saved.getRecurrenceReferenceMonth()).isEqualTo(2);
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2027, 3, 31));
	}

	@Test
	void generateInitialOccurrenceIfDue_usesFirstDayOfMonth_whenReceiptDayIsNull() {
		RecurringIncome rule = sampleRule(LocalDate.of(2026, 8, 1), null, null);

		generationService.generateInitialOccurrenceIfDue(rule, LocalDate.of(2026, 8, 5));

		ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
		verify(incomeRepository).save(captor.capture());
		assertThat(captor.getValue().getIncomeDate()).isEqualTo(LocalDate.of(2026, 8, 1));
		assertThat(rule.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

}
