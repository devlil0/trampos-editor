package com.devlil0.tramposeditor.enums;

/**
 * Uma mesma pessoa pode acumular EDITOR e CLIENT — por isso o usuário guarda um Set.
 * ADMIN nunca é atribuível via cadastro público (ver AuthService#sanitizeRoles).
 */
public enum UserRole {
    EDITOR,
    CLIENT,
    ADMIN
}
