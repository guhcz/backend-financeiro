package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.TransactionMethodType;

import java.math.BigDecimal;

public interface PaymentMethodTotalProjection {

	Long getTransactionMethodId();

	String getName();

	TransactionMethodType getMethodType();

	CardTransactionMode getCardTransactionMode();

	BigDecimal getTotal();

	Long getTransactionCount();

}
