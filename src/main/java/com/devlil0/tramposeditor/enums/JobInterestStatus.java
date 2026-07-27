package com.devlil0.tramposeditor.enums;

/**
 * Estado do interesse de um editor num pedido.
 * <p>
 * O ACCEPTED é o que libera a troca de contato entre as duas pontas — enquanto o chat
 * (Bloco 5) não existe, é ele que faz o papel de "vocês agora podem se falar".
 */
public enum JobInterestStatus {
    PENDING,
    ACCEPTED
}
