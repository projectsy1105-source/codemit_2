package com.back.boundedContext.member.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class MemberRequest {

    public record Join(String username, String password, String nickname) {
    }

    public record Login(String username, String password) {
    }

    public record Reissue(String refreshToken) {
    }
}
