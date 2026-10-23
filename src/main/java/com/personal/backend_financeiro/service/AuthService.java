package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.auth.LoginRequest;
import com.personal.backend_financeiro.dto.auth.LoginResponse;
import com.personal.backend_financeiro.dto.auth.RefreshTokenRequest;
import com.personal.backend_financeiro.exception.UnauthenticatedException;
import com.personal.backend_financeiro.repository.UserRepository;
import com.personal.backend_financeiro.security.AuthenticatedUser;
import com.personal.backend_financeiro.security.JwtProperties;
import com.personal.backend_financeiro.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final UserRepository userRepository;

	public LoginResponse login(LoginRequest request) {
		AuthenticatedUser principal;
		try {
			var authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
			principal = (AuthenticatedUser) authentication.getPrincipal();
		} catch (AuthenticationException e) {
			throw new UnauthenticatedException("Invalid email or password");
		}

		return issueTokens(principal.getId(), principal.getUsername());
	}

	public LoginResponse refresh(RefreshTokenRequest request) {
		try {
			Claims claims = jwtService.parseRefreshClaims(request.refreshToken());
			Long userId = claims.get("userId", Number.class).longValue();
			String email = claims.getSubject();

			if (!userRepository.existsById(userId)) {
				throw new UnauthenticatedException("Invalid refresh token");
			}

			return issueTokens(userId, email);
		} catch (JwtException | IllegalArgumentException e) {
			throw new UnauthenticatedException("Invalid or expired refresh token");
		}
	}

	private LoginResponse issueTokens(Long userId, String email) {
		String accessToken = jwtService.generateToken(userId, email);
		String refreshToken = jwtService.generateRefreshToken(userId, email);
		return new LoginResponse(accessToken, "Bearer", jwtProperties.expiration(),
				refreshToken, jwtProperties.refreshExpiration());
	}

}
