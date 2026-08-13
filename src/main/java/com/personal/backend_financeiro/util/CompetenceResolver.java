package com.personal.backend_financeiro.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Single source of truth for "which month should this transaction impact financially": always
 * the month after its own date (expenseDate/incomeDate), regardless of payment method. Reused by
 * expense persistence, recurring expense generation, category planning, card planning, monthly
 * limit, the unified transactions view and the dashboard so the rule is never duplicated or
 * allowed to diverge between screens.
 */
public final class CompetenceResolver {

	private CompetenceResolver() {
	}

	public static YearMonth resolve(LocalDate date) {
		return YearMonth.from(date).plusMonths(1);
	}

}
