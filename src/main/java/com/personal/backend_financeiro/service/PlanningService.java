package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.dto.planning.ExpenseEvolutionPointResponse;
import com.personal.backend_financeiro.dto.planning.PlanningDashboardResponse;
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
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanningService {

	private static final int PERCENTAGE_SCALE = 2;

	private final ExpenseRepository expenseRepository;
	private final MonthlyPlanningRepository monthlyPlanningRepository;
	private final MonthlyLimitRepository monthlyLimitRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public PlanningSummaryResponse summary(Long userId, Integer month, Integer year) {
		PlanningPeriodUtils.assertValid(month, year);

		LocalDate start = PlanningPeriodUtils.firstDayOf(year, month);
		LocalDate end = PlanningPeriodUtils.lastDayOf(year, month);

		BigDecimal totalSpent = expenseRepository.sumAmountByUserAndPeriod(userId, start, end);
		BigDecimal totalPlanned = monthlyPlanningRepository.sumAmountByUserAndPeriod(userId, month, year);

		return monthlyLimitRepository.findByUserIdAndYearAndMonth(userId, year, month)
				.map(limit -> buildSummaryWithLimit(month, year, limit, totalSpent, totalPlanned))
				.orElseGet(() -> new PlanningSummaryResponse(month, year, null, totalSpent, null, null, totalPlanned, null));
	}

	private PlanningSummaryResponse buildSummaryWithLimit(
			Integer month, Integer year, MonthlyLimit limit, BigDecimal totalSpent, BigDecimal totalPlanned) {
		BigDecimal monthlyLimit = limit.getAmount();
		BigDecimal availableAmount = monthlyLimit.subtract(totalSpent);
		BigDecimal percentageUsed = percentageOf(totalSpent, monthlyLimit);
		BigDecimal unplannedAmount = monthlyLimit.subtract(totalPlanned);

		return new PlanningSummaryResponse(
				month, year, monthlyLimit, totalSpent, availableAmount, percentageUsed, totalPlanned, unplannedAmount);
	}

	public List<CategoryExpenseResponse> expensesByCategory(Long userId, Integer month, Integer year) {
		PlanningPeriodUtils.assertValid(month, year);

		LocalDate start = PlanningPeriodUtils.firstDayOf(year, month);
		LocalDate end = PlanningPeriodUtils.lastDayOf(year, month);

		List<CategoryTotalProjection> totals = expenseRepository.sumAmountGroupedByCategory(userId, start, end);
		if (totals.isEmpty()) {
			return List.of();
		}

		BigDecimal grandTotal = totals.stream().map(CategoryTotalProjection::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<Long, Category> categoriesById = categoryRepository
				.findAllById(totals.stream().map(CategoryTotalProjection::getCategoryId).toList())
				.stream()
				.collect(Collectors.toMap(Category::getId, c -> c));

		return totals.stream()
				.sorted(Comparator.comparing(CategoryTotalProjection::getTotal).reversed())
				.map(projection -> new CategoryExpenseResponse(
						categoryMapper.toResponse(categoriesById.get(projection.getCategoryId())),
						projection.getTotal(),
						percentageOf(projection.getTotal(), grandTotal)))
				.toList();
	}

	public List<ExpenseEvolutionPointResponse> expenseEvolution(Long userId, Integer month, Integer year) {
		PlanningPeriodUtils.assertValid(month, year);

		LocalDate start = PlanningPeriodUtils.firstDayOf(year, month);
		LocalDate end = PlanningPeriodUtils.lastDayOf(year, month);

		List<DateTotalProjection> dailyTotals = expenseRepository.sumAmountGroupedByDate(userId, start, end);

		BigDecimal accumulated = BigDecimal.ZERO;
		List<ExpenseEvolutionPointResponse> points = new ArrayList<>(dailyTotals.size());
		for (DateTotalProjection dailyTotal : dailyTotals) {
			accumulated = accumulated.add(dailyTotal.getTotal());
			points.add(new ExpenseEvolutionPointResponse(dailyTotal.getDate(), dailyTotal.getTotal(), accumulated));
		}
		return points;
	}

	public PlanningDashboardResponse dashboard(Long userId, Integer month, Integer year) {
		return new PlanningDashboardResponse(
				summary(userId, month, year),
				expensesByCategory(userId, month, year),
				expenseEvolution(userId, month, year));
	}

	private BigDecimal percentageOf(BigDecimal part, BigDecimal total) {
		if (total.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return part.divide(total, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
	}

}
