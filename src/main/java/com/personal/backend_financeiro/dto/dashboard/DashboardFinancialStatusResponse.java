package com.personal.backend_financeiro.dto.dashboard;

import com.personal.backend_financeiro.enums.FinancialStatusType;

public record DashboardFinancialStatusResponse(

		FinancialStatusType type,
		String message

) {
}
