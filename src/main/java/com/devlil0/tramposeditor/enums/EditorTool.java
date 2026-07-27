package com.devlil0.tramposeditor.enums;

/**
 * Ferramentas de edição que o editor domina — vira filtro na vitrine.
 * Enum (e não tabela) de propósito: a lista muda pouco e evita seed/join extra no MVP.
 */
public enum EditorTool {
    PREMIERE,
    AFTER_EFFECTS,
    DAVINCI_RESOLVE,
    FINAL_CUT_PRO,
    CAPCUT
}
