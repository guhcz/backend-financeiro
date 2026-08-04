package com.personal.backend_financeiro.service;

import com.personal.backend_financeiro.dto.auth.LoginRequest;
import com.personal.backend_financeiro.dto.auth.LoginResponse;
import com.personal.backend_financeiro.exception.UnauthenticatedException;
import com.personal.backend_financeiro.security.AuthenticatedUser;
import com.personal.backend_financeiro.security.JwtProperties;
import com.personal.backend_financeiro.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtService jwtService;
	@Mock
	private JwtProperties jwtProperties;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private AuthService authService;

	@Test
	void login_throwsUnauthenticatedException_withSameMessage_whenCredentialsInvalid() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("bad credentials"));

		assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
				.isInstanceOf(UnauthenticatedException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void login_returnsToken_whenCredentialsValid() {
		AuthenticatedUser principal = new AuthenticatedUser(1L, "alice@example.com", "hashed");
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		when(jwtService.generateToken(1L, "alice@example.com")).thenReturn("a.jwt.token");
		when(jwtProperties.expiration()).thenReturn(3600000L);

		LoginResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

		assertThat(response.token()).isEqualTo("a.jwt.token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600000L);
	}

}
