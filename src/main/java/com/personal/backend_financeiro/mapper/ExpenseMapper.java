package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.expense.ExpenseRequest;
import com.personal.backend_financeiro.dto.expense.ExpenseResponse;
import com.personal.backend_financeiro.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface ExpenseMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	Expense toEntity(ExpenseRequest request);

	ExpenseResponse toResponse(Expense expense);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(ExpenseRequest request, @MappingTarget Expense expense);

}
