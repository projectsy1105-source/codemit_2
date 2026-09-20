package com.back.boundedContext.post.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class PostRequest {
    public record List(String keyword, int authorId, int page, int size) {}
    public record PostContent(int id, String title, String content) {}
}
