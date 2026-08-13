package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.creditcard.CreditCardRequest;
import com.personal.backend_financeiro.dto.creditcard.CreditCardResponse;
import com.personal.backend_financeiro.entity.CreditCard;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceInUseException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CreditCardMapper;
import com.personal.backend_financeiro.repository.CreditCardRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyCardPlanningRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditCardService {

	private final CreditCardRepository creditCardRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseRepository recurringExpenseRepository;
	private final MonthlyCardPlanningRepository monthlyCardPlanningRepository;
	private final CreditCardMapper creditCardMapper;

	@Transactional
	public CreditCardResponse create(Long userId, CreditCardRequest request) {
		if (creditCardRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
			throw new DuplicateResourceException("Credit card already exists: " + request.name());
		}

		CreditCard creditCard = creditCardMapper.toEntity(request);
		creditCard.setUser(userRepository.getReferenceById(userId));

		CreditCard saved = creditCardRepository.save(creditCard);
		return creditCardMapper.toResponse(saved);
	}

	public List<CreditCardResponse> listAllByUser(Long userId) {
		return creditCardRepository.findByUserId(userId).stream()
				.map(creditCardMapper::toResponse)
				.toList();
	}

	public Page<CreditCardResponse> listByUser(Long userId, Pageable pageable) {
		return creditCardRepository.findByUserId(userId, pageable).map(creditCardMapper::toResponse);
	}

	@Transactional
	public CreditCardResponse update(Long userId, Long creditCardId, CreditCardRequest request) {
		CreditCard creditCard = creditCardRepository.findByIdAndUserId(creditCardId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Credit card not found: " + creditCardId));

		if (creditCardRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, request.name(), creditCardId)) {
			throw new DuplicateResourceException("Credit card already exists: " + request.name());
		}

		creditCardMapper.updateEntityFromRequest(request, creditCard);
		return creditCardMapper.toResponse(creditCard);
	}

	@Transactional
	public void delete(Long userId, Long creditCardId) {
		CreditCard creditCard = creditCardRepository.findByIdAndUserId(creditCardId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Credit card not found: " + creditCardId));

		/*
		 * Soft delete (@SQLDelete -> UPDATE) never triggers the FK's ON DELETE RESTRICT, since no
		 * physical DELETE is issued. Without this check, an expense would keep pointing at a
		 * credit card that @SQLRestriction makes invisible to every query, and loading that
		 * expense's lazy credit card association later throws EntityNotFoundException (500)
		 * instead of failing safely here. Mirrors CategoryService.delete.
		 */
		if (expenseRepository.existsByCreditCardId(creditCardId)) {
			throw new ResourceInUseException("Credit card has expenses and cannot be deleted: " + creditCardId);
		}
		if (recurringExpenseRepository.existsByCreditCardId(creditCardId)) {
			throw new ResourceInUseException("Credit card has recurring expenses and cannot be deleted: " + creditCardId);
		}
		if (monthlyCardPlanningRepository.existsByCreditCardId(creditCardId)) {
			throw new ResourceInUseException("Credit card has monthly plannings and cannot be deleted: " + creditCardId);
		}

		creditCardRepository.delete(creditCard);
	}

}
