package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.CreditCard;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.PaymentMethod;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.ExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CreditCardRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ExpenseServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private CreditCardRepository creditCardRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExpenseMapper expenseMapper;

	@InjectMocks
	private ExpenseService expenseService;

	private static ExpenseRequest sampleRequest(Long categoryId) {
		return new ExpenseRequest(categoryId, "Lunch", new BigDecimal("25.50"),
				LocalDate.of(2026, 7, 10), PaymentMethod.PIX, null, null);
	}

	@Test
	void create_throwsResourceNotFoundException_whenCategoryNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> expenseService.create(1L, sampleRequest(9L)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(expenseRepository, never()).save(any(Expense.class));
	}

	@Test
	void getOne_throwsResourceNotFoundException_whenExpenseNotOwnedByUser() {
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> expenseService.getOne(1L, 3L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenNewCategoryNotOwnedByUser() {
		Expense expense = new Expense();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> expenseService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.ONLY_THIS))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_onlyUpdatesExpense_whenScopeIsOnlyThis() {
		RecurringExpense rule = new RecurringExpense();
		rule.setDescription("Old description");
		Expense expense = new Expense();
		expense.setRecurringExpense(rule);
		Category category = new Category();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		expenseService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.ONLY_THIS);

		assertThat(rule.getDescription()).isEqualTo("Old description");
	}

	@Test
	void update_alsoUpdatesRecurringExpense_whenScopeIsThisAndFuture() {
		RecurringExpense rule = new RecurringExpense();
		rule.setDescription("Old description");
		Expense expense = new Expense();
		expense.setRecurringExpense(rule);
		Category category = new Category();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		expenseService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(rule.getDescription()).isEqualTo("Lunch");
		assertThat(rule.getCategory()).isEqualTo(category);
	}

	@Test
	void update_ignoresThisAndFutureScope_whenExpenseIsManual() {
		Expense expense = new Expense();
		Category category = new Category();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		expenseService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(expense.getRecurringExpense()).isNull();
	}

	@Test
	void delete_deletesExpense_whenOwnedByUser() {
		Expense expense = new Expense();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));

		expenseService.delete(1L, 3L, RecurringUpdateScope.ONLY_THIS);

		verify(expenseRepository).delete(expense);
	}

	@Test
	void delete_endsRecurringExpense_whenScopeIsThisAndFuture() {
		RecurringExpense rule = new RecurringExpense();
		rule.setStatus(RecurrenceStatus.ACTIVE);
		Expense expense = new Expense();
		expense.setRecurringExpense(rule);
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));

		expenseService.delete(1L, 3L, RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ENDED);
		verify(expenseRepository).delete(expense);
	}

	@Test
	void delete_ignoresThisAndFutureScope_whenExpenseIsManual() {
		Expense expense = new Expense();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));

		expenseService.delete(1L, 3L, RecurringUpdateScope.THIS_AND_FUTURE);

		verify(expenseRepository).delete(expense);
	}

	@Test
	void create_throwsInvalidRequestException_whenCreditCardMethodWithoutCreditCardId() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));
		ExpenseRequest request = new ExpenseRequest(9L, "Compra", new BigDecimal("100.00"),
				LocalDate.of(2026, 8, 15), PaymentMethod.CREDIT_CARD, null, null);

		assertThatThrownBy(() -> expenseService.create(1L, request))
				.isInstanceOf(InvalidRequestException.class);

		verify(expenseRepository, never()).save(any(Expense.class));
	}

	@Test
	void create_throwsResourceNotFoundException_whenCreditCardNotOwnedByUser() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));
		when(creditCardRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
		ExpenseRequest request = new ExpenseRequest(9L, "Compra", new BigDecimal("100.00"),
				LocalDate.of(2026, 8, 15), PaymentMethod.CREDIT_CARD, null, 5L);

		assertThatThrownBy(() -> expenseService.create(1L, request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_computesBillingPeriodAsMonthAfterExpenseDate_forNonCreditCardPayment() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));
		when(expenseMapper.toEntity(any(ExpenseRequest.class))).thenReturn(new Expense());
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

		expenseService.create(1L, sampleRequest(9L));

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		assertThat(captor.getValue().getBillingMonth()).isEqualTo(8);
		assertThat(captor.getValue().getBillingYear()).isEqualTo(2026);
		assertThat(captor.getValue().getCreditCard()).isNull();
	}

	@Test
	void create_computesBillingPeriodAsMonthAfterExpenseDate_forCreditCardPayment() {
		Category category = new Category();
		CreditCard nubank = CreditCard.builder().id(5L).closingDay(20).dueDay(10).build();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));
		when(creditCardRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(nubank));
		when(expenseMapper.toEntity(any(ExpenseRequest.class))).thenReturn(new Expense());
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ExpenseRequest request = new ExpenseRequest(9L, "Compra", new BigDecimal("500.00"),
				LocalDate.of(2026, 8, 15), PaymentMethod.CREDIT_CARD, null, 5L);

		expenseService.create(1L, request);

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		// Same rule as any other payment method now: month after expenseDate, regardless of the
		// card's closing/due day.
		assertThat(captor.getValue().getBillingMonth()).isEqualTo(9);
		assertThat(captor.getValue().getBillingYear()).isEqualTo(2026);
		assertThat(captor.getValue().getCreditCard()).isEqualTo(nubank);
	}

	@Test
	void create_billingPeriodRollsOverToNextYear_whenExpenseDateIsDecember() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));
		when(expenseMapper.toEntity(any(ExpenseRequest.class))).thenReturn(new Expense());
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ExpenseRequest request = new ExpenseRequest(9L, "Ceia", new BigDecimal("200.00"),
				LocalDate.of(2026, 12, 20), PaymentMethod.PIX, null, null);

		expenseService.create(1L, request);

		ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(captor.capture());
		assertThat(captor.getValue().getBillingMonth()).isEqualTo(1);
		assertThat(captor.getValue().getBillingYear()).isEqualTo(2027);
	}

}
