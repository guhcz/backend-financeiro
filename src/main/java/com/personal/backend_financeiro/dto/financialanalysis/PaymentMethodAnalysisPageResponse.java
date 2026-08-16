package com.personal.backend_financeiro.dto.financialanalysis;

import java.math.BigDecimal;
import java.util.List;

public record PaymentMethodAnalysisPageResponse(

		List<PaymentMethodAnalysisResponse> content,
		int number,
		int size,
		long totalElements,
		int totalPages,
		BigDecimal totalAmount,
		long totalTransactionCount

) {
}
