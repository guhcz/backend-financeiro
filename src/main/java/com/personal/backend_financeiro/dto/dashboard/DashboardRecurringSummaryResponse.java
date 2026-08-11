package com.personal.backend_financeiro.dto.dashboard;

import java.math.BigDecimal;

public record DashboardRecurringSummaryResponse(

		long activeCount,
		long dueInNext7DaysCount,
		BigDecimal dueTodayAmount

) {
}
