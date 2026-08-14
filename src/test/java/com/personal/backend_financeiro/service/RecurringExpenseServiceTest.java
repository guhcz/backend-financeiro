package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseCreateRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseUpdateRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.RecurringExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.TransactionMethodRepository;
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
class RecurringExpenseServiceTest {

	private static final TransactionMethod PIX = TransactionMethod.builder().id(2L).name("Pix").type(TransactionMethodType.PIX).build();
	private static final TransactionMethod NUBANK = TransactionMethod.builder().id(5L).name("Nubank").type(TransactionMethodType.CARD).build();

	@Mock
	private RecurringExpenseRepository recurringExpenseRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TransactionMethodRepository transactionMethodRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private RecurringExpenseMapper recurringExpenseMapper;
	@Mock
	private RecurringExpenseGenerationService generationService;

	@InjectMocks
	private RecurringExpenseService recurringExpenseService;

	private static RecurringExpenseCreateRequest sampleCreateRequest(Long categoryId, RecurrenceFrequency frequency,
			LocalDate startDate, LocalDate endDate, Integer dueDay) {
		return new RecurringExpenseCreateRequest(categoryId, "Internet", new BigDecimal("119.90"),
				2L, null, "Plano residencial", frequency, dueDay, startDate, endDate);
	}

	private static RecurringExpenseUpdateRequest sampleUpdateRequest(Long categoryId, Integer dueDay, LocalDate endDate) {
		return new RecurringExpenseUpdateRequest(categoryId, "Internet", new BigDecimal("119.90"),
				2L, null, "Plano residencial", dueDay, endDate);
	}

	private static RecurringExpense ruleWithStatus(RecurrenceStatus status) {
		return RecurringExpense.builder()
				.id(1L)
				.description("Internet")
				.amount(new BigDecimal("119.90"))
				.transactionMethod(NUBANK)
				.cardTransactionMode(CardTransactionMode.CREDIT)
				.frequency(RecurrenceFrequency.MONTHLY)
				.dueDay(10)
				.startDate(LocalDate.of(2026, 8, 1))
				.nextGenerationDate(LocalDate.of(2026, 9, 10))
				.status(status)
				.build();
	}

