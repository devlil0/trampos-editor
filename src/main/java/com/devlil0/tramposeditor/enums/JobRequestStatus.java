package com.devlil0.tramposeditor.enums;

/**
 * Ciclo de vida do pedido do cliente. Enxuto de propósito: o pedido serve pra
 * atrair editores interessados, não pra virar gestão de projeto (FUNDACAO §10.2).
 */
public enum JobRequestStatus {
    /** Aberto: aparece pros editores e aceita novos interesses. */
    OPEN,
    /** Fechado pelo cliente (já resolveu ou desistiu). Some da lista dos editores. */
    CLOSED
}
