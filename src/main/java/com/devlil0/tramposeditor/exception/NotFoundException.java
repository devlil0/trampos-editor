package com.devlil0.tramposeditor.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String msg){
        super(msg);
    }

    public NotFoundException(Object id){
        super("Registro não encontrado: " + id);
    }
}
