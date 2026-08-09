package com.personal.backend_financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DateTotalProjection {

	LocalDate getDate();

	BigDecimal getTotal();

}
