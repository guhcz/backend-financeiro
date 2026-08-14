package com.personal.backend_financeiro.dto.monthlycardplanning;

import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;

import java.math.BigDecimal;

public record MonthlyCardPlanningResponse(

		Long id,
		TransactionMethodResponse transactionMethod,
		Integer month,
		Integer year,
		BigDecimal amount,
		boolean active

) {
}
