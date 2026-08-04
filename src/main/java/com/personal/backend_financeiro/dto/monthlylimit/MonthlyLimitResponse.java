package com.personal.backend_financeiro.dto.monthlylimit;

import java.math.BigDecimal;

public record MonthlyLimitResponse(

		Long id,
		Integer month,
		Integer year,
		BigDecimal amount,
		boolean active

) {
}
