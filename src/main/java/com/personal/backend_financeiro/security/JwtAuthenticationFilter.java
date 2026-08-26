package com.personal.backend_financeiro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	public static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";
	public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
	public static final String INVALID_TOKEN = "INVALID_TOKEN";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length());
			try {
				Claims claims = jwtService.parseClaims(token);
				Long userId = claims.get("userId", Number.class).longValue();
				String email = claims.getSubject();

				AuthenticatedUser principal = new AuthenticatedUser(userId, email, null);
				Authentication authentication =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (ExpiredJwtException e) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, TOKEN_EXPIRED);
				SecurityContextHolder.clearContext();
			} catch (JwtException | IllegalArgumentException e) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, INVALID_TOKEN);
				SecurityContextHolder.clearContext();
			}
		}

		chain.doFilter(request, response);
	}

}
