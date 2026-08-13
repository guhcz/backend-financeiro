package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.expense.ExpenseFilterRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.CreditCard;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.enums.PaymentMethod;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.enums.RecurringUpdateScope;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.ExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CreditCardRepository;
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
	private final CreditCardRepository creditCardRepository;
	private final UserRepository userRepository;
	private final ExpenseMapper expenseMapper;

	@Transactional
	public ExpenseResponse create(Long userId, ExpenseRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());
		CreditCard creditCard = resolveCreditCard(userId, request);

		Expense expense = expenseMapper.toEntity(request);
		expense.setUser(userRepository.getReferenceById(userId));
		expense.setCategory(category);
		applyCreditCardAndCompetence(expense, request, creditCard);

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
				.and(ExpenseSpecifications.descriptionContains(filter.description()))
				.and(ExpenseSpecifications.isRecurring(filter.recurring()));

		return expenseRepository.findAll(spec, pageable).map(expenseMapper::toResponse);
	}

	@Transactional
	public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request, RecurringUpdateScope scope) {
		Expense expense = findOwnedExpense(userId, expenseId);
		Category category = findOwnedCategory(userId, request.categoryId());
		CreditCard creditCard = resolveCreditCard(userId, request);

		expenseMapper.updateEntityFromRequest(request, expense);
		expense.setCategory(category);
		applyCreditCardAndCompetence(expense, request, creditCard);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && expense.getRecurringExpense() != null) {
			RecurringExpense rule = expense.getRecurringExpense();
			rule.setCategory(category);
			rule.setDescription(request.description());
			rule.setAmount(request.amount());
			rule.setPaymentMethod(request.paymentMethod());
			rule.setNotes(request.notes());
			rule.setCreditCard(creditCard);
		}

		return expenseMapper.toResponse(expense);
	}

	@Transactional
	public void delete(Long userId, Long expenseId, RecurringUpdateScope scope) {
		Expense expense = findOwnedExpense(userId, expenseId);

		if (scope == RecurringUpdateScope.THIS_AND_FUTURE && expense.getRecurringExpense() != null) {
			expense.getRecurringExpense().setStatus(RecurrenceStatus.ENDED);
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

	private CreditCard resolveCreditCard(Long userId, ExpenseRequest request) {
		if (request.paymentMethod() != PaymentMethod.CREDIT_CARD) {
			return null;
		}
		if (request.creditCardId() == null) {
			throw new InvalidRequestException("creditCardId is required when paymentMethod is CREDIT_CARD");
		}
		return creditCardRepository.findByIdAndUserId(request.creditCardId(), userId)
				.orElseThrow(() -> new ResourceNotFoundException("Credit card not found: " + request.creditCardId()));
	}

	/**
	 * Sets the credit card link (cleared when the payment method is not CREDIT_CARD, e.g. an
	 * edit that switches a card expense to PIX -- kept only to track spend per card for card
	 * planning) and recomputes the financial competence (billingMonth/billingYear) via the
	 * single centralized rule, reused by every other place that needs to know which month an
	 * expense counts towards.
	 */
	private void applyCreditCardAndCompetence(Expense expense, ExpenseRequest request, CreditCard creditCard) {
		expense.setCreditCard(creditCard);
		YearMonth competence = CompetenceResolver.resolve(request.expenseDate());
		expense.setBillingMonth(competence.getMonthValue());
		expense.setBillingYear(competence.getYear());
	}

}
