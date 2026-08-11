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

	boolean existsByRecurringExpenseIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(
			Long recurringExpenseId, Integer year, Integer month);

	boolean existsByRecurringExpenseId(Long recurringExpenseId);

	@Query("""
			SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
			WHERE e.user.id = :userId AND e.expenseDate BETWEEN :start AND :end
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

	@Query("""
			SELECT e.category.id AS categoryId, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.expenseDate BETWEEN :start AND :end
			GROUP BY e.category.id
			""")
	List<CategoryTotalProjection> sumAmountGroupedByCategory(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

	@Query("""
			SELECT e.expenseDate AS date, COALESCE(SUM(e.amount), 0) AS total
			FROM Expense e
			WHERE e.user.id = :userId AND e.expenseDate BETWEEN :start AND :end
			GROUP BY e.expenseDate
			ORDER BY e.expenseDate ASC
			""")
	List<DateTotalProjection> sumAmountGroupedByDate(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

	List<Expense> findTop5ByUserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
			Long userId, LocalDate start, LocalDate end);

}
