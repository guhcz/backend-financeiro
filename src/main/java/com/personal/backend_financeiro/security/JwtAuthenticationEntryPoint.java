package com.personal.backend_financeiro.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Keeps 401 responses in the same ProblemDetail shape as {@code GlobalExceptionHandler},
 * instead of Spring Security's default HTML/plain-text error response.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		String code = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
		if (code == null) {
			code = "AUTHENTICATION_REQUIRED";
		}
		String detail = JwtAuthenticationFilter.TOKEN_EXPIRED.equals(code)
				? "Access token expired"
				: JwtAuthenticationFilter.INVALID_TOKEN.equals(code) ? "Invalid access token" : "Authentication required";
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
		problem.setProperty("code", code);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problem);
	}

}
