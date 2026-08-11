package com.personal.backend_financeiro.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(

		BigDecimal totalIncome,
		BigDecimal totalExpenses,
		BigDecimal balance,
		BigDecimal monthlyLimit,
		BigDecimal availableAmount,
		BigDecimal limitPercentageUsed

) {
}
