package com.personal.backend_financeiro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {
	private static final String TOKEN_TYPE_CLAIM = "tokenType";
	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final JwtProperties jwtProperties;
	private final SecretKey key;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(Long userId, String email) {
		return generateToken(userId, email, ACCESS_TOKEN_TYPE, jwtProperties.expiration());
	}

	public String generateRefreshToken(Long userId, String email) {
		return generateToken(userId, email, REFRESH_TOKEN_TYPE, jwtProperties.refreshExpiration());
	}

	private String generateToken(Long userId, String email, String tokenType, long expiration) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(expiration);

		return Jwts.builder()
				.subject(email)
				.claim("userId", userId)
				.claim(TOKEN_TYPE_CLAIM, tokenType)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}

	public Claims parseClaims(String token) {
		Claims claims = parseSignedClaims(token);
		String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
		// Tokens issued before token types were introduced are access tokens.
		if (tokenType != null && !ACCESS_TOKEN_TYPE.equals(tokenType)) {
			throw new IllegalArgumentException("Token is not an access token");
		}
		return claims;
	}

	public Claims parseRefreshClaims(String token) {
		Claims claims = parseSignedClaims(token);
		if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
			throw new IllegalArgumentException("Token is not a refresh token");
		}
		return claims;
	}

	private Claims parseSignedClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}
