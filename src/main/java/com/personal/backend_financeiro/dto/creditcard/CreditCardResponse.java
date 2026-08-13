package com.personal.backend_financeiro.dto.creditcard;

public record CreditCardResponse(

		Long id,
		String name,
		Integer closingDay,
		Integer dueDay,
		boolean active

) {
}
