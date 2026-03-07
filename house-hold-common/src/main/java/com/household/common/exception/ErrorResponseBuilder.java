package com.household.common.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一错误响应构建器，各模块的 GlobalExceptionHandler 共用
 *
 * @author household
 */
public final class ErrorResponseBuilder {

    private ErrorResponseBuilder() {
    }

    public static Map<String, String> error(String code, String message) {
        Map<String, String> body = new HashMap<>(4);
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
