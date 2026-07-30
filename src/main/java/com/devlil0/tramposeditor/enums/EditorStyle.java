package com.devlil0.tramposeditor.enums;

/**
 * Como o editor corta — dimensão <b>ortogonal</b> à plataforma ({@link EditorNiche}):
 * um Reels pode ser meme ou cinematográfico, e o cliente costuma pedir pelo estilo
 * ("quero legenda animada", "quero algo clean") antes de falar de plataforma.
 * <p>
 * Lista curta de propósito: tag demais divide o estoque pequeno de editores e faz
 * filtro voltar vazio, que é o que mata a confiança num marketplace novo (FUNDACAO §2).
 */
public enum EditorStyle {
    /** Cortes rápidos, zoom, efeito sonoro — o corte de retenção. */
    DINAMICA,
    /** Legenda como elemento principal, estática ou animada. */
    LEGENDADA,
    /** Visual limpo, sem firula: institucional, educacional. */
    CLEAN,
    /** Cor tratada, ritmo de filme, sound design. */
    CINEMATOGRAFICA,
    /** Texto, ícone e elemento gráfico animado. */
    MOTION_GRAPHICS,
    /** Montagem que constrói narrativa. */
    STORYTELLING,
    /** Efeito engraçado e referência de internet. */
    MEME,
    /** Canal dark: sem rosto, narração sobre imagem de apoio. */
    DARK
}
