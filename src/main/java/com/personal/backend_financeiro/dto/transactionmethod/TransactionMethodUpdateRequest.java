package com.personal.backend_financeiro.dto.transactionmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "type é imutável após a criação e não faz parte deste payload.")
public record TransactionMethodUpdateRequest(

		@NotBlank
		@Size(max = 100)
		String name,

		@Valid
		@Schema(description = "Obrigatório quando o tipo original é CARD; ignorado para os demais tipos.")
		TransactionMethodCardDetailsRequest card

) {
}
