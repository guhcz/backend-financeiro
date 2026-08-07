package com.personal.backend_financeiro.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceDateCalculatorTest {

	@Test
	void resolveOccurrenceDate_clampsToLastDayOfMonth_whenDueDay31InNonLeapFebruary() {
		LocalDate result = RecurrenceDateCalculator.resolveOccurrenceDate(2027, 2, 31);

		assertThat(result).isEqualTo(LocalDate.of(2027, 2, 28));
	}

	@Test
	void resolveOccurrenceDate_clampsToLastDayOfMonth_whenDueDay31InLeapFebruary() {
		LocalDate result = RecurrenceDateCalculator.resolveOccurrenceDate(2028, 2, 31);

		assertThat(result).isEqualTo(LocalDate.of(2028, 2, 29));
	}

	@Test
	void resolveOccurrenceDate_clampsToLastDayOfMonth_whenDueDay31InThirtyDayMonth() {
		LocalDate result = RecurrenceDateCalculator.resolveOccurrenceDate(2026, 4, 31);

		assertThat(result).isEqualTo(LocalDate.of(2026, 4, 30));
	}

	@Test
	void resolveOccurrenceDate_returnsExactDay_whenDueDayWithinMonthRange() {
		LocalDate result = RecurrenceDateCalculator.resolveOccurrenceDate(2026, 8, 10);

		assertThat(result).isEqualTo(LocalDate.of(2026, 8, 10));
	}

	@Test
	void nextMonthOccurrence_crossesYearBoundary_whenCurrentMonthIsDecember() {
		LocalDate result = RecurrenceDateCalculator.nextMonthOccurrence(LocalDate.of(2026, 12, 10), 10);

		assertThat(result).isEqualTo(LocalDate.of(2027, 1, 10));
	}

	@Test
	void nextMonthOccurrence_clampsToLastDayOfMonth_whenNextMonthIsShorter() {
		LocalDate result = RecurrenceDateCalculator.nextMonthOccurrence(LocalDate.of(2026, 1, 31), 31);

		assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void resolveNextGenerationDateFrom_returnsThisMonth_whenDueDayNotYetReached() {
		LocalDate result = RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.of(2026, 8, 5), 10);

		assertThat(result).isEqualTo(LocalDate.of(2026, 8, 10));
	}

	@Test
	void resolveNextGenerationDateFrom_returnsToday_whenDueDayIsToday() {
		LocalDate result = RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.of(2026, 8, 10), 10);

		assertThat(result).isEqualTo(LocalDate.of(2026, 8, 10));
	}

	@Test
	void resolveNextGenerationDateFrom_returnsNextMonth_whenDueDayAlreadyPassedThisMonth() {
		LocalDate result = RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.of(2026, 8, 15), 10);

		assertThat(result).isEqualTo(LocalDate.of(2026, 9, 10));
	}

	@Test
	void resolveOccurrenceDate_defaultsToFirstDayOfMonth_whenDueDayIsNull() {
		LocalDate result = RecurrenceDateCalculator.resolveOccurrenceDate(2026, 8, null);

		assertThat(result).isEqualTo(LocalDate.of(2026, 8, 1));
	}

	@Test
	void nextMonthOccurrence_defaultsToFirstDayOfNextMonth_whenDueDayIsNull() {
		LocalDate result = RecurrenceDateCalculator.nextMonthOccurrence(LocalDate.of(2026, 8, 20), null);

		assertThat(result).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	@Test
	void resolveNextGenerationDateFrom_returnsNextMonthFirstDay_whenDueDayIsNullAndTodayIsNotTheFirst() {
		LocalDate result = RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.of(2026, 8, 15), null);

		assertThat(result).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	@Test
	void resolveNextGenerationDateFrom_returnsToday_whenDueDayIsNullAndTodayIsTheFirst() {
		LocalDate result = RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.of(2026, 8, 1), null);

		assertThat(result).isEqualTo(LocalDate.of(2026, 8, 1));
	}

}
