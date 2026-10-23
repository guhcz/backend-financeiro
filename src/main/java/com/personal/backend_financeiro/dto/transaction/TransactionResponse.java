package com.personal.backend_financeiro.dto.transaction;

import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.enums.CardTransactionMode;
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
		@Schema(description = "Nome da forma de pagamento (despesa, ex.: \"Nubank\", \"Pix\") ou de recebimento " +
				"(receita), como texto solto — os dois domínios usam modelos diferentes (TransactionMethod / ReceiptMethod).")
		String method,
		@Schema(description = "CREDIT/DEBIT quando a despesa foi feita em um TransactionMethod do tipo CARD; nulo " +
				"para qualquer outra forma de pagamento e para receitas.")
		CardTransactionMode cardTransactionMode,
		boolean recurring,
		boolean generatedAutomatically,
		String notes,
		CategoryResponse category,
		@Schema(description = "Mês/ano de competência (fatura, para cartão de crédito; mês da própria data para " +
				"os demais meios). Nulo para receitas, que não têm conceito de fatura.")
		Integer billingMonth,
		Integer billingYear,
		@Schema(description = "Número desta parcela; nulo quando não for uma compra parcelada.")
		Integer installmentNumber,
		@Schema(description = "Quantidade total de parcelas; nulo quando não for uma compra parcelada.")
		Integer installmentCount

) {
}
