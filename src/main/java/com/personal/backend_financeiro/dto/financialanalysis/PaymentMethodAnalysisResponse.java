package com.personal.backend_financeiro.dto.financialanalysis;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.TransactionMethodType;

import java.math.BigDecimal;

public record PaymentMethodAnalysisResponse(

		Long transactionMethodId,
		String name,
		TransactionMethodType methodType,
		CardTransactionMode cardMode,
		BigDecimal amount,
		BigDecimal percentage,
		Long transactionCount

) {
}
