package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodCardDetailsRequest;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodCardDetailsResponse;
import com.personal.backend_financeiro.dto.transactionmethod.TransactionMethodResponse;
import com.personal.backend_financeiro.entity.CreditCard;
import com.personal.backend_financeiro.entity.TransactionMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMethodMapper {

	@Mapping(target = "card", source = "creditCard")
	TransactionMethodResponse toResponse(TransactionMethod transactionMethod);

	TransactionMethodCardDetailsResponse toCardDetailsResponse(CreditCard creditCard);

	@Mapping(target = "id", ignore = true)
	CreditCard toCreditCardEntity(TransactionMethodCardDetailsRequest request);

	@Mapping(target = "id", ignore = true)
	void updateCreditCardFromRequest(TransactionMethodCardDetailsRequest request, @MappingTarget CreditCard creditCard);

}
