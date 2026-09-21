package com.back.global.security;

import com.back.boundedContext.member.domain.Member;
import com.back.global.exception.DomainException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays
    ) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경 변수가 필요합니다.");
        }

        try {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET은 32바이트 이상의 Base64 키여야 합니다.", exception);
        }
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /** Access/Refresh를 구분해 발급해야 Refresh Token으로 API를 호출할 수 없다. */
    public TokenPair createTokenPair(Member member) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
        Instant refreshExpiresAt = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        return new TokenPair(
                createToken(member, TokenType.ACCESS, now, accessExpiresAt),
                createToken(member, TokenType.REFRESH, now, refreshExpiresAt),
                accessExpiresAt,
                refreshExpiresAt
        );
    }

    public TokenPayload parse(String token, TokenType expectedType) {
        try {
            TokenPayload payload = toPayload(parseClaims(token));
            if (payload.tokenType() != expectedType) {
                throw new DomainException("401-2", "토큰 종류가 올바르지 않습니다.");
            }
            return payload;
        } catch (DomainException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DomainException("401-2", "유효하지 않거나 만료된 토큰입니다.");
        }
    }

    /** 필터에서는 잘못된 토큰을 조용히 익명 요청으로 처리하고 보안 설정이 401을 만든다. */
    public Optional<TokenPayload> parseAccessToken(String token) {
        try {
            TokenPayload payload = toPayload(parseClaims(token));
            return payload.tokenType() == TokenType.ACCESS ? Optional.of(payload) : Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String createToken(Member member, TokenType tokenType, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(member.getEmail())
                .claim("memberId", member.getId())
                .claim("tokenType", tokenType.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private TokenPayload toPayload(Claims claims) {
        Number memberId = claims.get("memberId", Number.class);
        String tokenType = claims.get("tokenType", String.class);
        if (memberId == null || tokenType == null || claims.getSubject() == null) {
            throw new IllegalArgumentException("필수 JWT Claim이 없습니다.");
        }
        return new TokenPayload(memberId.intValue(), claims.getSubject(), TokenType.valueOf(tokenType));
    }

    public enum TokenType {
        ACCESS, REFRESH
    }

    public record TokenPayload(int memberId, String email, TokenType tokenType) {
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }
}
