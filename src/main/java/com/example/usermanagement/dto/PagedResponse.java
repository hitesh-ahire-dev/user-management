package com.example.usermanagement.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Consistent wrapper for paginated results.
 *
 * <p>Returning a Spring {@code Page} directly would leak an unstable internal
 * JSON structure, so we expose a stable shape with explicit metadata.
 *
 * @param <T> the type of the page content
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    /** Builds a {@link PagedResponse} from a Spring Data {@link Page}. */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
