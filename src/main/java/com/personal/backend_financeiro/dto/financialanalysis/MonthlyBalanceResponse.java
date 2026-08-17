package com.personal.backend_financeiro.dto.financialanalysis;

import java.math.BigDecimal;

public record MonthlyBalanceResponse(

		Integer month,
		Integer year,
		BigDecimal incomeAmount,
		BigDecimal expenseAmount,
		BigDecimal balance

) {
}
