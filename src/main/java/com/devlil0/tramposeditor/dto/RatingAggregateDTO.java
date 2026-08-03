package com.devlil0.tramposeditor.dto;

/**
 * Resultado do recálculo da nota de alguém. Não é resposta de API — existe pra que a
 * agregação volte tipada da consulta em vez de num {@code Object[]}.
 */
public record RatingAggregateDTO(Double average, Long count) {
}
