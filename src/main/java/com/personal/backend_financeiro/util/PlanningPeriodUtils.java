package com.personal.backend_financeiro.util;

import com.personal.backend_financeiro.exception.InvalidRequestException;

import java.time.LocalDate;

public final class PlanningPeriodUtils {

	private PlanningPeriodUtils() {
	}

	public static void assertValid(Integer month, Integer year) {
		if (month == null || month < 1 || month > 12) {
			throw new InvalidRequestException("month must be between 1 and 12");
		}
		if (year == null || year < 2000) {
			throw new InvalidRequestException("year must be >= 2000");
		}
	}

	public static LocalDate firstDayOf(Integer year, Integer month) {
		return LocalDate.of(year, month, 1);
	}

	public static LocalDate lastDayOf(Integer year, Integer month) {
		LocalDate first = firstDayOf(year, month);
		return first.withDayOfMonth(first.lengthOfMonth());
	}

}
