package com.personal.backend_financeiro.dto.recurringexpense;

/**
 * All fields are optional filters; a null field means "do not filter by this".
 */
public record RecurringExpenseFilterRequest(

		Boolean active,
		Long categoryId,
		String description

) {
}
