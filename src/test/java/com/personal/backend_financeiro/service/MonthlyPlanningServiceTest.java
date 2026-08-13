package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.MonthlyPlanning;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.mapper.MonthlyPlanningMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CategoryTotalProjection;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyPlanningRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyPlanningServiceTest {

	@Mock
	private MonthlyPlanningRepository monthlyPlanningRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private MonthlyPlanningMapper monthlyPlanningMapper;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private MonthlyPlanningService monthlyPlanningService;

	private static MonthlyPlanningRequest sampleRequest() {
		return new MonthlyPlanningRequest(2L, 8, 2026, new BigDecimal("1000.00"));
	}

	@Test
	void create_throwsResourceNotFoundException_whenCategoryNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> monthlyPlanningService.create(1L, sampleRequest()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_throwsDuplicateResourceException_whenCategoryAlreadyPlannedForPeriod() {
		Category category = new Category();
		category.setId(2L);
		when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(category));

		MonthlyPlanning existing = new MonthlyPlanning();
		existing.setId(9L);
		when(monthlyPlanningRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 2L, 8, 2026))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> monthlyPlanningService.create(1L, sampleRequest()))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenNotOwnedByUser() {
		when(monthlyPlanningRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> monthlyPlanningService.update(1L, 9L, sampleRequest()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsDuplicateResourceException_whenMovingToPeriodOwnedByAnotherPlanning() {
		MonthlyPlanning current = new MonthlyPlanning();
		current.setId(9L);
		when(monthlyPlanningRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(current));

		Category category = new Category();
		category.setId(2L);
		when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(category));

		MonthlyPlanning other = new MonthlyPlanning();
		other.setId(42L);
		when(monthlyPlanningRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 2L, 8, 2026))
				.thenReturn(Optional.of(other));

		assertThatThrownBy(() -> monthlyPlanningService.update(1L, 9L, sampleRequest()))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void list_computesSpentRemainingAndPercentage_fromAggregatedExpenses() {
		Category category = new Category();
		category.setId(2L);
		MonthlyPlanning planning = new MonthlyPlanning();
		planning.setId(9L);
		planning.setCategory(category);
		planning.setAmount(new BigDecimal("1000.00"));

		PageRequest pageable = PageRequest.of(0, 10);
		Page<MonthlyPlanning> page = new PageImpl<>(List.of(planning), pageable, 1);
		when(monthlyPlanningRepository.findByUserIdAndMonthAndYear(1L, 8, 2026, pageable)).thenReturn(page);
		when(expenseRepository.sumAmountGroupedByCategory(1L, 2026, 8))
				.thenReturn(List.of(projection(2L, new BigDecimal("620.00"))));
		when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(2L, "Food", "#FF0000", null, true));

		Page<MonthlyPlanningItemResponse> result = monthlyPlanningService.list(1L, 8, 2026, pageable);

		MonthlyPlanningItemResponse item = result.getContent().get(0);
		assertThat(item.spentAmount()).isEqualByComparingTo("620.00");
		assertThat(item.remainingAmount()).isEqualByComparingTo("380.00");
		assertThat(item.percentageUsed()).isEqualByComparingTo("62.00");
	}

	@Test
	void list_defaultsSpentToZero_whenCategoryHasNoExpensesInPeriod() {
		Category category = new Category();
		category.setId(2L);
		MonthlyPlanning planning = new MonthlyPlanning();
		planning.setId(9L);
		planning.setCategory(category);
		planning.setAmount(new BigDecimal("1000.00"));

		PageRequest pageable = PageRequest.of(0, 10);
		Page<MonthlyPlanning> page = new PageImpl<>(List.of(planning), pageable, 1);
		when(monthlyPlanningRepository.findByUserIdAndMonthAndYear(1L, 8, 2026, pageable)).thenReturn(page);
		when(expenseRepository.sumAmountGroupedByCategory(1L, 2026, 8))
				.thenReturn(List.of());
		when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(2L, "Food", "#FF0000", null, true));

		Page<MonthlyPlanningItemResponse> result = monthlyPlanningService.list(1L, 8, 2026, pageable);

		MonthlyPlanningItemResponse item = result.getContent().get(0);
		assertThat(item.spentAmount()).isEqualByComparingTo("0");
		assertThat(item.remainingAmount()).isEqualByComparingTo("1000.00");
		assertThat(item.percentageUsed()).isEqualByComparingTo("0.00");
	}

	private static CategoryTotalProjection projection(Long categoryId, BigDecimal total) {
		return new CategoryTotalProjection() {
			@Override
			public Long getCategoryId() {
				return categoryId;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

}
