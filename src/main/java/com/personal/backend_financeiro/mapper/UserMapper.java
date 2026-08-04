package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.user.CreateUserRequest;
import com.personal.backend_financeiro.dto.user.UserResponse;
import com.personal.backend_financeiro.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

	/**
	 * Password is set explicitly by the Service after encoding — never mapped as-is here.
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "password", ignore = true)
	User toEntity(CreateUserRequest request);

	UserResponse toResponse(User user);

}
