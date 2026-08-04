package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitRequest;
import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitResponse;
import com.personal.backend_financeiro.entity.MonthlyLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MonthlyLimitMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	MonthlyLimit toEntity(MonthlyLimitRequest request);

	MonthlyLimitResponse toResponse(MonthlyLimit monthlyLimit);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(MonthlyLimitRequest request, @MappingTarget MonthlyLimit monthlyLimit);

}
