package com.personal.backend_financeiro.dto.financialanalysis;

import java.math.BigDecimal;

public record IncomeVsExpensesPointResponse(

		Integer month,
		Integer year,
		BigDecimal incomeAmount,
		BigDecimal expenseAmount

) {
}
