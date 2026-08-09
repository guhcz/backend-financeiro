package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.MonthlyPlanning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface MonthlyPlanningRepository extends JpaRepository<MonthlyPlanning, Long> {

	Optional<MonthlyPlanning> findByIdAndUserId(Long id, Long userId);

	Optional<MonthlyPlanning> findByUserIdAndCategoryIdAndMonthAndYear(
			Long userId, Long categoryId, Integer month, Integer year);

	Page<MonthlyPlanning> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year, Pageable pageable);

	boolean existsByCategoryId(Long categoryId);

	@Query("""
			SELECT COALESCE(SUM(p.amount), 0) FROM MonthlyPlanning p
			WHERE p.user.id = :userId AND p.month = :month AND p.year = :year
			""")
	BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);

}
