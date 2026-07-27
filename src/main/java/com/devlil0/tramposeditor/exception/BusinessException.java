package com.devlil0.tramposeditor.exception;

/**
 * Regra de negócio violada com dados válidos — ex.: e-mail já cadastrado.
 */
public class BusinessException extends ApiException {

    public BusinessException(String msg){
        super(msg);
    }
}
