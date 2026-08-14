package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseCreateRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseFilterRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseResponse;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseUpdateRequest;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.Expense;
import com.personal.backend_financeiro.entity.RecurringExpense;
import com.personal.backend_financeiro.entity.TransactionMethod;
import com.personal.backend_financeiro.enums.CardTransactionMode;
import com.personal.backend_financeiro.enums.RecurrenceFrequency;
import com.personal.backend_financeiro.enums.RecurrenceStatus;
import com.personal.backend_financeiro.exception.InvalidRequestException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.RecurringExpenseMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.TransactionMethodRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseSpecifications;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.util.CompetenceResolver;
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import com.personal.backend_financeiro.util.RecurrenceDateCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringExpenseService {

	private final RecurringExpenseRepository recurringExpenseRepository;
	private final CategoryRepository categoryRepository;
	private final TransactionMethodRepository transactionMethodRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseMapper recurringExpenseMapper;
	private final RecurringExpenseGenerationService generationService;

	@Transactional
	public RecurringExpenseResponse create(Long userId, RecurringExpenseCreateRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());
		TransactionMethod transactionMethod = findOwnedTransactionMethod(userId, request.transactionMethodId());
		CompetenceResolver.validateCardTransactionMode(transactionMethod, request.cardTransactionMode());

		if (request.frequency() != RecurrenceFrequency.MONTHLY) {
			throw new InvalidRequestException("Apenas a frequência MONTHLY é suportada no momento.");
		}
		if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
			throw new InvalidRequestException("A data final não pode ser anterior à data inicial.");
		}

		RecurringExpense rule = recurringExpenseMapper.toEntity(request);
		rule.setUser(userRepository.getReferenceById(userId));
		rule.setCategory(category);
		rule.setTransactionMethod(transactionMethod);
		rule.setStatus(RecurrenceStatus.ACTIVE);
		rule.setNextGenerationDate(RecurrenceDateCalculator.resolveOccurrenceDate(request.startDate(), request.dueDay()));

		RecurringExpense saved = recurringExpenseRepository.save(rule);
		generationService.generateInitialOccurrenceIfDue(saved, LocalDate.now());

		return recurringExpenseMapper.toResponse(saved);
	}

	public RecurringExpenseResponse getOne(Long userId, Long id) {
		return recurringExpenseMapper.toResponse(findOwnedRule(userId, id));
	}

	public Page<RecurringExpenseResponse> filter(Long userId, RecurringExpenseFilterRequest filter, Pageable pageable) {
		LocalDate monthStart = null;
		LocalDate monthEnd = null;
		if (filter.referenceMonth() != null || filter.referenceYear() != null) {
			PlanningPeriodUtils.assertValid(filter.referenceMonth(), filter.referenceYear());
			monthStart = PlanningPeriodUtils.firstDayOf(filter.referenceYear(), filter.referenceMonth());
			monthEnd = PlanningPeriodUtils.lastDayOf(filter.referenceYear(), filter.referenceMonth());
		}

		Specification<RecurringExpense> spec = Specification
				.where(RecurringExpenseSpecifications.belongsToUser(userId))
				.and(RecurringExpenseSpecifications.matchesActiveFilter(filter.active()))
				.and(RecurringExpenseSpecifications.hasCategory(filter.categoryId()))
				.and(RecurringExpenseSpecifications.descriptionContains(filter.description()))
				.and(RecurringExpenseSpecifications.activeDuring(monthStart, monthEnd));

		return recurringExpenseRepository.findAll(spec, pageable).map(recurringExpenseMapper::toResponse);
	}

	@Transactional
	public RecurringExpenseResponse update(Long userId, Long id, RecurringExpenseUpdateRequest request) {
		RecurringExpense rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser editada: " + id);
		}
		if (request.endDate() != null && request.endDate().isBefore(rule.getStartDate())) {
			throw new InvalidRequestException("A data final não pode ser anterior à data inicial.");
		}

		Category category = findOwnedCategory(userId, request.categoryId());
		TransactionMethod transactionMethod = findOwnedTransactionMethod(userId, request.transactionMethodId());
		CompetenceResolver.validateCardTransactionMode(transactionMethod, request.cardTransactionMode());
		Integer oldDueDay = rule.getDueDay();

		recurringExpenseMapper.updateEntityFromRequest(request, rule);
		rule.setCategory(category);
		rule.setTransactionMethod(transactionMethod);

		if (rule.getStatus() == RecurrenceStatus.ACTIVE && !Objects.equals(request.dueDay(), oldDueDay)) {
			rule.setNextGenerationDate(RecurrenceDateCalculator.resolveNextGenerationDateFrom(LocalDate.now(), request.dueDay()));
		}

		applyToFutureOccurrences(rule, category, transactionMethod, request.description(), request.amount(),
				request.cardTransactionMode(), request.notes(), LocalDate.now());

		return recurringExpenseMapper.toResponse(rule);
	}

	/**
	 * Occurrences up to app.recurring-expense.lookahead-months ahead are already persisted by the
	 * time a rule is edited or ended, so those edits/endings need to explicitly cascade to them —
	 * otherwise a rule change would only be visible on the current month, with every pre-generated
	 * future month silently keeping the old values.
	 */
	private void applyToFutureOccurrences(RecurringExpense rule, Category category, TransactionMethod transactionMethod,
			String description, BigDecimal amount, CardTransactionMode cardTransactionMode, String notes, LocalDate fromDateInclusive) {
		for (Expense future : expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(rule.getId(), fromDateInclusive)) {
			future.setCategory(category);
			future.setDescription(description);
			future.setAmount(amount);
			future.setTransactionMethod(transactionMethod);
			future.setCardTransactionMode(cardTransactionMode);
			future.setNotes(notes);

			YearMonth competence = CompetenceResolver.resolve(future.getExpenseDate(), transactionMethod.getType(), cardTransactionMode);
			future.setBillingMonth(competence.getMonthValue());
			future.setBillingYear(competence.getYear());
		}
	}

	@Transactional
	public RecurringExpenseResponse pause(Long userId, Long id) {
		RecurringExpense rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser pausada: " + id);
		}
		rule.setStatus(RecurrenceStatus.PAUSED);
		return recurringExpenseMapper.toResponse(rule);
	}

	@Transactional
	public RecurringExpenseResponse resume(Long userId, Long id) {
		RecurringExpense rule = findOwnedRule(userId, id);
		if (rule.getStatus() == RecurrenceStatus.ENDED) {
			throw new InvalidRequestException("Regra encerrada não pode ser reativada: " + id);
		}
		if (rule.getStatus() == RecurrenceStatus.PAUSED) {
			LocalDate today = LocalDate.now();
			if (rule.getEndDate() != null && rule.getEndDate().isBefore(today)) {
				throw new InvalidRequestException("Regra com data final vencida não pode ser reativada: " + id);
			}
			rule.setNextGenerationDate(RecurrenceDateCalculator.resolveNextGenerationDateFrom(today, rule.getDueDay()));
			rule.setStatus(RecurrenceStatus.ACTIVE);
		}
		return recurringExpenseMapper.toResponse(rule);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		RecurringExpense rule = findOwnedRule(userId, id);
		if (expenseRepository.existsIncludingInactiveByRecurringExpenseId(id)) {
			rule.setStatus(RecurrenceStatus.ENDED);
			expenseRepository.deleteAll(
					expenseRepository.findByRecurringExpenseIdAndExpenseDateGreaterThanEqual(id, LocalDate.now()));
		} else {
			recurringExpenseRepository.delete(rule);
		}
	}

	private RecurringExpense findOwnedRule(Long userId, Long id) {
		return recurringExpenseRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found: " + id));
	}

	private Category findOwnedCategory(Long userId, Long categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
	}

	private TransactionMethod findOwnedTransactionMethod(Long userId, Long transactionMethodId) {
		return transactionMethodRepository.findByIdAndUserId(transactionMethodId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction method not found: " + transactionMethodId));
	}

}
