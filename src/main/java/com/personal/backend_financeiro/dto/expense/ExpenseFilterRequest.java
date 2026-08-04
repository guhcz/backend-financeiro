package com.personal.backend_financeiro.dto.expense;

import com.personal.backend_financeiro.enums.PaymentMethod;

import java.time.LocalDate;

/**
 * All fields are optional filters; a null field means "do not filter by this".
 */
public record ExpenseFilterRequest(

		Long categoryId,
		PaymentMethod paymentMethod,
		LocalDate startDate,
		LocalDate endDate,
		String description

) {
}
