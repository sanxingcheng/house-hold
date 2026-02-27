package com.household.authuser.exception;

import com.household.authuser.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("UsernameExistsException 返回 409 和 USERNAME_EXISTS")
    void handleUsernameExists_returns409AndCode() {
        AuthService.UsernameExistsException e = new AuthService.UsernameExistsException("用户名已被使用");

        ResponseEntity<Map<String, String>> res = handler.handleUsernameExists(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).containsEntry("code", "USERNAME_EXISTS")
                .containsEntry("message", "用户名已被使用");
    }

    @Test
    @DisplayName("InvalidCredentialsException 返回 401 和 INVALID_CREDENTIALS")
    void handleInvalidCredentials_returns401AndCode() {
        AuthService.InvalidCredentialsException e = new AuthService.InvalidCredentialsException("用户名或密码错误");

        ResponseEntity<Map<String, String>> res = handler.handleInvalidCredentials(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("code", "INVALID_CREDENTIALS")
                .containsEntry("message", "用户名或密码错误");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 返回 400 和 VALIDATION_ERROR")
    void handleValidation_returns400AndCode() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(e.getBindingResult()).thenReturn(bindingResult);
        org.springframework.validation.FieldError fieldError =
                new org.springframework.validation.FieldError("req", "username", "用户名不能为空");
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<Map<String, String>> res = handler.handleValidation(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("message", "用户名不能为空");
    }
}
