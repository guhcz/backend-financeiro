package com.personal.backend_financeiro.dto.category;

public record CategoryResponse(

		Long id,
		String name,
		String color,
		String icon,
		boolean active

) {
}
