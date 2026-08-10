package com.personal.backend_financeiro.dto.recurringexpense;

/**
 * All fields are optional filters; a null field means "do not filter by this".
 * referenceMonth and referenceYear must be provided together: when both are set, only rules
 * whose period (startDate–endDate) overlaps that month/year are returned.
 */
public record RecurringExpenseFilterRequest(

		Boolean active,
		Long categoryId,
		String description,
		Integer referenceMonth,
		Integer referenceYear

) {
}
