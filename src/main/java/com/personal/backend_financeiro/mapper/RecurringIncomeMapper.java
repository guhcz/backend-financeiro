package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeCreateRequest;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeResponse;
import com.personal.backend_financeiro.dto.recurringincome.RecurringIncomeUpdateRequest;
import com.personal.backend_financeiro.entity.RecurringIncome;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface RecurringIncomeMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "nextGenerationDate", ignore = true)
	RecurringIncome toEntity(RecurringIncomeCreateRequest request);

	@Mapping(target = "active", expression = "java(recurringIncome.getStatus() == com.personal.backend_financeiro.enums.RecurrenceStatus.ACTIVE)")
	@Mapping(target = "status", expression = "java(recurringIncome.getStatus().name())")
	RecurringIncomeResponse toResponse(RecurringIncome recurringIncome);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "startDate", ignore = true)
	@Mapping(target = "frequency", ignore = true)
	@Mapping(target = "nextGenerationDate", ignore = true)
	void updateEntityFromRequest(RecurringIncomeUpdateRequest request, @MappingTarget RecurringIncome recurringIncome);

}
