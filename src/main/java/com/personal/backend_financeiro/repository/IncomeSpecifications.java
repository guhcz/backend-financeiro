package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.enums.ReceiptMethod;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Each filter method returns {@link Specification#unrestricted()} when the filter does not
 * apply. Since Spring Data JPA 4.0, {@code and}/{@code or}/{@code where} reject {@code null}
 * outright (throws {@link IllegalArgumentException}) — {@code unrestricted()} is the
 * supported no-op replacement, so callers can chain every filter unconditionally regardless
 * of which ones were actually provided.
 */
public final class IncomeSpecifications {

	private IncomeSpecifications() {
	}

	public static Specification<Income> belongsToUser(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
	}

	public static Specification<Income> hasCategory(Long categoryId) {
		if (categoryId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<Income> hasReceiptMethod(ReceiptMethod receiptMethod) {
		if (receiptMethod == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("receiptMethod"), receiptMethod);
	}

	public static Specification<Income> incomeDateFrom(LocalDate from) {
		if (from == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("incomeDate"), from);
	}

	public static Specification<Income> incomeDateTo(LocalDate to) {
		if (to == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("incomeDate"), to);
	}

	public static Specification<Income> descriptionContains(String text) {
		if (text == null || text.isBlank()) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%");
	}

	public static Specification<Income> isRecurring(Boolean recurring) {
		if (recurring == null) {
			return Specification.unrestricted();
		}
		return recurring
				? (root, query, cb) -> cb.isNotNull(root.get("recurringIncome"))
				: (root, query, cb) -> cb.isNull(root.get("recurringIncome"));
	}

}
