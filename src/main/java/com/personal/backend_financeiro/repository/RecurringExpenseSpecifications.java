package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Each filter method returns {@link Specification#unrestricted()} when the filter does not
 * apply, matching the convention used in {@link ExpenseSpecifications}.
 */
public final class RecurringExpenseSpecifications {

	private RecurringExpenseSpecifications() {
	}

	public static Specification<RecurringExpense> belongsToUser(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
	}

	public static Specification<RecurringExpense> matchesActiveFilter(Boolean active) {
		if (active == null) {
			return Specification.unrestricted();
		}
		return active
				? (root, query, cb) -> cb.equal(root.get("status"), RecurrenceStatus.ACTIVE)
				: (root, query, cb) -> cb.notEqual(root.get("status"), RecurrenceStatus.ACTIVE);
	}

	public static Specification<RecurringExpense> hasCategory(Long categoryId) {
		if (categoryId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<RecurringExpense> descriptionContains(String text) {
		if (text == null || text.isBlank()) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%");
	}

	/**
	 * Matches rules whose period (startDate–endDate) overlaps the given month, i.e.
	 * startDate <= monthEnd AND (endDate IS NULL OR endDate >= monthStart).
	 */
	public static Specification<RecurringExpense> activeDuring(LocalDate monthStart, LocalDate monthEnd) {
		if (monthStart == null || monthEnd == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.and(
				cb.lessThanOrEqualTo(root.get("startDate"), monthEnd),
				cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), monthStart)));
	}

}
