package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

	Optional<Income> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(
			Long recurringIncomeId, Integer year, Integer month);

	/**
	 * Income has @SQLRestriction("active = true"), so a plain derived existsByRecurringIncomeId
	 * would be blind to soft-deleted rows — but their physical row still exists and still holds
	 * the FK to recurring_incomes (ON DELETE RESTRICT). A rule whose only income was soft-deleted
	 * would look history-free to a restricted check, take the hard-delete path, and fail with a
	 * DataIntegrityViolationException. This native query bypasses the restriction to match what
	 * the FK actually sees.
	 */
	@Query(value = "SELECT EXISTS(SELECT 1 FROM incomes WHERE recurring_income_id = :recurringIncomeId)", nativeQuery = true)
	boolean existsIncludingInactiveByRecurringIncomeId(@Param("recurringIncomeId") Long recurringIncomeId);

	/**
	 * Occurrences pre-generated ahead of time (see app.recurring-income.lookahead-months) that
	 * still need to reflect a rule edit, or be removed when the rule ends — the caller picks the
	 * boundary date (today for a direct rule edit/end, or the clicked occurrence's date for a
	 * THIS_AND_FUTURE scope from Movimentações).
	 */
	List<Income> findByRecurringIncomeIdAndIncomeDateGreaterThanEqual(Long recurringIncomeId, LocalDate date);

	@Query("""
			SELECT COALESCE(SUM(i.amount), 0) FROM Income i
			WHERE i.user.id = :userId AND i.incomeDate BETWEEN :start AND :end
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

}
