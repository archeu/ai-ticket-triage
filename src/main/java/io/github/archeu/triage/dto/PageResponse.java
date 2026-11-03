package io.github.archeu.triage.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果。只返回前端需要的几个字段，不直接暴露 Spring Data 的 Page 结构。
 *
 * @param page 当前页码，从 0 开始
 */
public record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
