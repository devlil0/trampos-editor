package com.devlil0.tramposeditor.exception;

/**
 * Usuário autenticado, mas sem permissão pra ação — ex.: quem não é EDITOR
 * tentando criar perfil de editor, ou mexer em portfólio que não é seu.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String msg){
        super(msg);
    }
}
