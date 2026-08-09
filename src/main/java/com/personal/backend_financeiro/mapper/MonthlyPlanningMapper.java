package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningRequest;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningResponse;
import com.personal.backend_financeiro.entity.MonthlyPlanning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface MonthlyPlanningMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	MonthlyPlanning toEntity(MonthlyPlanningRequest request);

	MonthlyPlanningResponse toResponse(MonthlyPlanning monthlyPlanning);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(MonthlyPlanningRequest request, @MappingTarget MonthlyPlanning monthlyPlanning);

}
