package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.monthlylimit.MonthlyLimitRequest;
import com.personal.backend_financeiro.entity.MonthlyLimit;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.MonthlyLimitMapper;
import com.personal.backend_financeiro.repository.MonthlyLimitRepository;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyLimitServiceTest {

	@Mock
	private MonthlyLimitRepository monthlyLimitRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private MonthlyLimitMapper monthlyLimitMapper;

	@InjectMocks
	private MonthlyLimitService monthlyLimitService;

	private static MonthlyLimitRequest sampleRequest() {
		return new MonthlyLimitRequest(7, 2026, new BigDecimal("1000.00"));
	}

	@Test
	void create_throwsDuplicateResourceException_whenPeriodAlreadyHasLimit() {
		MonthlyLimit existing = new MonthlyLimit();
		existing.setId(9L);
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 7)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> monthlyLimitService.create(1L, sampleRequest()))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void update_throwsResourceNotFoundException_whenNotOwnedByUser() {
		when(monthlyLimitRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> monthlyLimitService.update(1L, 9L, sampleRequest()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void update_doesNotThrow_whenPeriodUnchangedForSameLimit() {
		MonthlyLimit existing = new MonthlyLimit();
		existing.setId(9L);
		when(monthlyLimitRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(existing));
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 7)).thenReturn(Optional.of(existing));

		monthlyLimitService.update(1L, 9L, sampleRequest());
	}

	@Test
	void findByPeriod_returnsEmpty_whenNoLimitExistsForPeriod() {
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 7)).thenReturn(Optional.empty());

		assertThat(monthlyLimitService.findByPeriod(1L, 7, 2026)).isEmpty();
	}

	@Test
	void update_throwsDuplicateResourceException_whenMovingToPeriodOwnedByAnotherLimit() {
		MonthlyLimit current = new MonthlyLimit();
		current.setId(9L);
		MonthlyLimit other = new MonthlyLimit();
		other.setId(42L);

		when(monthlyLimitRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(current));
		when(monthlyLimitRepository.findByUserIdAndYearAndMonth(1L, 2026, 7)).thenReturn(Optional.of(other));

		assertThatThrownBy(() -> monthlyLimitService.update(1L, 9L, sampleRequest()))
				.isInstanceOf(DuplicateResourceException.class);
	}

}
