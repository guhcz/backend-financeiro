package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.user.CreateUserRequest;
import com.personal.backend_financeiro.dto.user.UserResponse;
import com.personal.backend_financeiro.entity.User;
import com.personal.backend_financeiro.exception.DuplicateResourceException;
import com.personal.backend_financeiro.exception.ResourceNotFoundException;
import com.personal.backend_financeiro.mapper.UserMapper;
import com.personal.backend_financeiro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserResponse register(CreateUserRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateResourceException("Email already in use: " + request.email());
		}

		User user = userMapper.toEntity(request);
		user.setPassword(passwordEncoder.encode(request.password()));

		User saved = userRepository.save(user);
		return userMapper.toResponse(saved);
	}

	public UserResponse getCurrentUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
		return userMapper.toResponse(user);
	}

}
