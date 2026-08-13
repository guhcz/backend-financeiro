package com.personal.backend_financeiro.repository;

import com.personal.backend_financeiro.entity.CreditCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

	List<CreditCard> findByUserId(Long userId);

	Page<CreditCard> findByUserId(Long userId, Pageable pageable);

	Optional<CreditCard> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

}
