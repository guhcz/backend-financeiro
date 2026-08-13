package com.personal.backend_financeiro.repository;

import java.math.BigDecimal;

public interface CreditCardTotalProjection {

	Long getCreditCardId();

	BigDecimal getTotal();

}
