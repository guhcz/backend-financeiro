package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.expense.ExpenseFilterRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.ExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.TransactionMethodRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.ExpenseSpecifications;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.util.CompetenceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final CategoryRepository categoryRepository;
	private final TransactionMethodRepository transactionMethodRepository;
	private final UserRepository userRepository;
	private final ExpenseMapper expenseMapper;

	@Transactional
	public ExpenseResponse create(Long userId, ExpenseRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());
		TransactionMethod transactionMethod = findOwnedTransactionMethod(userId, request.transactionMethodId());

		Expense expense = expenseMapper.toEntity(request);
		expense.setUser(userRepository.getReferenceById(userId));
		expense.setCategory(category);
		applyTransactionMethodAndCompetence(expense, request, transactionMethod);

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
				.and(ExpenseSpecifications.hasTransactionMethod(filter.transactionMethodId()))
				.and(ExpenseSpecifications.expenseDateFrom(filter.startDate()))
				.and(ExpenseSpecifications.expenseDateTo(filter.endDate()))
				.and(ExpenseSpecifications.descriptionContains(filter.description()))
				.and(ExpenseSpecifications.isRecurring(filter.recurring()));

		return expenseRepository.findAll(spec, pageable).map(expenseMapper::toResponse);
	}

	@Transactional
	public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request, RecurringUpdateScope scope) {
		Expense expense = findOwnedExpense(userId, expenseId);
		Category category = findOwnedCategory(userId, request.categoryId());
		TransactionMethod transactionMethod = findOwnedTransactionMethod(userId, request.transactionMethodId());

		expenseMapper.updateEntityFromRequest(request, expense);
		expense.setCategory(category);
		applyTransactionMethodAndCompetence(expense, request, transactionMethod);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && expense.getRecurringExpense() != null) {
			RecurringExpense rule = expense.getRecurringExpense();
			rule.setCategory(category);
			rule.setDescription(request.description());
			rule.setAmount(request.amount());
			rule.setTransactionMethod(transactionMethod);
			rule.setCardTransactionMode(request.cardTransactionMode());
			rule.setNotes(request.notes());

			for (Expense future : expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(
					rule.getId(), expense.getExpenseDate().plusDays(1))) {
				future.setCategory(category);
				future.setDescription(request.description());
				future.setAmount(request.amount());
				future.setTransactionMethod(transactionMethod);
				future.setCardTransactionMode(request.cardTransactionMode());
				future.setNotes(request.notes());

				YearMonth futureCompetence = CompetenceResolver.resolve(
						future.getExpenseDate(), transactionMethod.getType(), request.cardTransactionMode());
				future.setBillingMonth(futureCompetence.getMonthValue());
				future.setBillingYear(futureCompetence.getYear());
			}
		}

		return expenseMapper.toResponse(expense);
	}

	@Transactional
	public void delete(Long userId, Long expenseId, RecurringUpdateScope scope) {
		Expense expense = findOwnedExpense(userId, expenseId);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && expense.getRecurringExpense() != null) {
			RecurringExpense rule = expense.getRecurringExpense();
			rule.setStatus(RecurrenceStatus.ENDED);
			expenseRepository.deleteAll(expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(
					rule.getId(), expense.getExpenseDate().plusDays(1)));
		}

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

	private TransactionMethod findOwnedTransactionMethod(Long userId, Long transactionMethodId) {
		return transactionMethodRepository.findByIdAndUserId(transactionMethodId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction method not found: " + transactionMethodId));
	}

	/**
	 * Validates the cardTransactionMode/transactionMethod consistency (required iff type ==
	 * CARD) and recomputes the financial competence (billingMonth/billingYear) via the single
	 * centralized rule, reused by every other place that needs to know which month an expense
	 * counts towards.
	 */
	private void applyTransactionMethodAndCompetence(Expense expense, ExpenseRequest request, TransactionMethod transactionMethod) {
		CompetenceResolver.validateCardTransactionMode(transactionMethod, request.cardTransactionMode());

		expense.setTransactionMethod(transactionMethod);
		expense.setCardTransactionMode(request.cardTransactionMode());

		YearMonth competence = CompetenceResolver.resolve(request.expenseDate(), transactionMethod.getType(), request.cardTransactionMode());
		expense.setBillingMonth(competence.getMonthValue());
		expense.setBillingYear(competence.getYear());
	}

}
