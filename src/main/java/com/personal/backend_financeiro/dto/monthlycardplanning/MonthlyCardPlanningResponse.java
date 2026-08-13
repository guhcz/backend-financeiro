package com.personal.backend_financeiro.dto.monthlycardplanning;

import com.personal.backend_financeiro.dto.creditcard.CreditCardResponse;

import java.math.BigDecimal;

public record MonthlyCardPlanningResponse(

		Long id,
		CreditCardResponse creditCard,
		Integer month,
		Integer year,
		BigDecimal amount,
		boolean active

) {
}
