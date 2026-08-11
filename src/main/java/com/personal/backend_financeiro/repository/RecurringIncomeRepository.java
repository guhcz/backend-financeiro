package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.RecurringIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringIncomeRepository extends JpaRepository<RecurringIncome, Long>, JpaSpecificationExecutor<RecurringIncome> {

	Optional<RecurringIncome> findByIdAndUserId(Long id, Long userId);

	boolean existsByCategoryId(Long categoryId);

	@Query("""
			SELECT r.id FROM RecurringIncome r
			WHERE r.status = com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE
			AND r.nextGenerationDate <= :today
			AND (r.endDate IS NULL OR r.endDate >= r.nextGenerationDate)
			""")
	List<Long> findEligibleRuleIds(@Param("today") LocalDate today);

}
