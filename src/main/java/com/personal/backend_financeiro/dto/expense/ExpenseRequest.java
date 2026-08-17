package com.personal.backend_financeiro.dto.expense;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(

		@NotNull
		Long categoryId,

		@NotBlank
		@Size(max = 255)
		String description,

		@NotNull
		@Positive
		BigDecimal amount,

		@NotNull
		LocalDate expenseDate,

		@NotNull
		Long transactionMethodId,

		/**
		 * Required when transactionMethodId points to a type CARD method (validated in
		 * ExpenseService, not here, since the requirement is conditional on another field).
		 * Ignored/must be null for every other transaction method type.
		 */
		CardTransactionMode cardTransactionMode,

		@Size(max = 500)
		String notes

) {
}
