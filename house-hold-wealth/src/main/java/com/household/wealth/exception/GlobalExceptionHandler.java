package com.household.wealth.exception;

import com.household.wealth.service.AccountService;
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
 * 全局异常处理器，统一处理财富服务异常
 *
 * @author household
 * @date 2025/01/01
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_CODE_ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    private static final String ERROR_CODE_FORBIDDEN = "FORBIDDEN";
    private static final String ERROR_CODE_NO_FAMILY = "NO_FAMILY";
    private static final String ERROR_CODE_VALIDATION = "VALIDATION_ERROR";
    private static final String ERROR_CODE_INTERNAL = "INTERNAL_ERROR";

    private static final String BODY_KEY_CODE = "code";
    private static final String BODY_KEY_MESSAGE = "message";

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AccountService.AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ERROR_CODE_ACCOUNT_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(AccountService.ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccountService.ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(ERROR_CODE_FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(AccountService.NoFamilyException.class)
    public ResponseEntity<Map<String, String>> handleNoFamily(AccountService.NoFamilyException e) {
        return ResponseEntity.badRequest()
                .body(errorBody(ERROR_CODE_NO_FAMILY, e.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParse(DateTimeParseException e) {
        return ResponseEntity.badRequest()
                .body(errorBody(ERROR_CODE_VALIDATION, "日期格式不正确，应为 YYYY-MM-DD"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().isEmpty()
                ? null
                : e.getBindingResult().getFieldErrors().get(0);
        String message = first != null ? first.getDefaultMessage() : "参数不合法";
        return ResponseEntity.badRequest()
                .body(errorBody(ERROR_CODE_VALIDATION, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(ERROR_CODE_INTERNAL, "服务内部异常"));
    }

    private static Map<String, String> errorBody(String code, String message) {
        Map<String, String> body = new HashMap<>(4);
        body.put(BODY_KEY_CODE, code);
        body.put(BODY_KEY_MESSAGE, message);
        return body;
    }
}
