package com.personal.backend_financeiro.dto.expense;

import java.time.LocalDate;

/**
 * All fields are optional filters; a null field means "do not filter by this".
 */
public record ExpenseFilterRequest(

		Long categoryId,
		Long transactionMethodId,
		LocalDate startDate,
		LocalDate endDate,
		String description,
		Boolean recurring

) {
}
