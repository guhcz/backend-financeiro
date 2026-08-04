package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.user.CreateUserRequest;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.mapper.UserMapper;
import com.personal.backend_financeiro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserMapper userMapper;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void register_throwsDuplicateResourceException_whenEmailAlreadyInUse() {
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

		CreateUserRequest request = new CreateUserRequest("Alice", "alice@example.com", "password123");

		assertThatThrownBy(() -> userService.register(request))
				.isInstanceOf(DuplicateResourceException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	void register_savesUserWithEncodedPassword_neverTheRawOne() {
		CreateUserRequest request = new CreateUserRequest("Alice", "alice@example.com", "password123");
		User entity = new User();

		when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(userMapper.toEntity(request)).thenReturn(entity);
		when(passwordEncoder.encode("password123")).thenReturn("hashed-value");
		when(userRepository.save(entity)).thenReturn(entity);

		userService.register(request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getPassword()).isEqualTo("hashed-value");
	}

}
