package com.devlil0.tramposeditor.enums;

/**
 * Onde o editor entrega — filtro central da vitrine (FUNDACAO §7).
 * <p>
 * Taxonomia por <b>plataforma</b>, não por formato: o cliente chega procurando
 * "editor de TikTok", não "editor de short-form". Continua trivialmente editável (é só enum).
 */
public enum EditorNiche {
    TIKTOK,
    REELS,
    YOUTUBE,
    PODCAST,
    /**
     * Canal sem rosto, narração sobre imagem de apoio. Era {@code EditorStyle.DARK} até a
     * V11: estilo é <b>como</b> se corta, e canal dark é <b>que tipo</b> de canal é — a
     * mesma natureza de YOUTUBE e PODCAST.
     */
    DARK
}
