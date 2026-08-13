package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

	Optional<Expense> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByCreditCardId(Long creditCardId);

	boolean existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(
			Long recurringExpenseId, Integer year, Integer month);

	boolean existsByRecurringExpenseId(Long recurringExpenseId);

	/**
	 * All "sum for a month" queries below filter by billingYear/billingMonth (financial
	 * competence), not expenseDate, so category planning, card planning, monthly limit and the
	 * dashboard all agree on which month an expense counts towards (the month after its own
	 * date) instead of the purchase date. See CompetenceResolver.
	 */
	@Query("""
			SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
			WHERE e.user.id = :userId AND e.billingYear = :year AND e.billingMonth = :month
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

	@Query("""
			SELECT e.category.id AS categoryId, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.billingYear = :year AND e.billingMonth = :month
			GROUP BY e.category.id
			""")
	List<CategoryTotalProjection> sumAmountGroupedByCategory(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

	@Query("""
			SELECT e.creditCard.id AS creditCardId, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.billingYear = :year AND e.billingMonth = :month AND e.creditCard IS NOT NULL
			GROUP BY e.creditCard.id
			""")
	List<CreditCardTotalProjection> sumAmountGroupedByCreditCard(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

	@Query("""
			SELECT e.expenseDate AS date, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.billingYear = :year AND e.billingMonth = :month
			GROUP BY e.expenseDate
			ORDER BY e.expenseDate ASC
			""")
	List<DateTotalProjection> sumAmountGroupedByDate(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

	List<Expense> findTop5ByUserIdAndBillingYearAndBillingMonthOrderByExpenseDateDescCreatedAtDesc(
			Long userId, Integer billingYear, Integer billingMonth);

}
