package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryRequest;
import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceInUseException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private RecurringExpenseRepository recurringExpenseRepository;
	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void create_throwsDuplicateResourceException_whenNameAlreadyExists() {
		when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Food")).thenReturn(true);

		CategoryRequest request = new CategoryRequest("Food", "#FF0000", null);

		assertThatThrownBy(() -> categoryService.create(1L, request))
				.isInstanceOf(DuplicateResourceException.class);

		verify(categoryRepository, never()).save(any());
	}

	@Test
	void create_savesCategory_whenNameIsUnique() {
		CategoryRequest request = new CategoryRequest("Food", "#FF0000", null);
		Category entity = new Category();
		Category saved = new Category();
		CategoryResponse response = new CategoryResponse(1L, "Food", "#FF0000", null, true);

		when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Food")).thenReturn(false);
		when(categoryMapper.toEntity(request)).thenReturn(entity);
		when(userRepository.getReferenceById(1L)).thenReturn(new User());
		when(categoryRepository.save(entity)).thenReturn(saved);
		when(categoryMapper.toResponse(saved)).thenReturn(response);

		CategoryResponse result = categoryService.create(1L, request);

		assertThat(result).isEqualTo(response);
	}

	@Test
	void update_throwsResourceNotFoundException_whenNotOwnedByUser() {
		when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

		CategoryRequest request = new CategoryRequest("Food", "#FF0000", null);

		assertThatThrownBy(() -> categoryService.update(1L, 5L, request))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_throwsDuplicateResourceException_whenRenamingToAnotherExistingCategory() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(1L, "Transport", 5L)).thenReturn(true);

		CategoryRequest request = new CategoryRequest("Transport", "#FF0000", null);

		assertThatThrownBy(() -> categoryService.update(1L, 5L, request))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void delete_throwsResourceInUseException_whenCategoryHasExpenses() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
		when(expenseRepository.existsByCategoryId(5L)).thenReturn(true);

		assertThatThrownBy(() -> categoryService.delete(1L, 5L))
				.isInstanceOf(ResourceInUseException.class);

		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void delete_throwsResourceInUseException_whenCategoryHasRecurringExpenses() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
		when(expenseRepository.existsByCategoryId(5L)).thenReturn(false);
		when(recurringExpenseRepository.existsByCategoryId(5L)).thenReturn(true);

		assertThatThrownBy(() -> categoryService.delete(1L, 5L))
				.isInstanceOf(ResourceInUseException.class);

		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void delete_deletesCategory_whenNoExpensesReferenceIt() {
		Category category = new Category();
		when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
		when(expenseRepository.existsByCategoryId(5L)).thenReturn(false);
		when(recurringExpenseRepository.existsByCategoryId(5L)).thenReturn(false);

		categoryService.delete(1L, 5L);

		verify(categoryRepository, times(1)).delete(category);
	}

}
