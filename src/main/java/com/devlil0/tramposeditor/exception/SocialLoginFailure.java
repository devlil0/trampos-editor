package com.devlil0.tramposeditor.exception;

public final class SocialLoginFailure extends RuntimeException {
    public enum Reason {
        CANCELLED("Login cancelado. Você pode tentar de novo."),
        EXPIRED("Essa tentativa de login expirou ou é inválida. Comece de novo."),
        EMAIL_UNVERIFIED("Confirme seu e-mail no provedor ou entre com senha."),
        LINK_NOT_ALLOWED("Entre com o método que você já usa para acessar sua conta."),
        ACCOUNT_INACTIVE("Conta desativada."),
        FAILED("Não foi possível concluir o login. Tente de novo.");

        private final String publicMessage;

        Reason(String publicMessage) {
            this.publicMessage = publicMessage;
        }

        public String publicMessage() {
            return publicMessage;
        }
    }

    private final Reason reason;

    public SocialLoginFailure(Reason reason) {
        super(reason.publicMessage());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
