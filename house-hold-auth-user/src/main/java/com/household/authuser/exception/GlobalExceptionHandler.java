package com.household.authuser.exception;

import com.household.authuser.service.AuthService;
import com.household.authuser.service.FamilyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器，统一处理认证和家庭服务异常
 *
 * @author household
 * @date 2025/01/01
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_CODE_USERNAME_EXISTS = "USERNAME_EXISTS";
    private static final String ERROR_CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String ERROR_CODE_NOT_FOUND = "NOT_FOUND";
    private static final String ERROR_CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String ERROR_CODE_FORBIDDEN = "FORBIDDEN";
    private static final String ERROR_CODE_VALIDATION = "VALIDATION_ERROR";

    private static final String BODY_KEY_CODE = "code";
    private static final String BODY_KEY_MESSAGE = "message";

    @ExceptionHandler(AuthService.UsernameExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameExists(AuthService.UsernameExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ERROR_CODE_USERNAME_EXISTS, e.getMessage()));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(AuthService.InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(ERROR_CODE_INVALID_CREDENTIALS, e.getMessage()));
    }

    @ExceptionHandler(FamilyService.NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(FamilyService.NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ERROR_CODE_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({FamilyService.AlreadyInFamilyException.class, FamilyService.BadRequestException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(errorBody(ERROR_CODE_BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(FamilyService.ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(FamilyService.ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ERROR_CODE_FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParse(DateTimeParseException e) {
        return ResponseEntity.badRequest().body(errorBody(ERROR_CODE_VALIDATION, "日期格式不正确，应为 YYYY-MM-DD"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().isEmpty()
                ? null
                : e.getBindingResult().getFieldErrors().get(0);
        String message = first != null ? first.getDefaultMessage() : "参数不合法";
        return ResponseEntity.badRequest().body(errorBody(ERROR_CODE_VALIDATION, message));
    }

    private static Map<String, String> errorBody(String code, String message) {
        Map<String, String> body = new HashMap<>(4);
        body.put(BODY_KEY_CODE, code);
        body.put(BODY_KEY_MESSAGE, message);
        return body;
    }
}
