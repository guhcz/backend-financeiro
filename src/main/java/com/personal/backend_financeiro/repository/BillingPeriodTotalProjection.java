package com.personal.backend_financeiro.repository;

import java.math.BigDecimal;

public interface BillingPeriodTotalProjection {

	Integer getBillingYear();

	Integer getBillingMonth();

	BigDecimal getTotal();

}
