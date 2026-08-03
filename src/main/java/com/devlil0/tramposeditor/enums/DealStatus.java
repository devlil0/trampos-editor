package com.devlil0.tramposeditor.enums;

/**
 * Estado do trampo combinado. {@code CANCELLED} existe de propósito: histórico que só
 * guarda sucesso não é histórico, é vitrine — e a honestidade sobre o que não deu certo
 * é parte da recusa do ESBOCO §4.2.
 */
public enum DealStatus {
    OPEN,
    DONE,
    CANCELLED
}
