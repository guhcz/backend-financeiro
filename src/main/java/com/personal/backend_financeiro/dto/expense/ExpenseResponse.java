package com.personal.backend_financeiro.dto.expense;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;
import com.personal.backend_financeiro.enums.CardTransactionMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(

		Long id,
		CategoryResponse category,
		String description,
		BigDecimal amount,
		LocalDate expenseDate,
		TransactionMethodResponse transactionMethod,
		CardTransactionMode cardTransactionMode,
		String notes,
		boolean active,
		boolean generatedAutomatically,
		boolean recurring,
		Long recurringExpenseId,
		Integer billingMonth,
		Integer billingYear

) {
}
