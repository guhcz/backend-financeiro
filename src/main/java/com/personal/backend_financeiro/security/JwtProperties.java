package com.personal.backend_financeiro.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

		@NotBlank
		String secret,

		@Positive
		long expiration,

		@Positive
		long refreshExpiration

) {
}
