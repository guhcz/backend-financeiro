package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.enums.PaymentMethod;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.ExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
	private UserRepository userRepository;
	@Mock
	private ExpenseMapper expenseMapper;

	@InjectMocks
	private ExpenseService expenseService;

	private static ExpenseRequest sampleRequest(Long categoryId) {
		return new ExpenseRequest(categoryId, "Lunch", new BigDecimal("25.50"),
				LocalDate.of(2026, 7, 10), PaymentMethod.PIX, null);
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

		assertThatThrownBy(() -> expenseService.update(1L, 3L, sampleRequest(9L)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void delete_deletesExpense_whenOwnedByUser() {
		Expense expense = new Expense();
		when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));

		expenseService.delete(1L, 3L);

		verify(expenseRepository).delete(expense);
	}

}
