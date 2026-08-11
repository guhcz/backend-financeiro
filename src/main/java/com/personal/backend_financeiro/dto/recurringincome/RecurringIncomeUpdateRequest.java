package com.personal.backend_financeiro.dto.recurringincome;

import com.personal.backend_financeiro.enums.ReceiptMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RecurringIncomeUpdateRequest", description = "startDate e frequency são imutáveis após a criação e não fazem parte deste payload.")
public record RecurringIncomeUpdateRequest(

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

		@Min(value = 1, message = "O dia de recebimento deve estar entre 1 e 31.")
		@Max(value = 31, message = "O dia de recebimento deve estar entre 1 e 31.")
		@Schema(description = """
				Dia de recebimento (1-31), opcional (deixe em branco para uma receita sem \
				data fixa). Alterar este valor numa regra ativa recalcula a próxima geração.""", example = "5")
		Integer receiptDay,

		LocalDate endDate

) {
}
