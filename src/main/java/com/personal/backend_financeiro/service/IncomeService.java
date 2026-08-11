package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.income.IncomeFilterRequest;
import com.personal.backend_financeiro.dto.income.IncomeRequest;
import com.personal.backend_financeiro.dto.income.IncomeResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Income;
import com.personal.backend_financeiro.entity.RecurringIncome;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.IncomeMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.IncomeRepository;
import com.personal.backend_financeiro.repository.IncomeSpecifications;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeService {

	private final IncomeRepository incomeRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final IncomeMapper incomeMapper;

	@Transactional
	public IncomeResponse create(Long userId, IncomeRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());

		Income income = incomeMapper.toEntity(request);
		income.setUser(userRepository.getReferenceById(userId));
		income.setCategory(category);

		Income saved = incomeRepository.save(income);
		return incomeMapper.toResponse(saved);
	}

	public IncomeResponse getOne(Long userId, Long incomeId) {
		Income income = findOwnedIncome(userId, incomeId);
		return incomeMapper.toResponse(income);
	}

	public Page<IncomeResponse> filter(Long userId, IncomeFilterRequest filter, Pageable pageable) {
		Specification<Income> spec = Specification
				.where(IncomeSpecifications.belongsToUser(userId))
				.and(IncomeSpecifications.hasCategory(filter.categoryId()))
				.and(IncomeSpecifications.hasReceiptMethod(filter.receiptMethod()))
				.and(IncomeSpecifications.incomeDateFrom(filter.startDate()))
				.and(IncomeSpecifications.incomeDateTo(filter.endDate()))
				.and(IncomeSpecifications.descriptionContains(filter.description()))
				.and(IncomeSpecifications.isRecurring(filter.recurring()));

		return incomeRepository.findAll(spec, pageable).map(incomeMapper::toResponse);
	}

	@Transactional
	public IncomeResponse update(Long userId, Long incomeId, IncomeRequest request, RecurringUpdateScope scope) {
		Income income = findOwnedIncome(userId, incomeId);
		Category category = findOwnedCategory(userId, request.categoryId());

		incomeMapper.updateEntityFromRequest(request, income);
		income.setCategory(category);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && income.getRecurringIncome() != null) {
			RecurringIncome rule = income.getRecurringIncome();
			rule.setCategory(category);
			rule.setDescription(request.description());
			rule.setAmount(request.amount());
			rule.setReceiptMethod(request.receiptMethod());
			rule.setNotes(request.notes());
		}

		return incomeMapper.toResponse(income);
	}

	@Transactional
	public void delete(Long userId, Long incomeId, RecurringUpdateScope scope) {
		Income income = findOwnedIncome(userId, incomeId);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && income.getRecurringIncome() != null) {
			income.getRecurringIncome().setStatus(RecurrenceStatus.ENDED);
		}

		incomeRepository.delete(income);
	}

	private Income findOwnedIncome(Long userId, Long incomeId) {
		return incomeRepository.findByIdAndUserId(incomeId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Income not found: " + incomeId));
	}

	private Category findOwnedCategory(Long userId, Long categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
	}

}
