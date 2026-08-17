package com.personal.backend_financeiro.repository;

import java.math.BigDecimal;

public interface TransactionMethodTotalProjection {

	Long getTransactionMethodId();

	BigDecimal getTotal();

}
