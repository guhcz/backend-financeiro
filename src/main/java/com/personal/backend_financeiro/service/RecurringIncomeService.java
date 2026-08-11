package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeCreateRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeFilterRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeResponse;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeUpdateRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.RecurringIncomeMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeRepository;
import com.personal.backend_financeiro.repository.RecurringIncomeSpecifications;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import com.personal.backend_financeiro.util.RecurrenceDateCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringIncomeService {

	private final RecurringIncomeRepository recurringIncomeRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final IncomeRepository incomeRepository;
	private final RecurringIncomeMapper recurringIncomeMapper;
	private final RecurringIncomeGenerationService generationService;

	@Transactional
	public RecurringIncomeResponse create(Long userId, RecurringIncomeCreateRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());

		if (request.frequency() != RecurrenceFrequency.MONTHLY) {
			throw new InvalidRequestException("Apenas a frequência MONTHLY é suportada no momento.");
		}
		if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
			throw new InvalidRequestException("A data final não pode ser anterior à data inicial.");
		}

		RecurringIncome rule = recurringIncomeMapper.toEntity(request);
		rule.setUser(userRepository.getReferenceById(userId));
		rule.setCategory(category);
		rule.setStatus(RecurrenceStatus.ACTIVE);
		rule.setNextGenerationDate(RecurrenceDateCalculator.resolveOccurrenceDate(request.startDate(), request.receiptDay()));

		RecurringIncome saved = recurringIncomeRepository.save(rule);
		generationService.generateInitialOccurrenceIfDue(saved, LocalDate.now());

		return recurringIncomeMapper.toResponse(saved);
	}

	public RecurringIncomeResponse getOne(Long userId, Long id) {
		return recurringIncomeMapper.toResponse(findOwnedRule(userId, id));
	}

	public Page<RecurringIncomeResponse> filter(Long userId, RecurringIncomeFilterRequest filter, Pageable pageable) {
		LocalDate monthStart = null;
		LocalDate monthEnd = null;
		if (filter.referenceMonth() != null || filter.referenceYear() != null) {
			PlanningPeriodUtils.assertValid(filter.referenceMonth(), filter.referenceYear());
			monthStart = PlanningPeriodUtils.firstDayOf(filter.referenceYear(), filter.referenceMonth());
			monthEnd = PlanningPeriodUtils.lastDayOf(filter.referenceYear(), filter.referenceMonth());
		}

		Specification<RecurringIncome> spec = Specification
				.where(RecurringIncomeSpecifications.belongsToUser(userId))
				.and(RecurringIncomeSpecifications.matchesActiveFilter(filter.active()))
				.and(RecurringIncomeSpecifications.hasCategory(filter.categoryId()))
				.and(RecurringIncomeSpecifications.descriptionContains(filter.description()))
				.and(RecurringIncomeSpecifications.activeDuring(monthStart, monthEnd));

		return recurringIncomeRepository.findAll(spec, pageable).map(recurringIncomeMapper::toResponse);
	}

	@Transactional
	public RecurringIncomeResponse update(Long userId, Long id, RecurringIncomeUpdateRequest request) {
		RecurringIncome rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser editada: " + id);
		}
		if (request.endDate() != null && request.endDate().isBefore(rule.getStartDate())) {
			throw new InvalidRequestException("A data final não pode ser anterior à data inicial.");
		}

		Category category = findOwnedCategory(userId, request.categoryId());
		Integer oldReceiptDay = rule.getReceiptDay();

		recurringIncomeMapper.updateEntityFromRequest(request, rule);
		rule.setCategory(category);

		if (rule.getStatus() == RecurrenceStatus.ACTIVE && !Objects.equals(request.receiptDay(), oldReceiptDay)) {
			rule.setNextGenerationDate(RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.now(), request.receiptDay()));
		}

		return recurringIncomeMapper.toResponse(rule);
	}

	@Transactional
	public RecurringIncomeResponse pause(Long userId, Long id) {
		RecurringIncome rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser pausada: " + id);
		}
		rule.setStatus(RecurrenceStatus.PAUSED);
		return recurringIncomeMapper.toResponse(rule);
	}

	@Transactional
	public RecurringIncomeResponse resume(Long userId, Long id) {
		RecurringIncome rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser reativada: " + id);
		}
		if (rule.getStatus() == RecurrenceStatus.PAUSED) {
			LocalDate today = LocalDate.now();
			if (rule.getEndDate() != null && rule.getEndDate().isBefore(today)) {
				throw new InvalidRequestException("Regra com data final vencida não pode ser reativada: " + id);
			}
			rule.setNextGenerationDate(RecurrenceDateCalculator.resolveNextGenerationDateFrom(today, rule.getReceiptDay()));
			rule.setStatus(RecurrenceStatus.ACTIVE);
		}
		return recurringIncomeMapper.toResponse(rule);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		RecurringIncome rule = findOwnedRule(userId, id);
		if (incomeRepository.existsByRecurringIncomeId(id)) {
			rule.setStatus(RecurrenceStatus.ENDED);
		} else {
			recurringIncomeRepository.delete(rule);
		}
	}

	private RecurringIncome findOwnedRule(Long userId, Long id) {
		return recurringIncomeRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Recurring income not found: " + id));
	}

	private Category findOwnedCategory(Long userId, Long categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
	}

}
