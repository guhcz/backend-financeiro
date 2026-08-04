package com.personal.backend_financeiro.config;

import com.personal.backend_financeiro.security.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	/**
	 * Resolves to the authenticated user's email; falls back to "system" for
	 * unauthenticated writes (e.g. user registration itself).
	 */
	@Bean
	public AuditorAware<String> auditorAware() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
				return Optional.of(user.getUsername());
			}
			return Optional.of("system");
		};
	}

}
