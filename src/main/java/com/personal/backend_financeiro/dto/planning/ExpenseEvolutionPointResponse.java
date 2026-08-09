package com.personal.backend_financeiro.dto.planning;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseEvolutionPointResponse(

		LocalDate date,
		BigDecimal dailyAmount,
		BigDecimal accumulatedAmount

) {
}
