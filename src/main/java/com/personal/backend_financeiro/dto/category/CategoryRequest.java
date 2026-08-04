package com.personal.backend_financeiro.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex color like #RRGGBB")
		String color,

		@Size(max = 50)
		String icon

) {
}
