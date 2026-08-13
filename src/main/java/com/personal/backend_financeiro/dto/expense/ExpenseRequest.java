package com.personal.backend_financeiro.dto.expense;

import com.personal.backend_financeiro.enums.PaymentMethod;
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
		PaymentMethod paymentMethod,

		@Size(max = 500)
		String notes,

		/**
		 * Required when paymentMethod is CREDIT_CARD (validated in ExpenseService, not here,
		 * since the requirement is conditional on another field). Ignored for every other
		 * payment method. Used only to track spend per card for card planning -- the invoice
		 * cycle no longer affects financial competence (see CompetenceResolver).
		 */
		Long creditCardId

) {
}
