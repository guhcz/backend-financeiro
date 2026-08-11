package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.user.CreateUserRequest;
import com.personal.backend_financeiro.dto.user.UserResponse;
import com.personal.backend_financeiro.security.CurrentUserProvider;
import com.personal.backend_financeiro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

	private final UserService userService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new user")
	@SecurityRequirements
	public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
		return userService.register(request);
	}

	@GetMapping("/me")
	@Operation(summary = "Retorna os dados do usuário autenticado")
	public UserResponse getCurrentUser() {
		return userService.getCurrentUser(currentUserProvider.getCurrentUserId());
	}

}
