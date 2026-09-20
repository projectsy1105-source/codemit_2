package com.back.boundedContext.member.dto;

import com.back.global.security.JwtTokenProvider;

public record TokenResponse(String accessToken, String refreshToken) {

    public static TokenResponse from(JwtTokenProvider.TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