	@Test
	void create_throwsResourceNotFoundException_whenCategoryNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringExpenseService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 8, 1), null, 10)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
	}

	@Test
	void create_throwsInvalidRequestException_whenFrequencyIsNotMonthly() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));

		assertThatThrownBy(() -> recurringExpenseService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 8, 1), null, 10)))
				.isInstanceOf(InvalidRequestException.class);

		verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
	}

	@Test
	void create_throwsInvalidRequestException_whenEndDateBeforeStartDate() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));

		assertThatThrownBy(() -> recurringExpenseService.create(1L, sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY,
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1), 10)))
				.isInstanceOf(InvalidRequestException.class);

		verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
	}

	@Test
	void create_allowsNullDueDay_andDefaultsGenerationToFirstDayOfMonth() {
		RecurringExpenseCreateRequest request = sampleCreateRequest(9L, RecurrenceFrequency.MONTHLY,
				LocalDate.of(2026, 8, 1), null, null);
		RecurringExpense entity = new RecurringExpense();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));
		when(recurringExpenseMapper.toEntity(request)).thenReturn(entity);
		when(userRepository.getReferenceById(1L)).thenReturn(new User());
		when(recurringExpenseRepository.save(entity)).thenReturn(entity);

		recurringExpenseService.create(1L, request);

		assertThat(entity.getDueDay()).isNull();
		assertThat(entity.getNextGenerationDate()).isEqualTo(LocalDate.of(2026, 8, 1));
	}

	@Test
	void update_allowsClearingDueDay_recalculatingNextGenerationDateAsFirstDayOfMonth() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));

		recurringExpenseService.update(1L, 1L, sampleUpdateRequest(9L, null, null));

		verify(recurringExpenseMapper).updateEntityFromRequest(any(), any());
	}

	@Test
	void getOne_throwsResourceNotFoundException_whenRuleNotOwnedByUser() {
		when(recurringExpenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringExpenseService.getOne(1L, 3L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenRuleNotOwnedByUser() {
		when(recurringExpenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recurringExpenseService.update(1L, 3L, sampleUpdateRequest(9L, 10, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsInvalidRequestException_whenRuleIsEnded() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringExpenseService.update(1L, 1L, sampleUpdateRequest(9L, 10, null)))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void update_recalculatesNextGenerationDate_whenDueDayChangesOnActiveRule() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));

		recurringExpenseService.update(1L, 1L, sampleUpdateRequest(9L, 20, null));

		verify(recurringExpenseMapper).updateEntityFromRequest(any(), any());
	}

	@Test
	void update_doesNotThrow_whenRuleIsPausedAndDueDayChanges() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));

		recurringExpenseService.update(1L, 1L, sampleUpdateRequest(9L, 20, null));

		verify(recurringExpenseMapper).updateEntityFromRequest(any(), any());
	}

	@Test
	void pause_setsStatusToPaused_whenActive() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringExpenseService.pause(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.PAUSED);
	}

	@Test
	void pause_isNoOp_whenAlreadyPaused() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringExpenseService.pause(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.PAUSED);
	}

	@Test
	void pause_throwsInvalidRequestException_whenEnded() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringExpenseService.pause(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void resume_reactivatesAndRecalculatesNextGenerationDate_whenPaused() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		rule.setNextGenerationDate(LocalDate.of(2026, 1, 10));
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringExpenseService.resume(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ACTIVE);
		assertThat(rule.getNextGenerationDate()).isAfterOrEqualTo(LocalDate.now());
	}

	@Test
	void resume_isNoOp_whenAlreadyActive() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		LocalDate originalNextGenerationDate = rule.getNextGenerationDate();
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		recurringExpenseService.resume(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ACTIVE);
		assertThat(rule.getNextGenerationDate()).isEqualTo(originalNextGenerationDate);
	}

	@Test
	void resume_throwsInvalidRequestException_whenEnded() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ENDED);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringExpenseService.resume(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void resume_throwsInvalidRequestException_whenEndDateAlreadyPassed() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.PAUSED);
		rule.setEndDate(LocalDate.now().minusDays(1));
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));

		assertThatThrownBy(() -> recurringExpenseService.resume(1L, 1L))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void delete_hardDeletes_whenNoExpensesGenerated() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsIncludingInactiveByRecurringExpenseId(1L)).thenReturn(false);

		recurringExpenseService.delete(1L, 1L);

		verify(recurringExpenseRepository).delete(rule);
	}

	@Test
	void delete_setsStatusToEnded_whenExpensesAlreadyGenerated() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsIncludingInactiveByRecurringExpenseId(1L)).thenReturn(true);

		recurringExpenseService.delete(1L, 1L);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ENDED);
		verify(recurringExpenseRepository, never()).delete(any(RecurringExpense.class));
	}

	@Test
	void update_appliesNewValuesToAlreadyGeneratedFutureExpenses() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		Category newCategory = new Category();
		com.personal.backend_financeiro.entity.Expense future = com.personal.backend_financeiro.entity.Expense.builder()
				.description("Internet").amount(new BigDecimal("119.90"))
				.expenseDate(LocalDate.of(2026, 9, 10)).transactionMethod(PIX).build();
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(newCategory));
		when(transactionMethodRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(PIX));
		when(expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(any(), any()))
				.thenReturn(java.util.List.of(future));

		recurringExpenseService.update(1L, 1L, sampleUpdateRequest(9L, 10, null));

		assertThat(future.getCategory()).isSameAs(newCategory);
		assertThat(future.getAmount()).isEqualByComparingTo("119.90");
		assertThat(future.getTransactionMethod()).isEqualTo(PIX);
		assertThat(future.getBillingMonth()).isEqualTo(9);
		assertThat(future.getBillingYear()).isEqualTo(2026);
	}

	@Test
	void delete_removesAlreadyGeneratedFutureExpenses_whenEndingRule() {
		RecurringExpense rule = ruleWithStatus(RecurrenceStatus.ACTIVE);
		when(recurringExpenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rule));
		when(expenseRepository.existsIncludingInactiveByRecurringExpenseId(1L)).thenReturn(true);
		java.util.List<com.personal.backend_financeiro.entity.Expense> future = java.util.List.of(
				com.personal.backend_financeiro.entity.Expense.builder().build());
		when(expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(any(), any())).thenReturn(future);

		recurringExpenseService.delete(1L, 1L);

		verify(expenseRepository).deleteAll(future);
	}

	@Test
	void create_throwsInvalidRequestException_whenCardMethodWithoutCardTransactionMode() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(NUBANK));
		RecurringExpenseCreateRequest request = new RecurringExpenseCreateRequest(9L, "Internet", new BigDecimal("119.90"),
				5L, null, null, RecurrenceFrequency.MONTHLY, 10, LocalDate.of(2026, 8, 1), null);

		assertThatThrownBy(() -> recurringExpenseService.create(1L, request))
				.isInstanceOf(InvalidRequestException.class);

		verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
	}

	@Test
	void create_throwsResourceNotFoundException_whenTransactionMethodNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(new Category()));
		when(transactionMethodRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
		RecurringExpenseCreateRequest request = new RecurringExpenseCreateRequest(9L, "Internet", new BigDecimal("119.90"),
				5L, CardTransactionMode.CREDIT, null, RecurrenceFrequency.MONTHLY, 10, LocalDate.of(2026, 8, 1), null);

		assertThatThrownBy(() -> recurringExpenseService.create(1L, request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
