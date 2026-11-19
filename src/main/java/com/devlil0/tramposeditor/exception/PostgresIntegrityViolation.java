package com.devlil0.tramposeditor.exception;

import org.postgresql.util.PSQLException;

import java.sql.SQLException;

public record PostgresIntegrityViolation(String sqlState, Duplicate duplicate) {

    public enum Duplicate {
        ACCOUNT_EMAIL("Este e-mail já está em uso."),
        ACCOUNT_USERNAME("Esse nome de usuário já está em uso."),
        SIGNUP_EMAIL("Já existe um cadastro pendente com este e-mail."),
        SIGNUP_USERNAME("Esse nome de usuário está reservado por outro cadastro."),
        JOB_INTEREST("Já existe uma candidatura para este pedido."),
        CONVERSATION("Já existe uma conversa para este pedido ou perfil."),
        OPEN_DEAL("Já existe um trampo aberto nesta conversa."),
        REVIEW("Já existe uma avaliação sua para este trampo.");

        private final String publicMessage;

        Duplicate(String publicMessage) {
            this.publicMessage = publicMessage;
        }

        public String publicMessage() {
            return publicMessage;
        }
    }

    public static PostgresIntegrityViolation from(Throwable exception) {
        String sqlState = "desconhecido";
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException postgres) {
                String state = safeState(postgres.getSQLState());
                var details = postgres.getServerErrorMessage();
                Duplicate duplicate = "23505".equals(state) && details != null
                        ? duplicateOf(details.getConstraint())
                        : null;
                return new PostgresIntegrityViolation(state, duplicate);
            }

            if (cause instanceof SQLException sql && "desconhecido".equals(sqlState)) {
                sqlState = safeState(sql.getSQLState());
            }
        }
        return new PostgresIntegrityViolation(sqlState, null);
    }

    private static Duplicate duplicateOf(String constraint) {
        return switch (constraint) {
            case "uk_users_email" -> Duplicate.ACCOUNT_EMAIL;
            case "uk_users_username" -> Duplicate.ACCOUNT_USERNAME;
            case "uk_pending_signups_email" -> Duplicate.SIGNUP_EMAIL;
            case "uk_pending_signups_username" -> Duplicate.SIGNUP_USERNAME;
            case "uk_job_interests_editor" -> Duplicate.JOB_INTEREST;
            case "uk_conversations_direta", "uk_conversations_por_pedido" -> Duplicate.CONVERSATION;
            case "uk_deals_um_aberto_por_conversa" -> Duplicate.OPEN_DEAL;
            case "uk_reviews_autor_por_trampo" -> Duplicate.REVIEW;
            case null, default -> null;
        };
    }

    private static String safeState(String state) {
        return state != null && state.matches("[0-9A-Z]{5}") ? state : "desconhecido";
    }
}
