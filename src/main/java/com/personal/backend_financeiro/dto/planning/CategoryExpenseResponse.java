package com.personal.backend_financeiro.dto.planning;

import com.personal.backend_financeiro.dto.category.CategoryResponse;

import java.math.BigDecimal;

public record CategoryExpenseResponse(

		CategoryResponse category,
		BigDecimal amount,
		BigDecimal percentage

) {
}
