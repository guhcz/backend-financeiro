package com.personal.backend_financeiro.dto.planning;

import java.math.BigDecimal;

public record PlanningSummaryResponse(

		Integer month,
		Integer year,
		BigDecimal monthlyLimit,
		BigDecimal totalSpent,
		BigDecimal availableAmount,
		BigDecimal percentageUsed,
		BigDecimal totalPlanned,
		BigDecimal unplannedAmount

) {
}
