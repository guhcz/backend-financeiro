package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

	Optional<Expense> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByTransactionMethodId(Long transactionMethodId);

	boolean existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(
			Long recurringExpenseId, Integer year, Integer month);

	/**
	 * Expense has @SQLRestriction("active = true"), so a plain derived existsByRecurringExpenseId
	 * would be blind to soft-deleted rows — but their physical row still exists and still holds
	 * the FK to recurring_expenses (ON DELETE RESTRICT). A rule whose only expense was soft-deleted
	 * would look history-free to a restricted check, take the hard-delete path, and fail with a
	 * DataIntegrityViolationException. This native query bypasses the restriction to match what the
	 * FK actually sees.
	 */
	@Query(value = "SELECT EXISTS(SELECT 1 FROM expenses WHERE recurring_expense_id = :recurringExpenseId)", nativeQuery = true)
	boolean existsIncludingInactiveByRecurringExpenseId(@Param("recurringExpenseId") Long recurringExpenseId);

	/**
	 * Occurrences pre-generated ahead of time (see app.recurring-expense.lookahead-months) that
	 * still need to reflect a rule edit, or be removed when the rule ends — the caller picks the
	 * boundary date (today for a direct rule edit/end, or the clicked occurrence's date for a
	 * THIS_AND_FUTURE scope from Movimentações).
	 */
	List<Expense> findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(Long recurringExpenseId, LocalDate date);

	/**
	 * Powers the dashboard's "due soon" widget. Occurrences are now pre-generated up to
	 * lookahead-months ahead (see RecurringExpenseGenerationService), so "what's due soon" reads
	 * from the already-generated Expense rows themselves rather than from the rule's
	 * nextGenerationDate — after the first generation pass that field points past the whole
	 * lookahead horizon, not at the next upcoming charge. The ACTIVE + dueDay IS NOT NULL filters
	 * mirror the old nextGenerationDate-based query: a paused/ended rule's already-generated future
	 * rows still exist but shouldn't be surfaced as "coming up," and a null-dueDay rule has no
	 * meaningful "due date" to show here.
	 */
	@Query("""
			SELECT COUNT(e) FROM Expense e
			WHERE e.user.id = :userId AND e.recurringExpense IS NOT NULL
			AND e.recurringExpense.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND e.recurringExpense.dueDay IS NOT NULL AND e.expenseDate BETWEEN :from AND :to
			""")
	long countRecurringDueBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
			WHERE e.user.id = :userId AND e.recurringExpense IS NOT NULL
			AND e.recurringExpense.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND e.recurringExpense.dueDay IS NOT NULL AND e.expenseDate = :date
			""")
	BigDecimal sumRecurringAmountDueOn(@Param("userId") Long userId, @Param("date") LocalDate date);

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

	/**
	 * Only CARD + CREDIT expenses consume card planning -- debit purchases on the same card
	 * settle immediately and must not count against the credit invoice budget (see
	 * CompetenceResolver / MonthlyCardPlanningService).
	 */
	@Query("""
			SELECT e.transactionMethod.id AS transactionMethodId, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.billingYear = :year AND e.billingMonth = :month
			AND e.cardTransactionMode = com.personal.backend_financeiro.enums.CardTransactionMode.CREDIT
			GROUP BY e.transactionMethod.id
			""")
	List<TransactionMethodTotalProjection> sumAmountGroupedByTransactionMethod(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

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
