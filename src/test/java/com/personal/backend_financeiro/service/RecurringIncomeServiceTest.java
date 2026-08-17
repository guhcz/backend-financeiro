package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeCreateRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeUpdateRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.enums.ReceiptMethod;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.RecurringIncomeMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringIncomeServiceTest {

	@Mock
	private RecurringIncomeRepository recurringIncomeRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private IncomeRepository incomeRepository;
	@Mock
	private RecurringIncomeMapper recurringIncomeMapper;
	@Mock
	private RecurringIncomeGenerationService generationService;

	@InjectMocks
	private RecurringIncomeService recurringIncomeService;

	private static RecurringIncomeCreateRequest sampleCreateRequest(Long categoryId, RecurrenceFrequency frequency,
			LocalDate startDate, LocalDate endDate, Integer receiptDay) {
		return new RecurringIncomeCreateRequest(categoryId, "Salário", new BigDecimal("6200.00"),
				ReceiptMethod.BANK_TRANSFER, "Salário mensal", frequency, receiptDay, startDate, endDate);
	}

	private static RecurringIncomeUpdateRequest sampleUpdateRequest(Long categoryId, Integer receiptDay, LocalDate endDate) {
		return new RecurringIncomeUpdateRequest(categoryId, "Salário", new BigDecimal("6200.00"),
				ReceiptMethod.BANK_TRANSFER, "Salário mensal", receiptDay, endDate);
	}

	private static RecurringIncome ruleWithStatus(RecurrenceStatus status) {
		return RecurringIncome.builder()
				.id(1L)
				.description("Salário")
				.amount(new BigDecimal("6200.00"))
				.receiptMethod(ReceiptMethod.BANK_TRANSFER)
				.frequency(RecurrenceFrequency.MONTHLY)
				.receiptDay(5)
				.startDate(LocalDate.of(2026, 8, 1))
				.nextGenerationDate(LocalDate.of(2026, 9, 5))
				.status(status)
				.build();
	}

	@Test
	void create_throwsResourceNotFoundException_whenCategoryNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringIncomeService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 8, 1), null, 5)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(recurringIncomeRepository, never()).save(any(RecurringIncome.class));
	}

	@Test
	void create_throwsInvalidRequestException_whenFrequencyIsNotMonthly() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));

		assertThatThrownBy(() -> recurringIncomeService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 8, 1), null, 5)))
				.isInstanceOf(InvalidRequestException.class);

		verify(recurringIncomeRepository, never()).save(any(RecurringIncome.class));
	}

	@Test
	void create_throwsInvalidRequestException_whenEndDateBeforeStartDate() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));

		assertThatThrownBy(() -> recurringIncomeService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY,
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1), 5)))
				.isInstanceOf(InvalidRequestException.class);

		verify(recurringIncomeRepository, never()).save(any(RecurringIncome.class));
	}

	@Test
	void create_allowsNullReceiptDay_andDefaultsGenerationToFirstDayOfMonth() {
		RecurringIncomeCreateRequest request = sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY,
				LocalDate.of(2026, 8, 1), null, null);
		RecurringIncome entity = new RecurringIncome();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(recurringIncomeMapper.toEntity(request)).thenReturn(entity);
		when(userRepository.getReferenceById(1L)).thenReturn(new User());
		when(recurringIncomeRepository.save(entity)).thenReturn(entity);

		recurringIncomeService.create(1L, request);

		assertThat(entity.getReceiptDay()).isNull();
		assertThat(entity.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 8, 1));
	}

	@Test
	void update_recalculatesNextGenerationDate_whenReceiptDayChangesOnActiveRule() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));

		recurringIncomeService.update(1L, 1L, sampleUpdateRequest(9L, 20, null));

		verify(recurringIncomeMapper).updateEntityFromRequest(any(), any());
	}

	@Test
	void getOne_throwsResourceNotFoundException_whenRuleNotOwnedByUser() {
		when(recurringIncomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringIncomeService.getOne(1L, 3L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenRuleNotOwnedByUser() {
		when(recurringIncomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringIncomeService.update(1L, 3L, sampleUpdateRequest(9L, 5, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsInvalidRequestException_whenRuleIsEnded() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringIncomeService.update(1L, 1L, sampleUpdateRequest(9L, 5, null)))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void update_doesNotThrow_whenRuleIsPausedAndReceiptDayChanges() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));

		recurringIncomeService.update(1L, 1L, sampleUpdateRequest(9L, 20, null));

		verify(recurringIncomeMapper).updateEntityFromRequest(any(), any());
	}

	@Test
	void pause_setsStatusToPaused_whenActive() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringIncomeService.pause(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.PAUSED);
	}

	@Test
	void pause_throwsInvalidRequestException_whenEnded() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringIncomeService.pause(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void resume_reactivatesAndRecalculatesNextGenerationDate_whenPaused() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		rule.setNextGenerationDate(LocalDate.of(2026, 1, 5));
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringIncomeService.resume(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ACTIVE);
		assertThat(rule.getNextGenerationDate()).isAfterOrEqualTo(LocalDate.now());
	}

	@Test
	void resume_throwsInvalidRequestException_whenEnded() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringIncomeService.resume(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void resume_throwsInvalidRequestException_whenEndDateAlreadyPassed() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		rule.setEndDate(LocalDate.now().minusDays(1));
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringIncomeService.resume(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void delete_hardDeletes_whenNoIncomesGenerated() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsIncludingInactiveByRecurringIncomeId(1L)).thenReturn(false);

		recurringIncomeService.delete(1L, 1L);

		verify(recurringIncomeRepository).delete(rule);
	}

	@Test
	void delete_setsStatusToEnded_whenIncomesAlreadyGenerated() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsIncludingInactiveByRecurringIncomeId(1L)).thenReturn(true);

		recurringIncomeService.delete(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ENDED);
		verify(recurringIncomeRepository, never()).delete(any(RecurringIncome.class));
	}

	@Test
	void update_appliesNewValuesToAlreadyGeneratedFutureIncomes() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		Category newCategory = new Category();
		com.personal.backend_financeiro.entity.Income future = com.personal.backend_financeiro.entity.Income.builder()
				.description("Salário").amount(new BigDecimal("6200.00")).receiptMethod(ReceiptMethod.BANK_TRANSFER).build();
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(newCategory));
		when(incomeRepository.findByRecurringIncomeIdAndIncomeDateGreaterThanEqual(any(), any()))
				.thenReturn(java.util.List.of(future));

		recurringIncomeService.update(1L, 1L, sampleUpdateRequest(9L, 5, null));

		assertThat(future.getCategory()).isSameAs(newCategory);
		assertThat(future.getAmount()).isEqualByComparingTo("6200.00");
	}

	@Test
	void delete_removesAlreadyGeneratedFutureIncomes_whenEndingRule() {
		RecurringIncome rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringIncomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(incomeRepository.existsIncludingInactiveByRecurringIncomeId(1L)).thenReturn(true);
		java.util.List<com.personal.backend_financeiro.entity.Income> future = java.util.List.of(
				com.personal.backend_financeiro.entity.Income.builder().build());
		when(incomeRepository.findByRecurringIncomeIdAndIncomeDateGreaterThanEqual(any(), any())).thenReturn(future);

		recurringIncomeService.delete(1L, 1L);

		verify(incomeRepository).deleteAll(future);
	}

}
