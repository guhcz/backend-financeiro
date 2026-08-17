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
		Boolean recurring,

		/**
		 * Optional financial-competence filter, additive to startDate/endDate (does not replace
		 * them, see TransactionRepository). When both month and year are present, expenses are
		 * filtered by their billing month/year (invoice month for credit card, purchase month for
		 * everything else) and incomes by their incomeDate's month/year.
		 */
		Integer month,
		Integer year

) {
}
