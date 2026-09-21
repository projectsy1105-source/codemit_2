package com.back.shared.post.dto;

import java.util.List;

/** Spring Data의 Page를 노출하지 않고 API에 필요한 페이징 정보만 전달한다. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

}
