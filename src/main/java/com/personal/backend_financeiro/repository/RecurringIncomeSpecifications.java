package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Each filter method returns {@link Specification#unrestricted()} when the filter does not
 * apply. Since Spring Data JPA 4.0, {@code and}/{@code or}/{@code where} reject {@code null}
 * outright (throws {@link IllegalArgumentException}) — {@code unrestricted()} is the
 * supported no-op replacement, so callers can chain every filter unconditionally regardless
 * of which ones were actually provided.
 */
public final class RecurringIncomeSpecifications {

	private RecurringIncomeSpecifications() {
	}

	public static Specification<RecurringIncome> belongsToUser(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
	}

	public static Specification<RecurringIncome> matchesActiveFilter(Boolean active) {
		if (active == null) {
			return Specification.unrestricted();
		}
		return active
				? (root, query, cb) -> cb.equal(root.get("status"), RecurrenceStatus.ACTIVE)
				: (root, query, cb) -> cb.notEqual(root.get("status"), RecurrenceStatus.ACTIVE);
	}

	public static Specification<RecurringIncome> hasCategory(Long categoryId) {
		if (categoryId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<RecurringIncome> descriptionContains(String text) {
		if (text == null || text.isBlank()) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%");
	}

	public static Specification<RecurringIncome> activeDuring(LocalDate monthStart, LocalDate monthEnd) {
		if (monthStart == null || monthEnd == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.and(
				cb.lessThanOrEqualTo(root.get("startDate"), monthEnd),
				cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), monthStart)));
	}

}
