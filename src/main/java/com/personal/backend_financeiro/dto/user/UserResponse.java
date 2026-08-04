package com.personal.backend_financeiro.dto.user;

import java.time.Instant;

public record UserResponse(

		Long id,
		String name,
		String email,
		boolean active,
		Instant createdAt

) {
}
