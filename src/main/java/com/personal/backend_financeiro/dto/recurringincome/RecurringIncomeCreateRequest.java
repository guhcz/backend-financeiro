package com.personal.backend_financeiro.dto.recurringincome;

import com.personal.backend_financeiro.enums.ReceiptMethod;
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

@Schema(name = "RecurringIncomeCreateRequest")
public record RecurringIncomeCreateRequest(

		@NotNull
		Long categoryId,

		@NotBlank
		@Size(max = 255)
		String description,

		@NotNull
		@Positive
		BigDecimal amount,

		@NotNull
		ReceiptMethod receiptMethod,

		@Size(max = 500)
		String notes,

		@NotNull
		@Schema(description = "Somente MONTHLY é suportado no momento.", example = "MONTHLY")
		RecurrenceFrequency frequency,

		@Min(value = 1, message = "O dia de recebimento deve estar entre 1 e 31.")
		@Max(value = 31, message = "O dia de recebimento deve estar entre 1 e 31.")
		@Schema(description = """
				Dia de recebimento (1-31), opcional. Se o mês não tiver esse dia, usa-se o \
				último dia válido. Deixe em branco para uma receita recorrente sem uma data \
				de recebimento definida (ex.: renda variável mensal) — nesse caso a geração \
				automática ainda ocorre todo mês, no dia 1, mas o campo permanece nulo. Essa \
				data técnica de referência não deve ser exibida como "data de recebimento \
				prevista" quando o campo for nulo.""", example = "5")
		Integer receiptDay,

		@NotNull
		LocalDate startDate,

		LocalDate endDate

) {
}
