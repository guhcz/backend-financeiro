package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.MonthlyCardPlanning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface MonthlyCardPlanningRepository extends JpaRepository<MonthlyCardPlanning, Long> {

	Optional<MonthlyCardPlanning> findByIdAndUserId(Long id, Long userId);

	Optional<MonthlyCardPlanning> findByUserIdAndTransactionMethodIdAndMonthAndYear(
			Long userId, Long transactionMethodId, Integer month, Integer year);

	Page<MonthlyCardPlanning> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year, Pageable pageable);

	boolean existsByTransactionMethodId(Long transactionMethodId);

	@Query("""
			SELECT COALESCE(SUM(p.amount), 0) FROM MonthlyCardPlanning p
			WHERE p.user.id = :userId AND p.month = :month AND p.year = :year
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);

}
