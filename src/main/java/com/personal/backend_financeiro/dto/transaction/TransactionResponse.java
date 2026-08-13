package com.personal.backend_financeiro.dto.transaction;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "TransactionResponse")
public record TransactionResponse(

		Long id,
		TransactionType type,
		@Schema(description = "Identificador lógico único (\"EXPENSE-10\"/\"INCOME-10\"): use-o no lugar de id " +
				"para chaves de UI, já que Expense e Income podem ter o mesmo id numérico.", example = "INCOME-5")
		String transactionKey,
		String description,
		BigDecimal amount,
		LocalDate date,
		@Schema(description = "Forma de pagamento (despesa) ou de recebimento (receita), como texto solto — " +
				"os dois domínios usam enums diferentes (PaymentMethod / ReceiptMethod).")
		String method,
		boolean recurring,
		boolean generatedAutomatically,
		String notes,
		CategoryResponse category,
		@Schema(description = "Mês/ano de competência (fatura, para cartão de crédito; mês da própria data para " +
				"os demais meios). Nulo para receitas, que não têm conceito de fatura.")
		Integer billingMonth,
		Integer billingYear

) {
}
