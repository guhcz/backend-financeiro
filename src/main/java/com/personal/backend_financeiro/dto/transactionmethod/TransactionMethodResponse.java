package com.personal.backend_financeiro.dto.transactionmethod;

import com.personal.backend_financeiro.enums.TransactionMethodType;

public record TransactionMethodResponse(

		Long id,
		String name,
		TransactionMethodType type,
		boolean active,
		TransactionMethodCardDetailsResponse card

) {
}
