package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.dto.planning.ExpenseEvolutionPointResponse;
import com.personal.backend_financeiro.dto.planning.PlanningSummaryResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.MonthlyLimit;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CategoryTotalProjection;
import com.personal.backend_financeiro.repository.DateTotalProjection;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyLimitRepository;
import com.personal.backend_financeiro.repository.MonthlyPlanningRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private MonthlyPlanningRepository monthlyPlanningRepository;
	@Mock
	private MonthlyLimitRepository monthlyLimitRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private PlanningService planningService;

	@Test
	void summary_percentageBelow100_whenSpentIsLessThanLimit() {
		when(expenseRepository.sumAmountByUserAndPeriod(1L, 2026, 8)).thenReturn(new BigDecimal("5000.00"));
		when(monthlyPlanningRepository.sumAmountByUserAndPeriod(1L, 8, 2026)).thenReturn(BigDecimal.ZERO);
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 8))
				.thenReturn(Optional.of(limit(new BigDecimal("10000.00"))));

		PlanningSummaryResponse summary = planningService.summary(1L, 8, 2026);

		assertThat(summary.percentageUsed()).isEqualByComparingTo("50.00");
		assertThat(summary.availableAmount()).isEqualByComparingTo("5000.00");
	}

	@Test
	void summary_percentageExactly100_whenSpentEqualsLimit() {
		when(expenseRepository.sumAmountByUserAndPeriod(1L, 2026, 8)).thenReturn(new BigDecimal("5000.00"));
		when(monthlyPlanningRepository.sumAmountByUserAndPeriod(1L, 8, 2026)).thenReturn(BigDecimal.ZERO);
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 8))
				.thenReturn(Optional.of(limit(new BigDecimal("5000.00"))));

		PlanningSummaryResponse summary = planningService.summary(1L, 8, 2026);

		assertThat(summary.percentageUsed()).isEqualByComparingTo("100.00");
		assertThat(summary.availableAmount()).isEqualByComparingTo("0.00");
	}

	@Test
	void summary_percentageAbove100_whenSpentExceedsLimit() {
		when(expenseRepository.sumAmountByUserAndPeriod(1L, 2026, 8)).thenReturn(new BigDecimal("5500.00"));
		when(monthlyPlanningRepository.sumAmountByUserAndPeriod(1L, 8, 2026)).thenReturn(BigDecimal.ZERO);
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 8))
				.thenReturn(Optional.of(limit(new BigDecimal("5000.00"))));

		PlanningSummaryResponse summary = planningService.summary(1L, 8, 2026);

		assertThat(summary.percentageUsed()).isEqualByComparingTo("110.00");
		assertThat(summary.availableAmount()).isEqualByComparingTo("-500.00");
	}

	@Test
	void summary_withoutMonthlyLimit_returnsNullLimitFields_butKeepsSpentAndPlanned() {
		when(expenseRepository.sumAmountByUserAndPeriod(1L, 2026, 8)).thenReturn(new BigDecimal("8250.00"));
		when(monthlyPlanningRepository.sumAmountByUserAndPeriod(1L, 8, 2026)).thenReturn(new BigDecimal("2300.00"));
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 8)).thenReturn(Optional.empty());

		PlanningSummaryResponse summary = planningService.summary(1L, 8, 2026);

		assertThat(summary.monthlyLimit()).isNull();
		assertThat(summary.availableAmount()).isNull();
		assertThat(summary.percentageUsed()).isNull();
		assertThat(summary.unplannedAmount()).isNull();
		assertThat(summary.totalSpent()).isEqualByComparingTo("8250.00");
		assertThat(summary.totalPlanned()).isEqualByComparingTo("2300.00");
	}

	@Test
	void summary_userWithoutPlanningOrExpenses_returnsZeroTotals() {
		when(expenseRepository.sumAmountByUserAndPeriod(1L, 2026, 8)).thenReturn(BigDecimal.ZERO);
		when(monthlyPlanningRepository.sumAmountByUserAndPeriod(1L, 8, 2026)).thenReturn(BigDecimal.ZERO);
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 8)).thenReturn(Optional.empty());

		PlanningSummaryResponse summary = planningService.summary(1L, 8, 2026);

		assertThat(summary.totalSpent()).isEqualByComparingTo("0");
		assertThat(summary.totalPlanned()).isEqualByComparingTo("0");
	}

	@Test
	void expensesByCategory_returnsEmptyList_whenNoExpenses() {
		when(expenseRepository.sumAmountGroupedByCategory(1L, 2026, 8)).thenReturn(List.of());

		List<CategoryExpenseResponse> result = planningService.expensesByCategory(1L, 8, 2026);

		assertThat(result).isEmpty();
	}

	@Test
	void expensesByCategory_computesPercentageAgainstGrandTotal() {
		Category food = new Category();
		food.setId(1L);
		when(expenseRepository.sumAmountGroupedByCategory(1L, 2026, 8))
				.thenReturn(List.of(projection(1L, new BigDecimal("600.00"))));
		when(categoryRepository.findAllById(List.of(1L))).thenReturn(List.of(food));
		when(categoryMapper.toResponse(food)).thenReturn(new CategoryResponse(1L, "Food", "#FF0000", null, true));

		List<CategoryExpenseResponse> result = planningService.expensesByCategory(1L, 8, 2026);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).percentage()).isEqualByComparingTo("100.00");
	}

	@Test
	void expenseEvolution_accumulatesMultipleDays() {
		when(expenseRepository.sumAmountGroupedByDate(1L, 2026, 8)).thenReturn(List.of(
				dateProjection(LocalDate.of(2026, 8, 1), new BigDecimal("850.00")),
				dateProjection(LocalDate.of(2026, 8, 5), new BigDecimal("1130.00"))));

		List<ExpenseEvolutionPointResponse> points = planningService.expenseEvolution(1L, 8, 2026);

		assertThat(points).hasSize(2);
		assertThat(points.get(0).accumulatedAmount()).isEqualByComparingTo("850.00");
		assertThat(points.get(1).accumulatedAmount()).isEqualByComparingTo("1980.00");
	}

	@Test
	void expenseEvolution_returnsEmptyList_whenUserHasNoExpenses() {
		when(expenseRepository.sumAmountGroupedByDate(1L, 2026, 8)).thenReturn(List.of());

		List<ExpenseEvolutionPointResponse> points = planningService.expenseEvolution(1L, 8, 2026);

		assertThat(points).isEmpty();
	}

	private static MonthlyLimit limit(BigDecimal amount) {
		MonthlyLimit limit = new MonthlyLimit();
		limit.setAmount(amount);
		return limit;
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

	private static DateTotalProjection dateProjection(LocalDate date, BigDecimal total) {
		return new DateTotalProjection() {
			@Override
			public LocalDate getDate() {
				return date;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

}
