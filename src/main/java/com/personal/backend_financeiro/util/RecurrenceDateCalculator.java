package com.personal.backend_financeiro.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A null dueDay means the recurring expense has no specific due date (e.g. money set aside
 * for something without a fixed deadline) — it still generates once a month, defaulting to
 * the 1st, but the day is never persisted back onto the rule.
 */
public final class RecurrenceDateCalculator {

	private static final int DEFAULT_DUE_DAY = 1;

	private RecurrenceDateCalculator() {
	}

	public static LocalDate resolveOccurrenceDate(int year, int month, Integer dueDay) {
		YearMonth yearMonth = YearMonth.of(year, month);
		int day = Math.min(effectiveDay(dueDay), yearMonth.lengthOfMonth());
		return yearMonth.atDay(day);
	}

	public static LocalDate resolveOccurrenceDate(LocalDate anchor, Integer dueDay) {
		return resolveOccurrenceDate(anchor.getYear(), anchor.getMonthValue(), dueDay);
	}

	public static LocalDate nextMonthOccurrence(LocalDate current, Integer dueDay) {
		YearMonth nextMonth = YearMonth.from(current).plusMonths(1);
		return resolveOccurrenceDate(nextMonth.getYear(), nextMonth.getMonthValue(), dueDay);
	}

	public static LocalDate resolveNextGenerationDateFrom(LocalDate today, Integer dueDay) {
		LocalDate candidate = resolveOccurrenceDate(today, dueDay);
		return candidate.isBefore(today) ? nextMonthOccurrence(today, dueDay) : candidate;
	}

	private static int effectiveDay(Integer dueDay) {
		return dueDay != null ? dueDay : DEFAULT_DUE_DAY;
	}

}
