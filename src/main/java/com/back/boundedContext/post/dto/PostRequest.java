package com.back.boundedContext.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PostRequest {
    public record Create(
            @NotBlank(message = "title은 필수입니다.") @Size(max = 200, message = "title은 200자 이하여야 합니다.") String title,
            @NotBlank(message = "content는 필수입니다.") String content
    ) {
    }

    public record Update(
            @NotBlank(message = "title은 필수입니다.") @Size(max = 200, message = "title은 200자 이하여야 합니다.") String title,
            @NotBlank(message = "content는 필수입니다.") String content
    ) {
    }

    public record Comment(
            @NotBlank(message = "content는 필수입니다.") @Size(max = 2_000, message = "content는 2,000자 이하여야 합니다.") String content
    ) {
    }
}
