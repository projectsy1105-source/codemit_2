package com.back.boundedContext.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MemberRequest {

    public record Join(
            @NotBlank(message = "email은 필수입니다.") @Email(message = "email 형식이 올바르지 않습니다.") String email,
            @NotBlank(message = "password는 필수입니다.") @Size(min = 8, max = 100, message = "password는 8자 이상 100자 이하여야 합니다.") String password,
            @NotBlank(message = "nickname은 필수입니다.") @Size(max = 50, message = "nickname은 50자 이하여야 합니다.") String nickname
    ) {
    }

    public record Login(
            @NotBlank(message = "email은 필수입니다.") @Email(message = "email 형식이 올바르지 않습니다.") String email,
            @NotBlank(message = "password는 필수입니다.") String password
    ) {
    }

    public record Reissue(@NotBlank(message = "refreshToken은 필수입니다.") String refreshToken) {
    }
}
