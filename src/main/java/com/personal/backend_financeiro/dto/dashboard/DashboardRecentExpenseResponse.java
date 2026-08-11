package com.personal.backend_financeiro.dto.dashboard;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardRecentExpenseResponse(

		Long id,
		String description,
		BigDecimal amount,
		LocalDate expenseDate,
		PaymentMethod paymentMethod,
		boolean recurring,
		CategoryResponse category

) {
}
