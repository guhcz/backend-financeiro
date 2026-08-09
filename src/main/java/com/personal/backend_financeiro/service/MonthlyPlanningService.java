package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningItemResponse;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningRequest;
import com.personal.backend_financeiro.dto.monthlyplanning.MonthlyPlanningResponse;
import com.personal.backend_financeiro.entity.Category;
import com.personal.backend_financeiro.entity.MonthlyPlanning;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.CategoryMapper;
import com.personal.backend_financeiro.mapper.MonthlyPlanningMapper;
import com.personal.backend_financeiro.repository.CategoryRepository;
import com.personal.backend_financeiro.repository.CategoryTotalProjection;
import com.personal.backend_financeiro.repository.ExpenseRepository;
import com.personal.backend_financeiro.repository.MonthlyPlanningRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.util.PlanningPeriodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyPlanningService {

	private final MonthlyPlanningRepository monthlyPlanningRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final ExpenseRepository expenseRepository;
	private final MonthlyPlanningMapper monthlyPlanningMapper;
	private final CategoryMapper categoryMapper;

	@Transactional
	public MonthlyPlanningResponse create(Long userId, MonthlyPlanningRequest request) {
		Category category = findOwnedCategory(userId, request.categoryId());
		assertPeriodAvailable(userId, request, null);

		MonthlyPlanning planning = monthlyPlanningMapper.toEntity(request);
		planning.setUser(userRepository.getReferenceById(userId));
		planning.setCategory(category);

		MonthlyPlanning saved = monthlyPlanningRepository.save(planning);
		return monthlyPlanningMapper.toResponse(saved);
	}

	public MonthlyPlanningResponse getOne(Long userId, Long planningId) {
		MonthlyPlanning planning = findOwnedPlanning(userId, planningId);
		return monthlyPlanningMapper.toResponse(planning);
	}

	@Transactional
	public MonthlyPlanningResponse update(Long userId, Long planningId, MonthlyPlanningRequest request) {
		MonthlyPlanning planning = findOwnedPlanning(userId, planningId);
		Category category = findOwnedCategory(userId, request.categoryId());
		assertPeriodAvailable(userId, request, planningId);

		monthlyPlanningMapper.updateEntityFromRequest(request, planning);
		planning.setCategory(category);

		return monthlyPlanningMapper.toResponse(planning);
	}

	@Transactional
	public void delete(Long userId, Long planningId) {
		MonthlyPlanning planning = findOwnedPlanning(userId, planningId);
		monthlyPlanningRepository.delete(planning);
	}

	public Page<MonthlyPlanningItemResponse> list(Long userId, Integer month, Integer year, Pageable pageable) {
		PlanningPeriodUtils.assertValid(month, year);

		Page<MonthlyPlanning> page = monthlyPlanningRepository.findByUserIdAndMonthAndYear(userId, month, year, pageable);

		LocalDate start = PlanningPeriodUtils.firstDayOf(year, month);
		LocalDate end = PlanningPeriodUtils.lastDayOf(year, month);
		Map<Long, BigDecimal> spentByCategory = expenseRepository.sumAmountGroupedByCategory(userId, start, end).stream()
				.collect(Collectors.toMap(CategoryTotalProjection::getCategoryId, CategoryTotalProjection::getTotal));

		return page.map(planning -> toItemResponse(planning, spentByCategory));
	}

	private MonthlyPlanningItemResponse toItemResponse(MonthlyPlanning planning, Map<Long, BigDecimal> spentByCategory) {
		BigDecimal planned = planning.getAmount();
		BigDecimal spent = spentByCategory.getOrDefault(planning.getCategory().getId(), BigDecimal.ZERO);
		BigDecimal remaining = planned.subtract(spent);
		BigDecimal percentage = planned.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: spent.divide(planned, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(2, RoundingMode.HALF_UP);

		return new MonthlyPlanningItemResponse(
				planning.getId(),
				categoryMapper.toResponse(planning.getCategory()),
				planned,
				spent,
				remaining,
				percentage);
	}

	private void assertPeriodAvailable(Long userId, MonthlyPlanningRequest request, Long planningIdToExclude) {
		monthlyPlanningRepository.findByUserIdAndCategoryIdAndMonthAndYear(
						userId, request.categoryId(), request.month(), request.year())
				.filter(existing -> !existing.getId().equals(planningIdToExclude))
				.ifPresent(existing -> {
					throw new DuplicateResourceException(
							"Já existe um planejamento para esta categoria no período informado.");
				});
	}

	private MonthlyPlanning findOwnedPlanning(Long userId, Long planningId) {
		return monthlyPlanningRepository.findByIdAndUserId(planningId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Monthly planning not found: " + planningId));
	}

	private Category findOwnedCategory(Long userId, Long categoryId) {
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
	}

}
