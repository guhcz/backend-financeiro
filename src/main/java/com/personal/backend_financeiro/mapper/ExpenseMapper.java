package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { CategoryMapper.class, TransactionMethodMapper.class })
public interface ExpenseMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "recurringExpense", ignore = true)
	@Mapping(target = "generatedAutomatically", ignore = true)
	@Mapping(target = "recurrenceReferenceYear", ignore = true)
	@Mapping(target = "recurrenceReferenceMonth", ignore = true)
	@Mapping(target = "transactionMethod", ignore = true)
	@Mapping(target = "cardTransactionMode", ignore = true)
	@Mapping(target = "billingMonth", ignore = true)
	@Mapping(target = "billingYear", ignore = true)
	Expense toEntity(ExpenseRequest request);

	@Mapping(target = "recurringExpenseId", expression = "java(expense.getRecurringExpense() != null ? expense.getRecurringExpense().getId() : null)")
	@Mapping(target = "recurring", expression = "java(expense.getRecurringExpense() != null)")
	ExpenseResponse toResponse(Expense expense);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "active", ignore = true)
	@Mapping(target = "recurringExpense", ignore = true)
	@Mapping(target = "generatedAutomatically", ignore = true)
	@Mapping(target = "recurrenceReferenceYear", ignore = true)
	@Mapping(target = "recurrenceReferenceMonth", ignore = true)
	@Mapping(target = "transactionMethod", ignore = true)
	@Mapping(target = "cardTransactionMode", ignore = true)
	@Mapping(target = "billingMonth", ignore = true)
	@Mapping(target = "billingYear", ignore = true)
	void updateEntityFromRequest(ExpenseRequest request, @MappingTarget Expense expense);

}
