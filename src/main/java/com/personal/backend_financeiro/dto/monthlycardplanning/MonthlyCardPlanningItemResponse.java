package com.personal.backend_financeiro.dto.monthlycardplanning;

import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;

import java.math.BigDecimal;

public record MonthlyCardPlanningItemResponse(

		Long id,
		TransactionMethodResponse transactionMethod,
		BigDecimal plannedAmount,
		BigDecimal spentAmount,
		BigDecimal remainingAmount,
		BigDecimal percentageUsed

) {
}
