package com.personal.backend_financeiro.dto.recurringexpense;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RecurringExpenseResponse")
public record RecurringExpenseResponse(

		Long id,
		CategoryResponse category,
		String description,
		BigDecimal amount,
		TransactionMethodResponse transactionMethod,
		CardTransactionMode cardTransactionMode,
		String notes,
		RecurrenceFrequency frequency,
		Integer dueDay,
		LocalDate startDate,
		LocalDate endDate,
		Integer installmentCount,
		@Schema(description = "Data em que a próxima despesa será gerada automaticamente.")
		LocalDate nextGenerationDate,
		@Schema(description = "true se a regra está ativa (gerando despesas mensalmente); false se pausada ou encerrada.")
		boolean active,
		@Schema(description = "Status detalhado da regra: ACTIVE, PAUSED ou ENDED.", example = "ACTIVE")
		String status

) {
}
