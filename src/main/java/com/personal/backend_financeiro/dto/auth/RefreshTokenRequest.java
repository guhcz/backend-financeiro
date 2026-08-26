package com.personal.backend_financeiro.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

		@NotBlank
		String refreshToken

) {
}
