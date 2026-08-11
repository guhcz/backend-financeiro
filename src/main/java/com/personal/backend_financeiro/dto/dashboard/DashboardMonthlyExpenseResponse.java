package com.personal.backend_financeiro.dto.dashboard;

import java.math.BigDecimal;

public record DashboardMonthlyExpenseResponse(

		Integer month,
		Integer year,
		BigDecimal amount

) {
}
