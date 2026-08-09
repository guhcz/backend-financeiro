package com.personal.backend_financeiro.repository;

import java.math.BigDecimal;

public interface CategoryTotalProjection {

	Long getCategoryId();

	BigDecimal getTotal();

}
