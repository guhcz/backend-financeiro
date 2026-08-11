package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.dashboard.DashboardFinancialStatusResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardMonthlyExpenseResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardRecentExpenseResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardRecurringSummaryResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardResponse;
import com.personal.backend_financeiro.dto.dashboard.DashboardSummaryResponse;
import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;
import com.personal.backend_financeiro.dto.planning.PlanningSummaryResponse;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.enums.FinancialStatusType;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private static final int RECENT_EXPENSES_LIMIT = 5;
	private static final int CATEGORIES_LIMIT = 5;
	private static final int HISTORY_MONTHS = 6;
	private static final int DUE_SOON_DAYS = 7;
	private static final BigDecimal ATTENTION_THRESHOLD = BigDecimal.valueOf(80);
	private static final BigDecimal LIMIT_THRESHOLD = BigDecimal.valueOf(100);

	private final PlanningService planningService;
	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseRepository recurringExpenseRepository;
	private final CategoryMapper categoryMapper;

	public DashboardResponse getDashboard(Long userId, Integer month, Integer year) {
		PlanningPeriodUtils.assertValid(month, year);

		PlanningSummaryResponse planningSummary = planningService.summary(userId, month, year);

		DashboardSummaryResponse summary = new DashboardSummaryResponse(
				null,
				planningSummary.totalSpent(),
				null,
				planningSummary.monthlyLimit(),
				planningSummary.availableAmount(),
				planningSummary.percentageUsed());

		List<CategoryExpenseResponse> expensesByCategory = planningService.expensesByCategory(userId, month, year)
				.stream()
				.limit(CATEGORIES_LIMIT)
				.toList();

		return new DashboardResponse(
				month,
				year,
				summary,
				recentExpenses(userId, month, year),
				expensesByCategory,
				monthlyExpenseHistory(userId, month, year),
				recurringExpensesSummary(userId),
				averageDailyExpense(month, year, planningSummary.totalSpent()),
				financialStatus(planningSummary.percentageUsed()));
	}

	private List<DashboardRecentExpenseResponse> recentExpenses(Long userId, Integer month, Integer year) {
		LocalDate start = PlanningPeriodUtils.firstDayOf(year, month);
		LocalDate end = PlanningPeriodUtils.lastDayOf(year, month);

		return expenseRepository
				.findTop5ByUserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(userId, start, end)
				.stream()
				.limit(RECENT_EXPENSES_LIMIT)
				.map(this::toRecentExpenseResponse)
				.toList();
	}

	private DashboardRecentExpenseResponse toRecentExpenseResponse(Expense expense) {
		return new DashboardRecentExpenseResponse(
				expense.getId(),
				expense.getDescription(),
				expense.getAmount(),
				expense.getExpenseDate(),
				expense.getPaymentMethod(),
				expense.getRecurringExpense() != null,
				categoryMapper.toResponse(expense.getCategory()));
	}

	private List<DashboardMonthlyExpenseResponse> monthlyExpenseHistory(Long userId, Integer month, Integer year) {
		YearMonth selected = YearMonth.of(year, month);
		List<DashboardMonthlyExpenseResponse> history = new ArrayList<>(HISTORY_MONTHS);

		for (int i = HISTORY_MONTHS - 1; i >= 0; i--) {
			YearMonth reference = selected.minusMonths(i);
			LocalDate start = PlanningPeriodUtils.firstDayOf(reference.getYear(), reference.getMonthValue());
			LocalDate end = PlanningPeriodUtils.lastDayOf(reference.getYear(), reference.getMonthValue());
			BigDecimal amount = expenseRepository.sumAmountByUserAndPeriod(userId, start, end);
			history.add(new DashboardMonthlyExpenseResponse(reference.getMonthValue(), reference.getYear(), amount));
		}

		return history;
	}

	private DashboardRecurringSummaryResponse recurringExpensesSummary(Long userId) {
		LocalDate today = LocalDate.now();

		long activeCount = recurringExpenseRepository.countByUserIdAndStatus(userId, RecurrenceStatus.ACTIVE);
		long dueInNext7DaysCount = recurringExpenseRepository.countDueBetween(userId, today, today.plusDays(DUE_SOON_DAYS));
		BigDecimal dueTodayAmount = recurringExpenseRepository.sumAmountDueOn(userId, today);

		return new DashboardRecurringSummaryResponse(activeCount, dueInNext7DaysCount, dueTodayAmount);
	}

	private BigDecimal averageDailyExpense(Integer month, Integer year, BigDecimal totalExpenses) {
		YearMonth selected = YearMonth.of(year, month);
		YearMonth current = YearMonth.now();

		if (selected.isAfter(current)) {
			return BigDecimal.ZERO;
		}

		int elapsedDays = selected.equals(current) ? LocalDate.now().getDayOfMonth() : selected.lengthOfMonth();
		return totalExpenses.divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
	}

	private DashboardFinancialStatusResponse financialStatus(BigDecimal percentageUsed) {
		if (percentageUsed == null) {
			return new DashboardFinancialStatusResponse(FinancialStatusType.NO_LIMIT,
					"Defina um limite mensal para acompanhar melhor seus gastos.");
		}
		if (percentageUsed.compareTo(LIMIT_THRESHOLD) > 0) {
			return new DashboardFinancialStatusResponse(FinancialStatusType.OVER_LIMIT,
					"Você ultrapassou o limite mensal.");
		}
		if (percentageUsed.compareTo(LIMIT_THRESHOLD) == 0) {
			return new DashboardFinancialStatusResponse(FinancialStatusType.LIMIT_REACHED,
					"Você atingiu o limite mensal.");
		}
		if (percentageUsed.compareTo(ATTENTION_THRESHOLD) >= 0) {
			return new DashboardFinancialStatusResponse(FinancialStatusType.ATTENTION,
					"Você está se aproximando do limite mensal.");
		}
		return new DashboardFinancialStatusResponse(FinancialStatusType.WITHIN_LIMIT,
				"Você está dentro do limite. Continue assim!");
	}

}
