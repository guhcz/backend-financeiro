package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.MonthlyLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyLimitRepository extends JpaRepository<MonthlyLimit, Long> {

	List<MonthlyLimit> findByUserId(Long userId);

	Optional<MonthlyLimit> findByIdAndUserId(Long id, Long userId);

	Optional<MonthlyLimit> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

}
