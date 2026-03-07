package com.household.common.exception;

/**
 * 请求参数或业务前置条件不满足异常，对应 HTTP 400
 *
 * @author household
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
