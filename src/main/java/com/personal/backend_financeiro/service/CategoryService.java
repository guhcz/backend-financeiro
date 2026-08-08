package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.category.CategoryRequest;
import com.personal.backend_financeiro.dto.category.CategoryResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceInUseException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.RecurringExpenseRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final RecurringExpenseRepository recurringExpenseRepository;
	private final CategoryMapper categoryMapper;

	@Transactional
	public CategoryResponse create(Long userId, CategoryRequest request) {
		if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
			throw new DuplicateResourceException("Category already exists: " + request.name());
		}

		Category category = categoryMapper.toEntity(request);
		category.setUser(userRepository.getReferenceById(userId));

		Category saved = categoryRepository.save(category);
		return categoryMapper.toResponse(saved);
	}

	public List<CategoryResponse> listAllByUser(Long userId) {
		return categoryRepository.findByUserId(userId).stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	public Page<CategoryResponse> listByUser(Long userId, Pageable pageable) {
		return categoryRepository.findByUserId(userId, pageable).map(categoryMapper::toResponse);
	}

	@Transactional
	public CategoryResponse update(Long userId, Long categoryId, CategoryRequest request) {
		Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

		if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, request.name(), categoryId)) {
			throw new DuplicateResourceException("Category already exists: " + request.name());
		}

		categoryMapper.updateEntityFromRequest(request, category);
		return categoryMapper.toResponse(category);
	}

	@Transactional
	public void delete(Long userId, Long categoryId) {
		Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

		/*
		 * Soft delete (@SQLDelete -> UPDATE) never triggers the FK's ON DELETE RESTRICT,
		 * since no physical DELETE is issued. Without this check, an expense would keep
		 * pointing at a category that @SQLRestriction makes invisible to every query,
		 * and loading that expense's lazy category association later throws
		 * EntityNotFoundException (500) instead of failing safely here.
		 */
		if (expenseRepository.existsByCategoryId(categoryId)) {
			throw new ResourceInUseException("Category has expenses and cannot be deleted: " + categoryId);
		}
		if (recurringExpenseRepository.existsByCategoryId(categoryId)) {
			throw new ResourceInUseException("Category has recurring expenses and cannot be deleted: " + categoryId);
		}

		categoryRepository.delete(category);
	}

}
