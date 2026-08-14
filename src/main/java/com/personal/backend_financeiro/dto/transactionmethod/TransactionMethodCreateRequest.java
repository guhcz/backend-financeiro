package com.personal.backend_financeiro.dto.transactionmethod;

import com.personal.backend_financeiro.enums.TransactionMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionMethodCreateRequest(

		@NotBlank
		@Size(max = 100)
		String name,

		@NotNull
		TransactionMethodType type,

		@Valid
		@Schema(description = "Obrigatório quando type é CARD; ignorado para os demais tipos.")
		TransactionMethodCardDetailsRequest card

) {
}
