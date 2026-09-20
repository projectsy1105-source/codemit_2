package com.back.shared.post.dto;

import java.util.List;

public record PageResponse<T> (List<T> list, int page, int size, int pageSize, int total) {

}
