package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseCreateRequest;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseResponse;
import com.personal.backend_financeiro.dto.recurringexpense.RecurringExpenseUpdateRequest;
import com.personal.backend_financeiro.entity.RecurringExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface RecurringExpenseMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "nextGenerationDate", ignore = true)
	RecurringExpense toEntity(RecurringExpenseCreateRequest request);

	@Mapping(target = "active", expression = "java(recurringExpense.getStatus() == com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE)")
	@Mapping(target = "status", expression = "java(recurringExpense.getStatus().name())")
	RecurringExpenseResponse toResponse(RecurringExpense recurringExpense);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "startDate", ignore = true)
	@Mapping(target = "frequency", ignore = true)
	@Mapping(target = "nextGenerationDate", ignore = true)
	void updateEntityFromRequest(RecurringExpenseUpdateRequest request, @MappingTarget RecurringExpense recurringExpense);

}
