package com.personal.backend_financeiro.dto.monthlylimit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MonthlyLimitRequest(

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
