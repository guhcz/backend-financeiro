package com.personal.backend_financeiro.dto.monthlyplanning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MonthlyPlanningRequest(

		@NotNull
		Long categoryId,

		@NotNull
		@Min(1)
		@Max(12)
		Integer month,

		@NotNull
		@Min(2000)
		Integer year,

		@NotNull
		@Positive
		BigDecimal amount

) {
}
