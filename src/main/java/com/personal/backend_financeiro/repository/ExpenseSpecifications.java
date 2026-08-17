package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Each filter method returns {@link Specification#unrestricted()} when the filter does not
 * apply. Since Spring Data JPA 4.0, {@code and}/{@code or}/{@code where} reject {@code null}
 * outright (throws {@link IllegalArgumentException}) — {@code unrestricted()} is the
 * supported no-op replacement, so callers can chain every filter unconditionally regardless
 * of which ones were actually provided.
 */
public final class ExpenseSpecifications {

	private ExpenseSpecifications() {
	}

	public static Specification<Expense> belongsToUser(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
	}

	public static Specification<Expense> hasCategory(Long categoryId) {
		if (categoryId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<Expense> hasTransactionMethod(Long transactionMethodId) {
		if (transactionMethodId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("transactionMethod").get("id"), transactionMethodId);
	}

	public static Specification<Expense> expenseDateFrom(LocalDate from) {
		if (from == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
	}

	public static Specification<Expense> expenseDateTo(LocalDate to) {
		if (to == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
	}

	public static Specification<Expense> descriptionContains(String text) {
		if (text == null || text.isBlank()) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%");
	}

	public static Specification<Expense> isRecurring(Boolean recurring) {
		if (recurring == null) {
			return Specification.unrestricted();
		}
		return recurring
				? (root, query, cb) -> cb.isNotNull(root.get("recurringExpense"))
				: (root, query, cb) -> cb.isNull(root.get("recurringExpense"));
	}

}
