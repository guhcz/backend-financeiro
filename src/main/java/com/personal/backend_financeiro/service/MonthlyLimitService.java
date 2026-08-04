package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitRequest;
import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitResponse;
import com.personal.backend_financeiro.entity.MonthlyLimit;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.MonthlyLimitMapper;
import com.personal.backend_financeiro.repository.MonthlyLimitRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyLimitService {

	private final MonthlyLimitRepository monthlyLimitRepository;
	private final UserRepository userRepository;
	private final MonthlyLimitMapper monthlyLimitMapper;

	@Transactional
	public MonthlyLimitResponse create(Long userId, MonthlyLimitRequest request) {
		assertPeriodAvailable(userId, request, null);

		MonthlyLimit limit = monthlyLimitMapper.toEntity(request);
		limit.setUser(userRepository.getReferenceById(userId));

		MonthlyLimit saved = monthlyLimitRepository.save(limit);
		return monthlyLimitMapper.toResponse(saved);
	}

	public List<MonthlyLimitResponse> listByUser(Long userId) {
		return monthlyLimitRepository.findByUserId(userId).stream()
				.map(monthlyLimitMapper::toResponse)
				.toList();
	}

	@Transactional
	public MonthlyLimitResponse update(Long userId, Long limitId, MonthlyLimitRequest request) {
		MonthlyLimit limit = monthlyLimitRepository.findByIdAndUserId(limitId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Monthly limit not found: " + limitId));

		assertPeriodAvailable(userId, request, limitId);

		monthlyLimitMapper.updateEntityFromRequest(request, limit);
		return monthlyLimitMapper.toResponse(limit);
	}

	@Transactional
	public void delete(Long userId, Long limitId) {
		MonthlyLimit limit = monthlyLimitRepository.findByIdAndUserId(limitId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Monthly limit not found: " + limitId));
		monthlyLimitRepository.delete(limit);
	}

	private void assertPeriodAvailable(Long userId, MonthlyLimitRequest request, Long limitIdToExclude) {
		monthlyLimitRepository.findByUserIdAndYearAndMonth(userId, request.year(), request.month())
				.filter(existing -> !existing.getId().equals(limitIdToExclude))
				.ifPresent(existing -> {
					throw new DuplicateResourceException(
							"Monthly limit already exists for %d/%d".formatted(request.month(), request.year()));
				});
	}

}
