package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.income.IncomeRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.ReceiptMethod;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.IncomeMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
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
class IncomeServiceTest {

	@Mock
	private IncomeRepository incomeRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private IncomeMapper incomeMapper;

	@InjectMocks
	private IncomeService incomeService;

	private static IncomeRequest sampleRequest(Long categoryId) {
		return new IncomeRequest(categoryId, "Salário", new BigDecimal("6200.00"),
				LocalDate.of(2026, 8, 5), ReceiptMethod.BANK_TRANSFER, null);
	}

	@Test
	void create_throwsResourceNotFoundException_whenCategoryNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> incomeService.create(1L, sampleRequest(9L)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(incomeRepository, never()).save(any(Income.class));
	}

	@Test
	void getOne_throwsResourceNotFoundException_whenIncomeNotOwnedByUser() {
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> incomeService.getOne(1L, 3L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenNewCategoryNotOwnedByUser() {
		Income income = new Income();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> incomeService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.ONLY_THIS))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_onlyUpdatesIncome_whenScopeIsOnlyThis() {
		RecurringIncome rule = new RecurringIncome();
		rule.setDescription("Old description");
		Income income = new Income();
		income.setRecurringIncome(rule);
		Category category = new Category();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		incomeService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.ONLY_THIS);

		assertThat(rule.getDescription()).isEqualTo("Old description");
	}

	@Test
	void update_alsoUpdatesRecurringIncome_whenScopeIsThisAndFuture() {
		RecurringIncome rule = new RecurringIncome();
		rule.setDescription("Old description");
		Income income = new Income();
		income.setRecurringIncome(rule);
		Category category = new Category();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		incomeService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(rule.getDescription()).isEqualTo("Salário");
		assertThat(rule.getCategory()).isEqualTo(category);
	}

	@Test
	void update_ignoresThisAndFutureScope_whenIncomeIsManual() {
		Income income = new Income();
		Category category = new Category();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));
		when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(category));

		incomeService.update(1L, 3L, sampleRequest(9L), RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(income.getRecurringIncome()).isNull();
	}

	@Test
	void delete_deletesIncome_whenOwnedByUser() {
		Income income = new Income();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));

		incomeService.delete(1L, 3L, RecurringUpdateScope.ONLY_THIS);

		verify(incomeRepository).delete(income);
	}

	@Test
	void delete_endsRecurringIncome_whenScopeIsThisAndFuture() {
		RecurringIncome rule = new RecurringIncome();
		rule.setStatus(RecurrenceStatus.ACTIVE);
		Income income = new Income();
		income.setRecurringIncome(rule);
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));

		incomeService.delete(1L, 3L, RecurringUpdateScope.THIS_AND_FUTURE);

		assertThat(rule.getStatus()).isEqualTo(RecurrenceStatus.ENDED);
		verify(incomeRepository).delete(income);
	}

	@Test
	void delete_ignoresThisAndFutureScope_whenIncomeIsManual() {
		Income income = new Income();
		when(incomeRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(income));

		incomeService.delete(1L, 3L, RecurringUpdateScope.THIS_AND_FUTURE);

		verify(incomeRepository).delete(income);
	}

}
