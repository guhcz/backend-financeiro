package com.personal.backend_financeiro.util;

import com.personal.backend_financeiro.exception.InvalidRequestException;

import java.time.LocalDate;

public final class DateRangeUtils {

	private DateRangeUtils() {
	}

	public static void assertValid(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new InvalidRequestException("startDate não pode ser posterior a endDate");
		}
	}

}
