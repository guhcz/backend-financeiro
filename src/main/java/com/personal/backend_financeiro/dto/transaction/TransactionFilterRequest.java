package com.personal.backend_financeiro.dto.transaction;

import java.time.LocalDate;

/**
 * All fields are optional filters; a null field means "do not filter by this". When type is
 * absent, both expenses and incomes are returned.
 */
public record TransactionFilterRequest(

		TransactionType type,
		Long categoryId,
		LocalDate startDate,
		LocalDate endDate,
		String description,
		Boolean recurring

) {
}
