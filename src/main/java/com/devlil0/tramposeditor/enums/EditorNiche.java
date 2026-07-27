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
    PODCAST
}
