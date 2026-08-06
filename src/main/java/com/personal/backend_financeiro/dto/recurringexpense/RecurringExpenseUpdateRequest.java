package com.personal.backend_financeiro.dto.recurringexpense;

import com.personal.backend_financeiro.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RecurringExpenseUpdateRequest", description = "startDate e frequency são imutáveis após a criação e não fazem parte deste payload.")
public record RecurringExpenseUpdateRequest(

		@NotNull
		Long categoryId,

		@NotBlank
		@Size(max = 255)
		String description,

		@NotNull
		@Positive
		BigDecimal amount,

		@NotNull
		PaymentMethod paymentMethod,

		@Size(max = 500)
		String notes,

		@NotNull
		@Min(value = 1, message = "O dia do vencimento deve estar entre 1 e 31.")
		@Max(value = 31, message = "O dia do vencimento deve estar entre 1 e 31.")
		@Schema(description = "Dia do vencimento (1-31). Alterar este valor numa regra ativa recalcula a próxima geração.", example = "10")
		Integer dueDay,

		LocalDate endDate

) {
}
