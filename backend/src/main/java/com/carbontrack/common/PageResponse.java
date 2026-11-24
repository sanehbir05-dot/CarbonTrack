package com.carbontrack.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Paged list shape from the shared contract (A8). {@code page} is zero-based. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
