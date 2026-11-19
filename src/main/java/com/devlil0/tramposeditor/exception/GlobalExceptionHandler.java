package com.devlil0.tramposeditor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleAuthRateLimit(AuthRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("error", "Muitas tentativas. Aguarde e tente novamente."));
    }

    @ExceptionHandler(com.devlil0.tramposeditor.notification.RequiredEmailException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleRequiredEmail(
            com.devlil0.tramposeditor.notification.RequiredEmailException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NotFoundException exception){
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleBusiness(BusinessException exception){
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleUnauthorized(UnauthorizedException exception){
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(ForbiddenException exception){
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException exception){
        List<String> errors = exception.getBindingResult().getFieldErrors()
                .stream()
                .map( e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return Map.of("errors", errors);
    }

}
