package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

	Optional<Income> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByRecurringIncomeIdAndRecurrenceReferenceYearAndRecurrenceReferenceMonth(
			Long recurringIncomeId, Integer year, Integer month);

	boolean existsByRecurringIncomeId(Long recurringIncomeId);

	@Query("""
			SELECT COALESCE(SUM(i.amount), 0) FROM Income i
			WHERE i.user.id = :userId AND i.incomeDate BETWEEN :start AND :end
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

}
