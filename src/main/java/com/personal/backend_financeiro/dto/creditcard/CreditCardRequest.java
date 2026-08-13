package com.personal.backend_financeiro.dto.creditcard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreditCardRequest(

		@NotBlank
		@Size(max = 100)
		String name,

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
