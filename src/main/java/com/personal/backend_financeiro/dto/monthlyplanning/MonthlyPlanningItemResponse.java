package com.personal.backend_financeiro.dto.monthlyplanning;

import com.personal.backend_financeiro.dto.category.CategoryResponse;

import java.math.BigDecimal;

public record MonthlyPlanningItemResponse(

		Long id,
		CategoryResponse category,
		BigDecimal plannedAmount,
		BigDecimal spentAmount,
		BigDecimal remainingAmount,
		BigDecimal percentageUsed

) {
}
