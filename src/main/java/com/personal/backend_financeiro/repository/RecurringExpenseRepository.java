package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long>, JpaSpecificationExecutor<RecurringExpense> {

	Optional<RecurringExpense> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	long countByUserIdAndStatus(Long userId, RecurrenceStatus status);

	@Query("""
			SELECT r.id FROM RecurringExpense r
			WHERE r.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND r.nextGenerationDate <= :today
			AND (r.endDate IS NULL OR r.endDate >= r.nextGenerationDate)
			""")
	List<Long> findEligibleRuleIds(@Param("today") LocalDate today);

	@Query("""
			SELECT COUNT(r) FROM RecurringExpense r
			WHERE r.user.id = :userId AND r.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND r.dueDay IS NOT NULL AND r.nextGenerationDate BETWEEN :from AND :to
			""")
	long countDueBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT COALESCE(SUM(r.amount), 0) FROM RecurringExpense r
			WHERE r.user.id = :userId AND r.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND r.dueDay IS NOT NULL AND r.nextGenerationDate = :date
			""")
	BigDecimal sumAmountDueOn(@Param("userId") Long userId, @Param("date") LocalDate date);

}
