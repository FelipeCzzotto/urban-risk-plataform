package br.com.urbanrisk.web.dto;

import br.com.urbanrisk.application.PageResult;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> p) {
        return new PageResponse<>(p.content(), p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
