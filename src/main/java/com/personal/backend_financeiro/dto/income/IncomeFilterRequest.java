package com.personal.backend_financeiro.dto.income;

import com.personal.backend_financeiro.enums.ReceiptMethod;

import java.time.LocalDate;

/**
 * All fields are optional filters; a null field means "do not filter by this".
 */
public record IncomeFilterRequest(

		Long categoryId,
		ReceiptMethod receiptMethod,
		LocalDate startDate,
		LocalDate endDate,
		String description,
		Boolean recurring

) {
}
