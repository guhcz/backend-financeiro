package com.personal.backend_financeiro.mapper;

import com.personal.backend_financeiro.dto.category.CategoryRequest;
import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	Category toEntity(CategoryRequest request);

	CategoryResponse toResponse(Category category);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "active", ignore = true)
	void updateEntityFromRequest(CategoryRequest request, @MappingTarget Category category);

}
