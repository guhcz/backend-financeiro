package com.personal.backend_financeiro.dto.financialanalysis;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.CardTransactionMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LargestExpenseResponse(

		Long id,
		String description,
		CategoryResponse category,
		PaymentMethodSummaryResponse transactionMethod,
		CardTransactionMode cardMode,
		LocalDate date,
		Integer billingMonth,
		Integer billingYear,
		BigDecimal amount

) {
}
