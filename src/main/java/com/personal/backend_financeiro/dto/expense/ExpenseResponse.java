package com.personal.backend_financeiro.dto.expense;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(

		Long id,
		CategoryResponse category,
		String description,
		BigDecimal amount,
		LocalDate expenseDate,
		PaymentMethod paymentMethod,
		String notes,
		boolean active

) {
}
