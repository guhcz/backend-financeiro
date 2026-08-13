package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.creditcard.CreditCardRequest;
import com.personal.backend_financeiro.dto.creditcard.CreditCardResponse;
import com.personal.backend_financeiro.entity.CreditCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	CreditCard toEntity(CreditCardRequest request);

	CreditCardResponse toResponse(CreditCard creditCard);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(CreditCardRequest request, @MappingTarget CreditCard creditCard);

}
