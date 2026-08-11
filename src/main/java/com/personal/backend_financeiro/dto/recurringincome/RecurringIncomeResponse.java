package com.personal.backend_financeiro.dto.recurringincome;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.ReceiptMethod;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RecurringIncomeResponse")
public record RecurringIncomeResponse(

		Long id,
		CategoryResponse category,
		String description,
		BigDecimal amount,
		ReceiptMethod receiptMethod,
		String notes,
		RecurrenceFrequency frequency,
		Integer receiptDay,
		LocalDate startDate,
		LocalDate endDate,
		@Schema(description = "Data em que a próxima receita será gerada automaticamente.")
		LocalDate nextGenerationDate,
		@Schema(description = "true se a regra está ativa (gerando receitas mensalmente); false se pausada ou encerrada.")
		boolean active,
		@Schema(description = "Status detalhado da regra: ACTIVE, PAUSED ou ENDED.", example = "ACTIVE")
		String status

) {
}
