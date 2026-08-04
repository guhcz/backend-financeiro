package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.expense.ExpenseFilterRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.ExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.ExpenseSpecifications;
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
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final ExpenseMapper expenseMapper;

	@Transactional
	public ExpenseResponse create(Long userId, ExpenseRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());

		Expense expense = expenseMapper.toEntity(request);
		expense.setUser(userRepository.getReferenceById(userId));
		expense.setCategory(category);

		Expense saved = expenseRepository.save(expense);
		return expenseMapper.toResponse(saved);
	}

	public ExpenseResponse getOne(Long userId, Long expenseId) {
		Expense expense = findOwnedExpense(userId, expenseId);
		return expenseMapper.toResponse(expense);
	}

	public Page<ExpenseResponse> filter(Long userId, ExpenseFilterRequest filter, Pageable pageable) {
		Specification<Expense> spec = Specification
				.where(ExpenseSpecifications.belongsToUser(userId))
				.and(ExpenseSpecifications.hasCategory(filter.categoryId()))
				.and(ExpenseSpecifications.hasPaymentMethod(filter.paymentMethod()))
				.and(ExpenseSpecifications.expenseDateFrom(filter.startDate()))
				.and(ExpenseSpecifications.expenseDateTo(filter.endDate()))
				.and(ExpenseSpecifications.descriptionContains(filter.description()));

		return expenseRepository.findAll(spec, pageable).map(expenseMapper::toResponse);
	}

	@Transactional
	public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
		Expense expense = findOwnedExpense(userId, expenseId);
		Category category = findOwnedCategory(userId, request.categoryId());

		expenseMapper.updateEntityFromRequest(request, expense);
		expense.setCategory(category);

		return expenseMapper.toResponse(expense);
	}

	@Transactional
	public void delete(Long userId, Long expenseId) {
		Expense expense = findOwnedExpense(userId, expenseId);
		expenseRepository.delete(expense);
	}

	private Expense findOwnedExpense(Long userId, Long expenseId) {
		return expenseRepository.findByIdAndUserId(expenseId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));
	}

	private Category findOwnedCategory(Long userId, Long categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
	}

}
