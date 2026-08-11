package com.personal.backend_financeiro.dto.transaction;

/**
 * Presentation-only distinction between the two underlying entities (Expense, Income) in the
 * unified /transactions read view. Never persisted — there is no TransactionEntity.
 */
public enum TransactionType {

	EXPENSE,
	INCOME

}
