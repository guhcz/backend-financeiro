package com.personal.backend_financeiro.util;

import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Single source of truth for "which month should this transaction impact financially" and for
 * the cardTransactionMode/transactionMethod consistency rule. Reused by expense persistence,
 * recurring expense generation, category planning, card planning, monthly limit, the unified
 * transactions view and the dashboard so neither rule is ever duplicated or allowed to diverge
 * between screens.
 */
public final class CompetenceResolver {

	private CompetenceResolver() {
	}

	/**
	 * CARD + CREDIT keeps the invoice behavior (month after the purchase date). Every other
	 * combination -- CARD + DEBIT included -- counts towards its own date's month, since debit
	 * and non-card methods settle immediately instead of going through a monthly invoice.
	 */
	public static YearMonth resolve(LocalDate date, TransactionMethodType type, CardTransactionMode cardMode) {
		if (type == TransactionMethodType.CARD && cardMode == CardTransactionMode.CREDIT) {
			return YearMonth.from(date).plusMonths(1);
		}
		return YearMonth.from(date);
	}

	public static void validateCardTransactionMode(TransactionMethod method, CardTransactionMode cardMode) {
		boolean isCard = method.getType() == TransactionMethodType.CARD;
		if (isCard && cardMode == null) {
			throw new InvalidRequestException("cardTransactionMode is required when transactionMethod type is CARD");
		}
		if (!isCard && cardMode != null) {
			throw new InvalidRequestException("cardTransactionMode must be null when transactionMethod type is not CARD");
		}
	}

}
