package com.devlil0.tramposeditor.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação neutro — não vaza o formato interno do Page do Spring Data
 * (que muda entre versões) pra resposta pública da API.
 */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PageResponseDTO<T> from(Page<E> source, Function<E, T> mapper){
        return new PageResponseDTO<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast()
        );
    }
}
