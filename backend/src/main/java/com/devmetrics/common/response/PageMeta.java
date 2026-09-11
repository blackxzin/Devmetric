package com.devmetrics.common.response;

import org.springframework.data.domain.Page;

public record PageMeta(int page, int size, long total, int totalPages) {

    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
