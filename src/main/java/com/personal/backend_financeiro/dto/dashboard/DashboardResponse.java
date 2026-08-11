package com.personal.backend_financeiro.dto.dashboard;

import com.personal.backend_financeiro.dto.planning.CategoryExpenseResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(

		Integer month,
		Integer year,
		DashboardSummaryResponse summary,
		List<DashboardRecentExpenseResponse> recentExpenses,
		List<CategoryExpenseResponse> expensesByCategory,
		List<DashboardMonthlyExpenseResponse> monthlyExpenseHistory,
		DashboardRecurringSummaryResponse recurringExpensesSummary,
		BigDecimal averageDailyExpense,
		DashboardFinancialStatusResponse financialStatus

) {
}
