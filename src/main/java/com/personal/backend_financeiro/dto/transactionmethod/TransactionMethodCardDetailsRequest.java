package com.personal.backend_financeiro.dto.transactionmethod;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransactionMethodCardDetailsRequest(

		@NotNull
		@Min(1)
		@Max(31)
		Integer closingDay,

		@NotNull
		@Min(1)
		@Max(31)
		Integer dueDay

) {
}
