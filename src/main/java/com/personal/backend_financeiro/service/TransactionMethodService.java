package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodCardDetailsRequest;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodCreateRequest;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodUpdateRequest;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceInUseException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.TransactionMethodMapper;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyCardPlanningRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.TransactionMethodRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The single visual "forma de pagamento" registration -- a card (Nubank, Santander...) is just a
 * TransactionMethod of type CARD with an attached card-details sub-object; there is no separate
 * "Cartões" registration for the user to create or see.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionMethodService {

	private final TransactionMethodRepository transactionMethodRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseRepository recurringExpenseRepository;
	private final MonthlyCardPlanningRepository monthlyCardPlanningRepository;
	private final TransactionMethodMapper transactionMethodMapper;

	@Transactional
	public TransactionMethodResponse create(Long userId, TransactionMethodCreateRequest request) {
		if (transactionMethodRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
			throw new DuplicateResourceException("Transaction method already exists: " + request.name());
		}
		assertCardConsistency(request.type(), request.card());

		TransactionMethod transactionMethod = new TransactionMethod();
		transactionMethod.setUser(userRepository.getReferenceById(userId));
		transactionMethod.setName(request.name());
		transactionMethod.setType(request.type());
		if (request.type() == TransactionMethodType.CARD) {
			transactionMethod.setCreditCard(transactionMethodMapper.toCreditCardEntity(request.card()));
		}

		TransactionMethod saved = transactionMethodRepository.save(transactionMethod);
		return transactionMethodMapper.toResponse(saved);
	}

	public List<TransactionMethodResponse> listAllByUser(Long userId) {
		return transactionMethodRepository.findByUserId(userId).stream()
				.map(transactionMethodMapper::toResponse)
				.toList();
	}

	public Page<TransactionMethodResponse> listByUser(Long userId, Pageable pageable) {
		return transactionMethodRepository.findByUserId(userId, pageable).map(transactionMethodMapper::toResponse);
	}

	@Transactional
	public TransactionMethodResponse update(Long userId, Long transactionMethodId, TransactionMethodUpdateRequest request) {
		TransactionMethod transactionMethod = findOwned(userId, transactionMethodId);

		if (transactionMethodRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, request.name(), transactionMethodId)) {
			throw new DuplicateResourceException("Transaction method already exists: " + request.name());
		}
		assertCardConsistency(transactionMethod.getType(), request.card());

		transactionMethod.setName(request.name());
		if (transactionMethod.getType() == TransactionMethodType.CARD) {
			transactionMethodMapper.updateCreditCardFromRequest(request.card(), transactionMethod.getCreditCard());
		}

		return transactionMethodMapper.toResponse(transactionMethod);
	}

	@Transactional
	public void delete(Long userId, Long transactionMethodId) {
		TransactionMethod transactionMethod = findOwned(userId, transactionMethodId);

		/*
		 * Soft delete (@SQLDelete -> UPDATE) never triggers the FK's ON DELETE RESTRICT, since no
		 * physical DELETE is issued. Without this check, an expense would keep pointing at a
		 * transaction method that @SQLRestriction makes invisible to every query, and loading
		 * that expense's lazy association later throws EntityNotFoundException (500) instead of
		 * failing safely here. Mirrors CategoryService.delete.
		 */
		if (expenseRepository.existsByTransactionMethodId(transactionMethodId)) {
			throw new ResourceInUseException("Transaction method has expenses and cannot be deleted: " + transactionMethodId);
		}
		if (recurringExpenseRepository.existsByTransactionMethodId(transactionMethodId)) {
			throw new ResourceInUseException("Transaction method has recurring expenses and cannot be deleted: " + transactionMethodId);
		}
		if (monthlyCardPlanningRepository.existsByTransactionMethodId(transactionMethodId)) {
			throw new ResourceInUseException("Transaction method has monthly plannings and cannot be deleted: " + transactionMethodId);
		}

		transactionMethodRepository.delete(transactionMethod);
	}

	private void assertCardConsistency(TransactionMethodType type, TransactionMethodCardDetailsRequest card) {
		if (type == TransactionMethodType.CARD && card == null) {
			throw new InvalidRequestException("card is required when type is CARD");
		}
		if (type != TransactionMethodType.CARD && card != null) {
			throw new InvalidRequestException("card must not be provided when type is not CARD");
		}
	}

	private TransactionMethod findOwned(Long userId, Long transactionMethodId) {
		return transactionMethodRepository.findByIdAndUserId(transactionMethodId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction method not found: " + transactionMethodId));
	}

}
