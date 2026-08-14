package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningRequest;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningResponse;
import com.personal.backend_financeiro.entity.MonthlyCardPlanning;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.TransactionMethodType;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.MonthlyCardPlanningMapper;
import com.personal.backend_financeiro.mapper.TransactionMethodMapper;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyCardPlanningRepository;
import com.personal.backend_financeiro.repository.TransactionMethodRepository;
import com.personal.backend_financeiro.repository.TransactionMethodTotalProjection;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyCardPlanningService {

	private final MonthlyCardPlanningRepository monthlyCardPlanningRepository;
	private final TransactionMethodRepository transactionMethodRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final MonthlyCardPlanningMapper monthlyCardPlanningMapper;
	private final TransactionMethodMapper transactionMethodMapper;

	@Transactional
	public MonthlyCardPlanningResponse create(Long userId, MonthlyCardPlanningRequest request) {
		TransactionMethod transactionMethod = findOwnedCardTransactionMethod(userId, request.transactionMethodId());
		assertPeriodAvailable(userId, request, null);

		MonthlyCardPlanning planning = monthlyCardPlanningMapper.toEntity(request);
		planning.setUser(userRepository.getReferenceById(userId));
		planning.setTransactionMethod(transactionMethod);

		MonthlyCardPlanning saved = monthlyCardPlanningRepository.save(planning);
		return monthlyCardPlanningMapper.toResponse(saved);
	}

	public MonthlyCardPlanningResponse getOne(Long userId, Long planningId) {
		MonthlyCardPlanning planning = findOwnedPlanning(userId, planningId);
		return monthlyCardPlanningMapper.toResponse(planning);
	}

	@Transactional
	public MonthlyCardPlanningResponse update(Long userId, Long planningId, MonthlyCardPlanningRequest request) {
		MonthlyCardPlanning planning = findOwnedPlanning(userId, planningId);
		TransactionMethod transactionMethod = findOwnedCardTransactionMethod(userId, request.transactionMethodId());
		assertPeriodAvailable(userId, request, planningId);

		monthlyCardPlanningMapper.updateEntityFromRequest(request, planning);
		planning.setTransactionMethod(transactionMethod);

		return monthlyCardPlanningMapper.toResponse(planning);
	}

	@Transactional
	public void delete(Long userId, Long planningId) {
		MonthlyCardPlanning planning = findOwnedPlanning(userId, planningId);
		monthlyCardPlanningRepository.delete(planning);
	}

	public Page<MonthlyCardPlanningItemResponse> list(Long userId, Integer month, Integer year, Pageable pageable) {
		PlanningPeriodUtils.assertValid(month, year);

		Page<MonthlyCardPlanning> page = monthlyCardPlanningRepository.findByUserIdAndMonthAndYear(userId, month, year, pageable);

		Map<Long, BigDecimal> spentByTransactionMethod = expenseRepository.sumAmountGroupedByTransactionMethod(userId, year, month).stream()
				.collect(Collectors.toMap(TransactionMethodTotalProjection::getTransactionMethodId, TransactionMethodTotalProjection::getTotal));

		return page.map(planning -> toItemResponse(planning, spentByTransactionMethod));
	}

	private MonthlyCardPlanningItemResponse toItemResponse(MonthlyCardPlanning planning, Map<Long, BigDecimal> spentByTransactionMethod) {
		BigDecimal planned = planning.getAmount();
		BigDecimal spent = spentByTransactionMethod.getOrDefault(planning.getTransactionMethod().getId(), BigDecimal.ZERO);
		BigDecimal remaining = planned.subtract(spent);
		BigDecimal percentage = planned.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: spent.divide(planned, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(2, RoundingMode.HALF_UP);

		return new MonthlyCardPlanningItemResponse(
				planning.getId(),
				transactionMethodMapper.toResponse(planning.getTransactionMethod()),
				planned,
				spent,
				remaining,
				percentage);
	}

	private void assertPeriodAvailable(Long userId, MonthlyCardPlanningRequest request, Long planningIdToExclude) {
		monthlyCardPlanningRepository.findByUserIdAndTransactionMethodIdAndMonthAndYear(
						userId, request.transactionMethodId(), request.month(), request.year())
				.filter(existing -> !existing.getId().equals(planningIdToExclude))
				.ifPresent(existing -> {
					throw new DuplicateResourceException(
							"Já existe um planejamento para este cartão no período informado.");
				});
	}

	private MonthlyCardPlanning findOwnedPlanning(Long userId, Long planningId) {
		return monthlyCardPlanningRepository.findByIdAndUserId(planningId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Monthly card planning not found: " + planningId));
	}

	private TransactionMethod findOwnedCardTransactionMethod(Long userId, Long transactionMethodId) {
		TransactionMethod transactionMethod = transactionMethodRepository.findByIdAndUserId(transactionMethodId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction method not found: " + transactionMethodId));
		if (transactionMethod.getType() != TransactionMethodType.CARD) {
			throw new InvalidRequestException("Card planning requires a transaction method of type CARD: " + transactionMethodId);
		}
		return transactionMethod;
	}

}
