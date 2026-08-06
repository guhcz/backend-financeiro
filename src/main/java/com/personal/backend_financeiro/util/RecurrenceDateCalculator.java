package com.personal.backend_financeiro.util;

import java.time.LocalDate;
import java.time.YearMonth;

public final class RecurrenceDateCalculator {

	private RecurrenceDateCalculator() {
	}

	public static LocalDate resolveOccurrenceDate(int year, int month, int dueDay) {
		YearMonth yearMonth = YearMonth.of(year, month);
		int day = Math.min(dueDay, yearMonth.lengthOfMonth());
		return yearMonth.atDay(day);
	}

	public static LocalDate resolveOccurrenceDate(LocalDate anchor, int dueDay) {
		return resolveOccurrenceDate(anchor.getYear(), anchor.getMonthValue(), dueDay);
	}

	public static LocalDate nextMonthOccurrence(LocalDate current, int dueDay) {
		YearMonth nextMonth = YearMonth.from(current).plusMonths(1);
		return resolveOccurrenceDate(nextMonth.getYear(), nextMonth.getMonthValue(), dueDay);
	}

	public static LocalDate resolveNextGenerationDateFrom(LocalDate today, int dueDay) {
		LocalDate candidate = resolveOccurrenceDate(today, dueDay);
		return candidate.isBefore(today) ? nextMonthOccurrence(today, dueDay) : candidate;
	}

}
