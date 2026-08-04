package com.personal.backend_financeiro.controller;

import com.personal.backend_financeiro.dto.auth.LoginRequest;
import com.personal.backend_financeiro.dto.auth.LoginResponse;
import com.personal.backend_financeiro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(summary = "Authenticate and obtain a JWT")
	@SecurityRequirements
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

}
