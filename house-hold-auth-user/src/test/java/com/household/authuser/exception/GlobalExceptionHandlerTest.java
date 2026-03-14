package com.household.authuser.exception;

import com.household.authuser.service.AuthService;
import com.household.common.exception.BadRequestException;
import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.format.DateTimeParseException;
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

    /** 验证用户名已存在异常返回 409 CONFLICT 和 USERNAME_EXISTS 错误码 */
    @Test
    @DisplayName("UsernameExistsException 返回 409 和 USERNAME_EXISTS")
    void handleUsernameExists_returns409AndCode() {
        AuthService.UsernameExistsException e = new AuthService.UsernameExistsException("用户名已被使用");

        ResponseEntity<Map<String, String>> res = handler.handleUsernameExists(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).containsEntry("code", "USERNAME_EXISTS")
                .containsEntry("message", "用户名已被使用");
    }

    /** 验证凭证无效异常返回 401 UNAUTHORIZED 和 INVALID_CREDENTIALS 错误码 */
    @Test
    @DisplayName("InvalidCredentialsException 返回 401 和 INVALID_CREDENTIALS")
    void handleInvalidCredentials_returns401AndCode() {
        AuthService.InvalidCredentialsException e = new AuthService.InvalidCredentialsException("用户名或密码错误");

        ResponseEntity<Map<String, String>> res = handler.handleInvalidCredentials(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("code", "INVALID_CREDENTIALS")
                .containsEntry("message", "用户名或密码错误");
    }

    /** 验证资源未找到异常返回 404 NOT_FOUND 和 NOT_FOUND 错误码 */
    @Test
    @DisplayName("NotFoundException 返回 404 和 NOT_FOUND")
    void handleNotFound_returns404AndCode() {
        NotFoundException e = new NotFoundException("家庭不存在");

        ResponseEntity<Map<String, String>> res = handler.handleNotFound(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsEntry("code", "NOT_FOUND")
                .containsEntry("message", "家庭不存在");
    }

    /** 验证业务前置条件不满足时返回 400 BAD_REQUEST（场景：已属于家庭） */
    @Test
    @DisplayName("BadRequestException（已属于家庭）返回 400 和 BAD_REQUEST")
    void handleAlreadyInFamily_returns400AndCode() {
        BadRequestException e = new BadRequestException("您已属于一个家庭");

        ResponseEntity<Map<String, String>> res = handler.handleBadRequest(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("code", "BAD_REQUEST")
                .containsEntry("message", "您已属于一个家庭");
    }

    /** 验证通用业务错误返回 400 BAD_REQUEST */
    @Test
    @DisplayName("BadRequestException 返回 400 和 BAD_REQUEST")
    void handleBadRequest_returns400AndCode() {
        BadRequestException e = new BadRequestException("家庭ID不能为空");

        ResponseEntity<Map<String, String>> res = handler.handleBadRequest(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("code", "BAD_REQUEST")
                .containsEntry("message", "家庭ID不能为空");
    }

    /** 验证无权操作异常返回 403 FORBIDDEN */
    @Test
    @DisplayName("ForbiddenException 返回 403 和 FORBIDDEN")
    void handleForbidden_returns403AndCode() {
        ForbiddenException e = new ForbiddenException("您不属于该家庭");

        ResponseEntity<Map<String, String>> res = handler.handleForbidden(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).containsEntry("code", "FORBIDDEN")
                .containsEntry("message", "您不属于该家庭");
    }

    /** 验证日期格式解析失败时返回 400 和固定提示消息 */
    @Test
    @DisplayName("DateTimeParseException 返回 400 和 VALIDATION_ERROR")
    void handleDateTimeParse_returns400AndValidationError() {
        DateTimeParseException e = new DateTimeParseException("parse error", "invalid-date", 0);

        ResponseEntity<Map<String, String>> res = handler.handleDateTimeParse(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("message", "日期格式不正确，应为 YYYY-MM-DD");
    }

    /** 验证参数校验异常返回 400 和第一个字段错误信息 */
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

    /** 验证 MethodArgumentNotValidException 无字段错误时返回默认提示 */
    @Test
    @DisplayName("无字段错误时返回默认提示")
    void handleValidation_noFieldErrors_returnsDefaultMessage() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(e.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

        ResponseEntity<Map<String, String>> res = handler.handleValidation(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("message", "参数不合法");
    }
}
