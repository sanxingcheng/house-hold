package com.household.authuser.exception;

import com.household.authuser.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthService.UsernameExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameExists(AuthService.UsernameExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("USERNAME_EXISTS", e.getMessage()));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(AuthService.InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().isEmpty()
                ? null
                : e.getBindingResult().getFieldErrors().get(0);
        String message = first != null ? first.getDefaultMessage() : "参数不合法";
        return ResponseEntity.badRequest().body(errorBody("VALIDATION_ERROR", message));
    }

    private static Map<String, String> errorBody(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
