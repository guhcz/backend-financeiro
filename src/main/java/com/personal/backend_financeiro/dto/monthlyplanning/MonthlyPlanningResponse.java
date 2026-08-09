package com.personal.backend_financeiro.dto.monthlyplanning;

import com.personal.backend_financeiro.dto.category.CategoryResponse;

import java.math.BigDecimal;

public record MonthlyPlanningResponse(

		Long id,
		CategoryResponse category,
		Integer month,
		Integer year,
		BigDecimal amount,
		boolean active

) {
}
