package com.personal.backend_financeiro.dto.income;

import com.personal.backend_financeiro.enums.ReceiptMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(

		@NotNull
		Long categoryId,

		@NotBlank
		@Size(max = 255)
		String description,

		@NotNull
		@Positive
		BigDecimal amount,

		@NotNull
		LocalDate incomeDate,

		@NotNull
		ReceiptMethod receiptMethod,

		@Size(max = 500)
		String notes

) {
}
