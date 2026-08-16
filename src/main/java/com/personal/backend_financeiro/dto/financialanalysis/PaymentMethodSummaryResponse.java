package com.personal.backend_financeiro.dto.financialanalysis;

import com.personal.backend_financeiro.enums.TransactionMethodType;

public record PaymentMethodSummaryResponse(

		Long id,
		String name,
		TransactionMethodType type

) {
}
