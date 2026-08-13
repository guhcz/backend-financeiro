package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningRequest;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningResponse;
import com.personal.backend_financeiro.entity.CreditCard;
import com.personal.backend_financeiro.entity.MonthlyCardPlanning;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CreditCardMapper;
import com.personal.backend_financeiro.mapper.MonthlyCardPlanningMapper;
import com.personal.backend_financeiro.repository.CreditCardRepository;
import com.personal.backend_financeiro.repository.CreditCardTotalProjection;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyCardPlanningRepository;
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
	private final CreditCardRepository creditCardRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final MonthlyCardPlanningMapper monthlyCardPlanningMapper;
	private final CreditCardMapper creditCardMapper;

	@Transactional
	public MonthlyCardPlanningResponse create(Long userId, MonthlyCardPlanningRequest request) {
		CreditCard creditCard = findOwnedCreditCard(userId, request.creditCardId());
		assertPeriodAvailable(userId, request, null);

		MonthlyCardPlanning planning = monthlyCardPlanningMapper.toEntity(request);
		planning.setUser(userRepository.getReferenceById(userId));
		planning.setCreditCard(creditCard);

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
		CreditCard creditCard = findOwnedCreditCard(userId, request.creditCardId());
		assertPeriodAvailable(userId, request, planningId);

		monthlyCardPlanningMapper.updateEntityFromRequest(request, planning);
		planning.setCreditCard(creditCard);

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

		Map<Long, BigDecimal> spentByCreditCard = expenseRepository.sumAmountGroupedByCreditCard(userId, year, month).stream()
				.collect(Collectors.toMap(CreditCardTotalProjection::getCreditCardId, CreditCardTotalProjection::getTotal));

		return page.map(planning -> toItemResponse(planning, spentByCreditCard));
	}

	private MonthlyCardPlanningItemResponse toItemResponse(MonthlyCardPlanning planning, Map<Long, BigDecimal> spentByCreditCard) {
		BigDecimal planned = planning.getAmount();
		BigDecimal spent = spentByCreditCard.getOrDefault(planning.getCreditCard().getId(), BigDecimal.ZERO);
		BigDecimal remaining = planned.subtract(spent);
		BigDecimal percentage = planned.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: spent.divide(planned, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(2, RoundingMode.HALF_UP);

		return new MonthlyCardPlanningItemResponse(
				planning.getId(),
				creditCardMapper.toResponse(planning.getCreditCard()),
				planned,
				spent,
				remaining,
				percentage);
	}

	private void assertPeriodAvailable(Long userId, MonthlyCardPlanningRequest request, Long planningIdToExclude) {
		monthlyCardPlanningRepository.findByUserIdAndCreditCardIdAndMonthAndYear(
						userId, request.creditCardId(), request.month(), request.year())
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

	private CreditCard findOwnedCreditCard(Long userId, Long creditCardId) {
		return creditCardRepository.findByIdAndUserId(creditCardId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Credit card not found: " + creditCardId));
	}

}
