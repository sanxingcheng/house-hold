package com.household.single.exception;

import com.household.authuser.service.AuthService;
import com.household.common.exception.BadRequestException;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;

import static com.household.common.exception.ErrorResponseBuilder.error;

/**
 * Unified GlobalExceptionHandler combining handlers from auth-user and wealth modules.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Auth-user specific
    @ExceptionHandler(AuthService.UsernameExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameExists(AuthService.UsernameExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("USERNAME_EXISTS", e.getMessage()));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(AuthService.InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("INVALID_CREDENTIALS", e.getMessage()));
    }

    // Common handlers
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        log.error("NOT_FOUND: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException e) {
        log.error("BAD_REQUEST: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(error("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException e) {
        log.error("FORBIDDEN: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParse(DateTimeParseException e) {
        log.error("VALIDATION_ERROR: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "日期格式不正确，应为 YYYY-MM-DD"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().isEmpty()
                ? null
                : e.getBindingResult().getFieldErrors().get(0);
        ObjectError firstGlobal = e.getBindingResult().getGlobalErrors().isEmpty()
                ? null
                : e.getBindingResult().getGlobalErrors().get(0);
        String message = first != null
                ? first.getDefaultMessage()
                : firstGlobal != null ? firstGlobal.getDefaultMessage() : "参数不合法";
        log.error("VALIDATION_ERROR: {}", message, e);
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        log.error("INTERNAL_ERROR", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "服务内部异常"));
    }
}
