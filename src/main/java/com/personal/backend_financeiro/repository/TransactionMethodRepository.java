package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.TransactionMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionMethodRepository extends JpaRepository<TransactionMethod, Long> {

	List<TransactionMethod> findByUserId(Long userId);

	Page<TransactionMethod> findByUserId(Long userId, Pageable pageable);

	Optional<TransactionMethod> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

}
