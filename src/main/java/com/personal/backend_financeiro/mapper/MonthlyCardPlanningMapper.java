package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningRequest;
import com.personal.backend_financeiro.dto.monthlycardplanning.MonthlyCardPlanningResponse;
import com.personal.backend_financeiro.entity.MonthlyCardPlanning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CreditCardMapper.class)
public interface MonthlyCardPlanningMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "creditCard", ignore = true)
	MonthlyCardPlanning toEntity(MonthlyCardPlanningRequest request);

	MonthlyCardPlanningResponse toResponse(MonthlyCardPlanning monthlyCardPlanning);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "creditCard", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(MonthlyCardPlanningRequest request, @MappingTarget MonthlyCardPlanning monthlyCardPlanning);

}
