package com.personal.backend_financeiro.dto.monthlycardplanning;

import com.personal.backend_financeiro.dto.creditcard.CreditCardResponse;

import java.math.BigDecimal;

public record MonthlyCardPlanningItemResponse(

		Long id,
		CreditCardResponse creditCard,
		BigDecimal plannedAmount,
		BigDecimal spentAmount,
		BigDecimal remainingAmount,
		BigDecimal percentageUsed

) {
}
