package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.auth.LoginRequest;
import com.personal.backend_financeiro.dto.auth.LoginResponse;
import com.personal.backend_financeiro.exception.UnauthenticatedException;
import com.personal.backend_financeiro.security.AuthenticatedUser;
import com.personal.backend_financeiro.security.JwtProperties;
import com.personal.backend_financeiro.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	public LoginResponse login(LoginRequest request) {
		AuthenticatedUser principal;
		try {
			var authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
			principal = (AuthenticatedUser) authentication.getPrincipal();
		} catch (AuthenticationException e) {
			throw new UnauthenticatedException("Invalid email or password");
		}

		String token = jwtService.generateToken(principal.getId(), principal.getUsername());
		return new LoginResponse(token, "Bearer", jwtProperties.expiration());
	}

}
