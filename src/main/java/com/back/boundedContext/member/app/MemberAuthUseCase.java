package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.domain.RefreshToken;
import com.back.boundedContext.member.out.MemberRepository;
import com.back.boundedContext.member.out.RefreshTokenRepository;
import com.back.global.exception.DomainException;
import com.back.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class MemberAuthUseCase {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtTokenProvider.TokenPair login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw invalidCredentials();
        }

        return issueAndSaveTokens(member);
    }

    @Transactional
    public JwtTokenProvider.TokenPair reissue(String refreshToken) {
        JwtTokenProvider.TokenPayload payload = jwtTokenProvider.parse(refreshToken, JwtTokenProvider.TokenType.REFRESH);
        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new DomainException("401-2", "유효하지 않은 Refresh Token 입니다."));
        RefreshToken savedRefreshToken = refreshTokenRepository.findByMember_Id(member.getId())
                .orElseThrow(() -> new DomainException("401-2", "유효하지 않은 Refresh Token 입니다."));

        if (savedRefreshToken.isExpired() || !passwordEncoder.matches(toBcryptInput(refreshToken), savedRefreshToken.getTokenHash())) {
            throw new DomainException("401-2", "유효하지 않은 Refresh Token 입니다.");
        }

        return issueAndSaveTokens(member);
    }

    @Transactional
    public void logout(int memberId) {
        // Access Token 자체는 무상태이므로, Refresh Token을 삭제해 재발급만 즉시 차단한다.
        refreshTokenRepository.deleteByMember_Id(memberId);
    }

    private JwtTokenProvider.TokenPair issueAndSaveTokens(Member member) {
        JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.createTokenPair(member);
        String refreshTokenHash = passwordEncoder.encode(toBcryptInput(tokenPair.refreshToken()));
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.ofInstant(tokenPair.refreshTokenExpiresAt(), ZoneOffset.UTC);

        refreshTokenRepository.findByMember_Id(member.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.renew(refreshTokenHash, refreshTokenExpiresAt),
                        () -> refreshTokenRepository.save(new RefreshToken(member, refreshTokenHash, refreshTokenExpiresAt))
                );

        return tokenPair;
    }

    private DomainException invalidCredentials() {
        // 이메일 존재 여부를 구분하지 않아 계정 열거 공격을 줄인다.
        return new DomainException("401-1", "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    private String toBcryptInput(String refreshToken) {
        try {
            // JWT는 BCrypt의 72바이트 입력 제한보다 길 수 있어 SHA-256으로 먼저 고정 길이화한다.
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
