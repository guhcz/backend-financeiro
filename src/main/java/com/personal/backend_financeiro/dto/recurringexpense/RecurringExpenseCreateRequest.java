package com.personal.backend_financeiro.dto.recurringexpense;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RecurringExpenseCreateRequest")
public record RecurringExpenseCreateRequest(

		@NotNull
		Long categoryId,

		@NotBlank
		@Size(max = 255)
		String description,

		@NotNull
		@Positive
		BigDecimal amount,

		@NotNull
		Long transactionMethodId,

		@Schema(description = "Obrigatório quando a forma de pagamento é do tipo CARD; ignorado/deve ser nulo para as demais.")
		CardTransactionMode cardTransactionMode,

		@Size(max = 500)
		String notes,

		@NotNull
		@Schema(description = "Somente MONTHLY é suportado no momento.", example = "MONTHLY")
		RecurrenceFrequency frequency,

		@Min(value = 1, message = "O dia do vencimento deve estar entre 1 e 31.")
		@Max(value = 31, message = "O dia do vencimento deve estar entre 1 e 31.")
		@Schema(description = """
				Dia do vencimento (1-31), opcional. Se o mês não tiver esse dia, usa-se o \
				último dia válido. Deixe em branco para uma despesa fixa sem data de \
				vencimento (ex.: uma reserva mensal) — nesse caso a geração automática \
				ainda ocorre todo mês, no dia 1, mas o campo permanece nulo.""", example = "10")
		Integer dueDay,

		@NotNull
		LocalDate startDate,

		LocalDate endDate

) {
}
