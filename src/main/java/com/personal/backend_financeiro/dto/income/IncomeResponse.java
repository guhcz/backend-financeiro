package com.personal.backend_financeiro.dto.income;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.ReceiptMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeResponse(

		Long id,
		CategoryResponse category,
		String description,
		BigDecimal amount,
		LocalDate incomeDate,
		ReceiptMethod receiptMethod,
		String notes,
		boolean active,
		boolean generatedAutomatically,
		boolean recurring,
		Long recurringIncomeId

) {
}
