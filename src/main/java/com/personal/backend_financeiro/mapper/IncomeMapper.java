package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.income.IncomeRequest;
import com.personal.backend_financeiro.dto.income.IncomeResponse;
import com.personal.backend_financeiro.entity.Income;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface IncomeMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "recurringIncome", ignore = true)
	@Mapping(target = "generatedAutomatically", ignore = true)
	@Mapping(target = "recurrenceReferenceYear", ignore = true)
	@Mapping(target = "recurrenceReferenceMonth", ignore = true)
	Income toEntity(IncomeRequest request);

	@Mapping(target = "recurringIncomeId", expression = "java(income.getRecurringIncome() != null ? income.getRecurringIncome().getId() : null)")
	@Mapping(target = "recurring", expression = "java(income.getRecurringIncome() != null)")
	IncomeResponse toResponse(Income income);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "active", ignore = true)
	@Mapping(target = "recurringIncome", ignore = true)
	@Mapping(target = "generatedAutomatically", ignore = true)
	@Mapping(target = "recurrenceReferenceYear", ignore = true)
	@Mapping(target = "recurrenceReferenceMonth", ignore = true)
	void updateEntityFromRequest(IncomeRequest request, @MappingTarget Income income);

}
