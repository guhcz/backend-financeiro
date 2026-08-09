package com.personal.backend_financeiro.dto.planning;

import java.util.List;

public record PlanningDashboardResponse(

		PlanningSummaryResponse summary,
		List<CategoryExpenseResponse> expensesByCategory,
		List<ExpenseEvolutionPointResponse> expenseEvolution

) {
}
